package com.example.ratelimitdemo.service;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class RateLimiterReflectionTest {

    @Test
    void testRefillDeltaLessOrEqualZeroPath() throws Exception {
        RateLimiterService svc = new RateLimiterService();
        String key = "rfl:test";
        // create the bucket
        assertTrue(svc.tryConsume(key, 3, 1.0));

        // access private buckets map and TokenBucket instance
        Field bucketsField = RateLimiterService.class.getDeclaredField("buckets");
        bucketsField.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<String, Object> buckets = (Map<String, Object>) bucketsField.get(svc);
        assertTrue(buckets.containsKey(key));
        Object tokenBucket = buckets.get(key);
        assertNotNull(tokenBucket);

        // set lastRefillEpochMilli to future so deltaMillis <= 0 in refill()
        Field lastRefill = tokenBucket.getClass().getDeclaredField("lastRefillEpochMilli");
        lastRefill.setAccessible(true);
        long future = System.currentTimeMillis() + 10_000L;
        lastRefill.setLong(tokenBucket, future);

        // calling getBucketInfo should invoke getTokens which calls refill() and take the deltaMillis <=0 branch
        RateLimiterService.BucketInfo info = svc.getBucketInfo(key, 3, 1.0);
        assertNotNull(info);
        // remaining should be >=0 and <= capacity
        assertTrue(info.getRemaining() >= 0 && info.getRemaining() <= 3);
    }

    @Test
    void testAddTokensCapsAtCapacity() throws Exception {
        RateLimiterService svc = new RateLimiterService();
        String key = "add:test";
        // consume to create bucket and reduce tokens
        assertTrue(svc.tryConsume(key, 2, 0.0));
        assertTrue(svc.tryConsume(key, 2, 0.0));
        // now tokens should be 0
        RateLimiterService.BucketInfo info0 = svc.getBucketInfo(key, 2, 0.0);
        assertEquals(0, info0.getRemaining());

        // call tryConsumeComposite with minute bucket allowing to trigger addTokens rollback path
        boolean ok = svc.tryConsumeComposite(key + ":short", 2, 0.0, key + ":minute", 0, 0.0);
        assertFalse(ok);

        // now ensure capacity was not exceeded by addTokens
        RateLimiterService.BucketInfo infoAfter = svc.getBucketInfo(key + ":short", 2, 0.0);
        assertTrue(infoAfter.getRemaining() <= 2);
    }
}

