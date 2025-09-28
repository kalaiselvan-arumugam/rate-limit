package com.example.ratelimitdemo.config;

import com.example.ratelimitdemo.service.RateLimiterService;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.*;

public class RateLimitInterceptorBypassTest {

    @Test
    void testInterceptorBypassesNonRateLimitedPath() throws Exception {
        RateLimiterService rls = new RateLimiterService();
        RateLimitInterceptor interceptor = new RateLimitInterceptor(rls);
        // set config values via reflection
        Field f1 = interceptor.getClass().getDeclaredField("cfgShortCapacity");
        f1.setAccessible(true);
        f1.setInt(interceptor, 5);
        Field f2 = interceptor.getClass().getDeclaredField("cfgShortWindowSeconds");
        f2.setAccessible(true);
        f2.setInt(interceptor, 5);
        Field f3 = interceptor.getClass().getDeclaredField("cfgMinuteCapacity");
        f3.setAccessible(true);
        f3.setInt(interceptor, 10);
        Field f4 = interceptor.getClass().getDeclaredField("cfgMinuteWindowSeconds");
        f4.setAccessible(true);
        f4.setInt(interceptor, 60);

        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken("bob", "x"));

        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/api/test2");
        MockHttpServletResponse resp = new MockHttpServletResponse();

        boolean allowed = interceptor.preHandle(req, resp, new Object());
        assertTrue(allowed);
        // headers should not be set for bypassed path
        assertNull(resp.getHeader("X-RateLimit-Short-Remaining"));
    }
}

