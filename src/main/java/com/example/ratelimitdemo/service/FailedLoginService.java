package com.example.ratelimitdemo.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class FailedLoginService {
    private static final Logger log = LoggerFactory.getLogger(FailedLoginService.class);

    private final Map<String, Attempt> userAttempts = new ConcurrentHashMap<>();
    private final Map<String, Attempt> ipAttempts = new ConcurrentHashMap<>();

    private final int threshold;
    private final long lockMillis;

    public FailedLoginService(@Value("${security.bruteforce.threshold:5}") int threshold,
                              @Value("${security.bruteforce.lockMinutes:15}") int lockMinutes) {
        this.threshold = threshold;
        this.lockMillis = lockMinutes * 60L * 1000L;
        log.info("FailedLoginService initialized with threshold={} lockMinutes={}", threshold, lockMinutes);
    }

    public void recordFailed(String username, String ip) {
        if (username != null) {
            userAttempts.compute(username, (k, v) -> {
                if (v == null) v = new Attempt();
                v.count++;
                if (v.count >= threshold) v.lockUntil = Instant.now().toEpochMilli() + lockMillis;
                return v;
            });
            log.warn("Failed login for user={} ip={} count={}", username, ip, userAttempts.get(username).count);
        }
        if (ip != null) {
            ipAttempts.compute(ip, (k, v) -> {
                if (v == null) v = new Attempt();
                v.count++;
                if (v.count >= threshold) v.lockUntil = Instant.now().toEpochMilli() + lockMillis;
                return v;
            });
            log.warn("Failed login for ip={} username={} count={}", ip, username, ipAttempts.get(ip).count);
        }
    }

    public void recordSuccess(String username, String ip) {
        if (username != null) {
            userAttempts.remove(username);
            log.info("Reset failed attempts for user={}", username);
        }
        if (ip != null) {
            ipAttempts.remove(ip);
            log.info("Reset failed attempts for ip={}", ip);
        }
    }

    public boolean isUserBlocked(String username) {
        if (username == null) return false;
        Attempt a = userAttempts.get(username);
        boolean blocked = a != null && a.lockUntil > Instant.now().toEpochMilli();
        if (blocked) log.debug("User {} is blocked until {}", username, a.lockUntil);
        return blocked;
    }

    public boolean isIpBlocked(String ip) {
        if (ip == null) return false;
        Attempt a = ipAttempts.get(ip);
        boolean blocked = a != null && a.lockUntil > Instant.now().toEpochMilli();
        if (blocked) log.debug("IP {} is blocked until {}", ip, a.lockUntil);
        return blocked;
    }

    public int remainingAttemptsForUser(String username) {
        if (username == null) return threshold;
        Attempt a = userAttempts.get(username);
        return a == null ? threshold : Math.max(0, threshold - a.count);
    }

    // New helper: returns remaining lock seconds for user or 0 if not locked
    public long getUserLockRemainingSeconds(String username) {
        if (username == null) return 0L;
        Attempt a = userAttempts.get(username);
        if (a == null || a.lockUntil <= Instant.now().toEpochMilli()) return 0L;
        long millis = a.lockUntil - Instant.now().toEpochMilli();
        long secs = (millis + 999) / 1000; // ceil
        return secs;
    }

    // New helper: returns unlock epoch millis or 0 if not locked
    public long getUserUnlockEpochMillis(String username) {
        if (username == null) return 0L;
        Attempt a = userAttempts.get(username);
        if (a == null || a.lockUntil <= Instant.now().toEpochMilli()) return 0L;
        return a.lockUntil;
    }

    // New helper for IP locks
    public long getIpLockRemainingSeconds(String ip) {
        if (ip == null) return 0L;
        Attempt a = ipAttempts.get(ip);
        if (a == null || a.lockUntil <= Instant.now().toEpochMilli()) return 0L;
        long millis = a.lockUntil - Instant.now().toEpochMilli();
        long secs = (millis + 999) / 1000;
        return secs;
    }

    public long getIpUnlockEpochMillis(String ip) {
        if (ip == null) return 0L;
        Attempt a = ipAttempts.get(ip);
        if (a == null || a.lockUntil <= Instant.now().toEpochMilli()) return 0L;
        return a.lockUntil;
    }

    private static class Attempt {
        int count = 0;
        long lockUntil = 0L;
    }
}
