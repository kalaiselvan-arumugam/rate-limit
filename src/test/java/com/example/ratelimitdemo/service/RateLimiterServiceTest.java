package com.example.ratelimitdemo.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class RateLimiterServiceTest {

    @Test
    void testSimpleTryConsumeAndRefill() throws Exception {
        RateLimiterService svc = new RateLimiterService();
        String key = "user:test";
        // capacity 2, no refill
        boolean a1 = svc.tryConsume(key, 2, 0.0);
        boolean a2 = svc.tryConsume(key, 2, 0.0);
        boolean a3 = svc.tryConsume(key, 2, 0.0);
        assertTrue(a1);
        assertTrue(a2);
        assertFalse(a3);

        RateLimiterService.BucketInfo info = svc.getBucketInfo(key, 2, 0.0);
        assertEquals(0, info.getRemaining());
        // when refill is zero and tokens < 1, retryAfter should be Integer.MAX_VALUE
        assertEquals(Integer.MAX_VALUE, info.getRetryAfterSeconds());

        // wait a bit and ensure no refill happens (refill=0)
        Thread.sleep(200);
        RateLimiterService.BucketInfo info2 = svc.getBucketInfo(key, 2, 0.0);
        assertEquals(0, info2.getRemaining());

        // Now use a bucket with refill 1 token/sec and capacity 2: consume both, then wait to refill one
        String k2 = "user:test2";
        assertTrue(svc.tryConsume(k2, 2, 1.0));
        assertTrue(svc.tryConsume(k2, 2, 1.0));
        assertFalse(svc.tryConsume(k2, 2, 1.0));
        // wait ~1100ms for one token
        Thread.sleep(1100);
        RateLimiterService.BucketInfo info3 = svc.getBucketInfo(k2, 2, 1.0);
        assertTrue(info3.getRemaining() >= 1);
        // since at least one token is available, retryAfter should be 0
        assertEquals(0, info3.getRetryAfterSeconds());
    }

    @Test
    void testTryConsumeCompositeRollback() {
        RateLimiterService svc = new RateLimiterService();
        String shortKey = "u:ep:short";
        String minuteKey = "u:ep:minute";

        // short capacity 1 (allows one), minute capacity 0 (never allows)
        boolean ok = svc.tryConsumeComposite(shortKey, 1, 0.0, minuteKey, 0, 0.0);
        assertFalse(ok, "Composite should fail because minute bucket has capacity 0");

        // short bucket should have been rolled back to 1 remaining
        RateLimiterService.BucketInfo shortInfo = svc.getBucketInfo(shortKey, 1, 0.0);
        assertEquals(1, shortInfo.getRemaining());
    }

    @Test
    void testCompositeSuccessDecrementsBothBuckets() {
        RateLimiterService svc = new RateLimiterService();
        String s = "s:ep:short";
        String m = "s:ep:minute";

        // both capacities 2 with no refill
        boolean ok = svc.tryConsumeComposite(s, 2, 0.0, m, 2, 0.0);
        assertTrue(ok);

        RateLimiterService.BucketInfo si = svc.getBucketInfo(s, 2, 0.0);
        RateLimiterService.BucketInfo mi = svc.getBucketInfo(m, 2, 0.0);
        assertEquals(1, si.getRemaining());
        assertEquals(1, mi.getRemaining());
    }

    @Test
    void testRetryAfterCalculationWithRefillRate() {
        RateLimiterService svc = new RateLimiterService();
        String k = "user:calc";
        // capacity 1, refill 0.5 tokens/sec -> when consumed, retryAfter = ceil(1 / 0.5) = 2
        assertTrue(svc.tryConsume(k, 1, 0.5));
        assertFalse(svc.tryConsume(k, 1, 0.5));
        RateLimiterService.BucketInfo info = svc.getBucketInfo(k, 1, 0.5);
        assertEquals(0, info.getRemaining());
        assertEquals(2, info.getRetryAfterSeconds());
    }
}
