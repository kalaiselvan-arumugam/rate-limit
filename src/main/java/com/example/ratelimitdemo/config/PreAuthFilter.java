package com.example.ratelimitdemo.config;

import com.example.ratelimitdemo.service.FailedLoginService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Base64;

@Component
public class PreAuthFilter extends OncePerRequestFilter {
    private static final Logger log = LoggerFactory.getLogger(PreAuthFilter.class);

    private final FailedLoginService failedLoginService;

    private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_OFFSET_DATE_TIME.withZone(ZoneId.systemDefault());

    public PreAuthFilter(FailedLoginService failedLoginService) {
        this.failedLoginService = failedLoginService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String auth = request.getHeader("Authorization");
        String ip = resolveIp(request);

        if (StringUtils.hasText(auth) && auth.startsWith("Basic ")) {
            String base64 = auth.substring(6);
            try {
                String decoded = new String(Base64.getDecoder().decode(base64), StandardCharsets.UTF_8);
                int idx = decoded.indexOf(':');
                String username = idx > 0 ? decoded.substring(0, idx) : decoded;

                boolean userBlocked = failedLoginService.isUserBlocked(username);
                boolean ipBlocked = failedLoginService.isIpBlocked(ip);

                if (userBlocked || ipBlocked) {
                    int remaining = failedLoginService.remainingAttemptsForUser(username);

                    long remainingSeconds = userBlocked ? failedLoginService.getUserLockRemainingSeconds(username) : failedLoginService.getIpLockRemainingSeconds(ip);
                    long unlockEpoch = userBlocked ? failedLoginService.getUserUnlockEpochMillis(username) : failedLoginService.getIpUnlockEpochMillis(ip);
                    String unlockTimeIso = unlockEpoch > 0 ? ISO.format(Instant.ofEpochMilli(unlockEpoch)) : "";

                    response.setHeader("X-Auth-Blocked-User", String.valueOf(username));
                    response.setHeader("X-Auth-Blocked-Remaining", String.valueOf(remaining));
                    response.setHeader("X-Auth-Blocked-Remaining-Seconds", String.valueOf(remainingSeconds));
                    response.setHeader("X-Auth-Blocked-Unlock-Epoch-Millis", String.valueOf(unlockEpoch));
                    response.setHeader("X-Auth-Blocked-Unlock-Time", unlockTimeIso);

                    int retry = (remainingSeconds > 0 && remainingSeconds < Integer.MAX_VALUE) ? (int) remainingSeconds : 60;
                    response.setHeader("Retry-After", String.valueOf(retry));
                    log.warn("Blocking request for blocked username={} ip={} userBlocked={} ipBlocked={} remainingSeconds={} unlock={}", username, ip, userBlocked, ipBlocked, remainingSeconds, unlockTimeIso);
                    response.sendError(429, "Too Many Failed Login Attempts");
                    return;
                }
            } catch (IllegalArgumentException e) {
                log.debug("Failed to decode Basic auth header", e);
            }
        }

        filterChain.doFilter(request, response);
    }

    private String resolveIp(HttpServletRequest request) {
        String xf = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(xf)) {
            return xf.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
