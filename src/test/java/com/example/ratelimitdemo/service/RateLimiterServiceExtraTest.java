package com.example.ratelimitdemo.service;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.*;

public class RateLimiterServiceExtraTest {

    @Test
    void testTryConsumeCompositeShortEmptyFails() {
        RateLimiterService svc = new RateLimiterService();
        String shortKey = "short:zero";
        String minuteKey = "minute:ok";

        // create a short bucket with capacity 0 -> tryConsume should fail immediately
        boolean ok = svc.tryConsumeComposite(shortKey, 0, 0.0, minuteKey, 5, 1.0);
        assertFalse(ok);

        // minute bucket should remain untouched (should have full capacity)
        RateLimiterService.BucketInfo mi = svc.getBucketInfo(minuteKey, 5, 1.0);
        assertEquals(5, mi.getCapacity());
        assertTrue(mi.getRemaining() <= 5);
    }

    @Test
    void testGetBucketInfoFractionalTokensRetryCalc() throws Exception {
        RateLimiterService svc = new RateLimiterService();
        String key = "frac:test";
        // create bucket
        assertTrue(svc.tryConsume(key, 2, 0.5));

        // reflect into token bucket and set tokens to 0.3
        Field bucketsField = RateLimiterService.class.getDeclaredField("buckets");
        bucketsField.setAccessible(true);
        @SuppressWarnings("unchecked")
        java.util.Map<String, Object> buckets = (java.util.Map<String, Object>) bucketsField.get(svc);
        Object tb = buckets.get(key);
        assertNotNull(tb);
        Field tokensField = tb.getClass().getDeclaredField("tokens");
        tokensField.setAccessible(true);
        tokensField.setDouble(tb, 0.3);

        // refill rate is 0.5 -> missing = 0.7 -> retryAfter = ceil(0.7/0.5)=2
        RateLimiterService.BucketInfo info = svc.getBucketInfo(key, 2, 0.5);
        assertEquals(2, info.getRetryAfterSeconds());
    }
}

