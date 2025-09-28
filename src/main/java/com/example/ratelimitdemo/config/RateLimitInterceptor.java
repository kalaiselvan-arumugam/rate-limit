package com.example.ratelimitdemo.config;

import com.example.ratelimitdemo.service.RateLimiterService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class RateLimitInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(RateLimitInterceptor.class);

    private final RateLimiterService rateLimiterService;

    @Value("${ratelimit.short.capacity:5}")
    private int cfgShortCapacity;

    @Value("${ratelimit.short.windowSeconds:5}")
    private int cfgShortWindowSeconds;

    @Value("${ratelimit.minute.capacity:10}")
    private int cfgMinuteCapacity;

    @Value("${ratelimit.minute.windowSeconds:60}")
    private int cfgMinuteWindowSeconds;

    public RateLimitInterceptor(RateLimiterService rateLimiterService) {
        this.rateLimiterService = rateLimiterService;
        log.info("RateLimitInterceptor created");
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String path = request.getRequestURI();
        log.debug("preHandle path={}", path);
        // apply limiter only to test1 and test3
        if (!"/api/test1".equals(path) && !"/api/test3".equals(path)) {
            log.trace("Bypassing rate limiter for path={}", path);
            return true;
        }

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String user = (auth != null && auth.getName() != null) ? auth.getName() : "anonymous";
        String keyBase = user + ":" + path.substring(path.lastIndexOf('/') + 1);

        double shortRefill = ((double) cfgShortCapacity) / Math.max(1, cfgShortWindowSeconds);
        double minuteRefill = ((double) cfgMinuteCapacity) / Math.max(1, cfgMinuteWindowSeconds);

        String shortKey = keyBase + ":short";
        String minuteKey = keyBase + ":minute";

        log.debug("Applying composite rate limit for user={} shortKey={} minuteKey={} (short cap={}, shortWindow={}, minute cap={}, minuteWindow={})",
                user, shortKey, minuteKey, cfgShortCapacity, cfgShortWindowSeconds, cfgMinuteCapacity, cfgMinuteWindowSeconds);

        boolean allowed = rateLimiterService.tryConsumeComposite(shortKey, cfgShortCapacity, shortRefill,
                minuteKey, cfgMinuteCapacity, minuteRefill);

        // fetch bucket snapshots for headers
        RateLimiterService.BucketInfo shortInfo = rateLimiterService.getBucketInfo(shortKey, cfgShortCapacity, shortRefill);
        RateLimiterService.BucketInfo minuteInfo = rateLimiterService.getBucketInfo(minuteKey, cfgMinuteCapacity, minuteRefill);

        // set headers on current response if available
        response.setHeader("X-RateLimit-Short-Limit", String.valueOf(shortInfo.getCapacity()));
        response.setHeader("X-RateLimit-Short-Remaining", String.valueOf(shortInfo.getRemaining()));
        response.setHeader("X-RateLimit-Short-Retry-After", String.valueOf(shortInfo.getRetryAfterSeconds()));

        response.setHeader("X-RateLimit-Minute-Limit", String.valueOf(minuteInfo.getCapacity()));
        response.setHeader("X-RateLimit-Minute-Remaining", String.valueOf(minuteInfo.getRemaining()));
        response.setHeader("X-RateLimit-Minute-Retry-After", String.valueOf(minuteInfo.getRetryAfterSeconds()));

        if (!allowed) {
            int retry = Math.max(shortInfo.getRetryAfterSeconds(), minuteInfo.getRetryAfterSeconds());
            if (retry > 0 && retry < Integer.MAX_VALUE) {
                response.setHeader("Retry-After", String.valueOf(retry));
            }
            log.warn("Blocking request for user={} path={} shortRemaining={} minuteRemaining={} retryAfter={}",
                    user, path, shortInfo.getRemaining(), minuteInfo.getRemaining(), retry);
            response.sendError(429, "Too Many Requests");
            return false;
        }

        log.debug("Request allowed for user={} path={} shortRemaining={} minuteRemaining={}", user, path, shortInfo.getRemaining(), minuteInfo.getRemaining());
        return true;
    }
}
