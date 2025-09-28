package com.example.ratelimitdemo.service;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class RateLimiterService {

    private final Map<String, TokenBucket> buckets = new ConcurrentHashMap<>();

    /**
     * Try to consume a token for the given key (typically username+endpoint).
     * Returns true if allowed, false if rate limit exceeded.
     */
    public boolean tryConsume(String key, int capacity, double refillTokensPerSecond) {
        TokenBucket bucket = buckets.computeIfAbsent(key, k -> new TokenBucket(capacity, refillTokensPerSecond));
        return bucket.tryConsume();
    }

    /**
     * Composite consume: consumes one token from both short and minute buckets atomically (with rollback).
     */
    public boolean tryConsumeComposite(String shortKey, int shortCapacity, double shortRefillTokensPerSecond,
                                       String minuteKey, int minuteCapacity, double minuteRefillTokensPerSecond) {
        TokenBucket shortBucket = buckets.computeIfAbsent(shortKey, k -> new TokenBucket(shortCapacity, shortRefillTokensPerSecond));
        TokenBucket minuteBucket = buckets.computeIfAbsent(minuteKey, k -> new TokenBucket(minuteCapacity, minuteRefillTokensPerSecond));

        // First try short bucket
        boolean shortAllowed = shortBucket.tryConsume();
        if (!shortAllowed) {
            return false;
        }
        // Then try minute bucket; if fails, roll back short bucket by returning one token
        boolean minuteAllowed = minuteBucket.tryConsume();
        if (minuteAllowed) {
            return true;
        }
        // rollback short bucket
        shortBucket.addTokens(1.0);
        return false;
    }

    /**
     * Return snapshot information about the bucket for headers: capacity, remaining tokens (floor), and retry-after seconds.
     */
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
                return true;
            }
            return false;
        }

        synchronized void addTokens(double amount) {
            refill();
            tokens = Math.min(capacity, tokens + amount);
        }

        synchronized double getTokens() {
            refill();
            return tokens;
        }

        private void refill() {
            long now = Instant.now().toEpochMilli();
            long deltaMillis = now - lastRefillEpochMilli;
            if (deltaMillis <= 0) return;
            double add = (deltaMillis / 1000.0) * refillTokensPerSecond;
            tokens = Math.min(capacity, tokens + add);
            lastRefillEpochMilli = now;
        }
    }
}
