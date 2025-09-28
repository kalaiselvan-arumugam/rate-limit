package com.example.ratelimitdemo.config;

import com.example.ratelimitdemo.service.RateLimiterService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class RateLimitInterceptor implements HandlerInterceptor {

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
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String path = request.getRequestURI();
        // apply limiter only to test1 and test3
        if (!"/api/test1".equals(path) && !"/api/test3".equals(path)) {
            return true;
        }

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String user = (auth != null && auth.getName() != null) ? auth.getName() : "anonymous";
        String keyBase = user + ":" + path.substring(path.lastIndexOf('/') + 1);

        double shortRefill = ((double) cfgShortCapacity) / Math.max(1, cfgShortWindowSeconds);
        double minuteRefill = ((double) cfgMinuteCapacity) / Math.max(1, cfgMinuteWindowSeconds);

        String shortKey = keyBase + ":short";
        String minuteKey = keyBase + ":minute";

        boolean allowed = rateLimiterService.tryConsumeComposite(shortKey, cfgShortCapacity, shortRefill,
                minuteKey, cfgMinuteCapacity, minuteRefill);

        // set headers
        RateLimiterService.BucketInfo shortInfo = rateLimiterService.getBucketInfo(shortKey, cfgShortCapacity, shortRefill);
        RateLimiterService.BucketInfo minuteInfo = rateLimiterService.getBucketInfo(minuteKey, cfgMinuteCapacity, minuteRefill);

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
            // SC_TOO_MANY_REQUESTS constant may not be available in jakarta servlet API version; use literal 429
            response.sendError(429, "Too Many Requests");
            return false;
        }

        return true;
    }
}
