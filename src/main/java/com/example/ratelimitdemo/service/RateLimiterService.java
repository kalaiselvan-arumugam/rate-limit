package com.example.ratelimitdemo.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class RateLimiterService {

    private static final Logger log = LoggerFactory.getLogger(RateLimiterService.class);

    private final Map<String, TokenBucket> buckets = new ConcurrentHashMap<>();

    public boolean tryConsume(String key, int capacity, double refillTokensPerSecond) {
        TokenBucket bucket = buckets.computeIfAbsent(key, k -> new TokenBucket(capacity, refillTokensPerSecond));
        boolean allowed = bucket.tryConsume();
        log.debug("tryConsume key={} capacity={} refill={} allowed={}", key, capacity, refillTokensPerSecond, allowed);
        return allowed;
    }

    public boolean tryConsumeComposite(String shortKey, int shortCapacity, double shortRefillTokensPerSecond,
                                       String minuteKey, int minuteCapacity, double minuteRefillTokensPerSecond) {
        TokenBucket shortBucket = buckets.computeIfAbsent(shortKey, k -> new TokenBucket(shortCapacity, shortRefillTokensPerSecond));
        TokenBucket minuteBucket = buckets.computeIfAbsent(minuteKey, k -> new TokenBucket(minuteCapacity, minuteRefillTokensPerSecond));

        // First try short bucket
        boolean shortAllowed = shortBucket.tryConsume();
        log.debug("tryConsumeComposite shortKey={} allowed={} tokensRemaining={} (capacity={})",
                shortKey, shortAllowed, shortBucket.peekTokens(), shortCapacity);
        if (!shortAllowed) {
            return false;
        }
        // Then try minute bucket; if fails, roll back short bucket by returning one token
        boolean minuteAllowed = minuteBucket.tryConsume();
        log.debug("tryConsumeComposite minuteKey={} allowed={} tokensRemaining={} (capacity={})",
                minuteKey, minuteAllowed, minuteBucket.peekTokens(), minuteCapacity);
        if (minuteAllowed) {
            return true;
        }
        // rollback short bucket
        shortBucket.addTokens(1.0);
        log.debug("Rolled back shortKey={} after minute bucket fail; tokensNow={}", shortKey, shortBucket.peekTokens());
        return false;
    }

    // Return snapshot information about the bucket for headers: capacity, remaining tokens (floor), and retry-after seconds.
    public BucketInfo getBucketInfo(String key, int capacity, double refillTokensPerSecond) {
        TokenBucket bucket = buckets.computeIfAbsent(key, k -> new TokenBucket(capacity, refillTokensPerSecond));
        double tokens = bucket.getTokens();
        int remaining = (int) Math.floor(tokens);
        int retryAfterSeconds = 0;
        if (tokens < 1.0) {
            double missing = 1.0 - tokens;
            if (refillTokensPerSecond > 0) {
                retryAfterSeconds = (int) Math.ceil(missing / refillTokensPerSecond);
            } else {
                retryAfterSeconds = Integer.MAX_VALUE;
            }
        }
        log.debug("getBucketInfo key={} capacity={} tokens={} remaining={} retryAfter={}", key, capacity, tokens, remaining, retryAfterSeconds);
        return new BucketInfo(capacity, remaining, retryAfterSeconds);
    }

    public static class BucketInfo {
        private final int capacity;
        private final int remaining;
        private final int retryAfterSeconds;

        public BucketInfo(int capacity, int remaining, int retryAfterSeconds) {
            this.capacity = capacity;
            this.remaining = remaining;
            this.retryAfterSeconds = retryAfterSeconds;
        }

        public int getCapacity() {
            return capacity;
        }

        public int getRemaining() {
            return remaining;
        }

        public int getRetryAfterSeconds() {
            return retryAfterSeconds;
        }
    }

    private static class TokenBucket {
        private final int capacity;
        private final double refillTokensPerSecond;
        private double tokens;
        private long lastRefillEpochMilli;

        TokenBucket(int capacity, double refillTokensPerSecond) {
            this.capacity = capacity;
            this.refillTokensPerSecond = refillTokensPerSecond;
            this.tokens = capacity;
            this.lastRefillEpochMilli = Instant.now().toEpochMilli();
        }

        synchronized boolean tryConsume() {
            refill();
            if (tokens >= 1.0) {
                tokens -= 1.0;
                log.trace("Token consumed; tokens now={} (capacity={})", tokens, capacity);
                return true;
            }
            log.trace("Token not available; tokens={} (capacity={})", tokens, capacity);
            return false;
        }

        synchronized void addTokens(double amount) {
            refill();
            tokens = Math.min(capacity, tokens + amount);
            log.trace("Added tokens amount={} tokensNow={} (capacity={})", amount, tokens, capacity);
        }

        synchronized double getTokens() {
            refill();
            return tokens;
        }

        synchronized double peekTokens() {
            refill();
            return tokens;
        }

        private void refill() {
            long now = Instant.now().toEpochMilli();
            long deltaMillis = now - lastRefillEpochMilli;
            if (deltaMillis <= 0) {
                log.trace("refill() skipped because deltaMillis<={}, lastRefill={}", deltaMillis, lastRefillEpochMilli);
                return;
            }
            double add = (deltaMillis / 1000.0) * refillTokensPerSecond;
            tokens = Math.min(capacity, tokens + add);
            lastRefillEpochMilli = now;
            log.trace("Refilled tokens by {} over {}ms; tokens={} (capacity={})", add, deltaMillis, tokens, capacity);
        }
    }
}
