package com.example.ratelimitdemo.config;

import com.example.ratelimitdemo.service.RateLimiterService;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.*;

public class RateLimitInterceptorTest {

    private void setField(Object target, String name, int value) throws Exception {
        Field f = target.getClass().getDeclaredField(name);
        f.setAccessible(true);
        f.setInt(target, value);
    }

    @Test
    void testAllowedRequestSetsHeaders() throws Exception {
        RateLimiterService rls = new RateLimiterService();
        RateLimitInterceptor interceptor = new RateLimitInterceptor(rls);
        // set config values via reflection
        setField(interceptor, "cfgShortCapacity", 5);
        setField(interceptor, "cfgShortWindowSeconds", 5);
        setField(interceptor, "cfgMinuteCapacity", 10);
        setField(interceptor, "cfgMinuteWindowSeconds", 60);

        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken("bob", "x"));

        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/api/test1");
        MockHttpServletResponse resp = new MockHttpServletResponse();

        boolean allowed = interceptor.preHandle(req, resp, new Object());
        assertTrue(allowed);
        assertNotNull(resp.getHeader("X-RateLimit-Short-Remaining"));
        assertNotNull(resp.getHeader("X-RateLimit-Minute-Remaining"));
    }

    @Test
    void testBlockedRequestReturns429() throws Exception {
        RateLimiterService rls = new RateLimiterService();
        RateLimitInterceptor interceptor = new RateLimitInterceptor(rls);
        // configure to block by setting minute capacity to 0
        setField(interceptor, "cfgShortCapacity", 1);
        setField(interceptor, "cfgShortWindowSeconds", 5);
        setField(interceptor, "cfgMinuteCapacity", 0);
        setField(interceptor, "cfgMinuteWindowSeconds", 60);

        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken("alice", "x"));

        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/api/test1");
        MockHttpServletResponse resp = new MockHttpServletResponse();

        boolean allowed = interceptor.preHandle(req, resp, new Object());
        assertFalse(allowed);
        assertEquals(429, resp.getStatus());
    }
}

