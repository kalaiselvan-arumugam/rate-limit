package com.example.ratelimitdemo.config;

import com.example.ratelimitdemo.service.FailedLoginService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationListener;
import org.springframework.security.authentication.event.AbstractAuthenticationFailureEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;

@Component
public class AuthenticationEventListener implements ApplicationListener<org.springframework.context.ApplicationEvent> {
    private static final Logger log = LoggerFactory.getLogger(AuthenticationEventListener.class);

    private final FailedLoginService failedLoginService;

    public AuthenticationEventListener(FailedLoginService failedLoginService) {
        this.failedLoginService = failedLoginService;
    }

    @Override
    public void onApplicationEvent(org.springframework.context.ApplicationEvent event) {
        if (event instanceof AuthenticationSuccessEvent) {
            AuthenticationSuccessEvent ase = (AuthenticationSuccessEvent) event;
            String username = ase.getAuthentication() != null ? ase.getAuthentication().getName() : null;
            String ip = resolveIp();
            failedLoginService.recordSuccess(username, ip);
            log.info("Authentication success for user={} ip={}", username, ip);
        } else if (event instanceof AbstractAuthenticationFailureEvent) {
            AbstractAuthenticationFailureEvent afe = (AbstractAuthenticationFailureEvent) event;
            String username = afe.getAuthentication() != null ? String.valueOf(afe.getAuthentication().getPrincipal()) : null;
            String ip = resolveIp();
            failedLoginService.recordFailed(username, ip);
            log.warn("Authentication failure for principal={} ip={} exception={}", username, ip, afe.getException().getMessage());
        }
    }

    private String resolveIp() {
        RequestAttributes reqAttrs = RequestContextHolder.getRequestAttributes();
        if (reqAttrs instanceof ServletRequestAttributes) {
            HttpServletRequest req = ((ServletRequestAttributes) reqAttrs).getRequest();
            String xf = req.getHeader("X-Forwarded-For");
            if (xf != null && !xf.isEmpty()) return xf.split(",")[0].trim();
            return req.getRemoteAddr();
        }
        return "unknown";
    }
}

