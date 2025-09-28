package com.example.ratelimitdemo.config;

import com.example.ratelimitdemo.service.FailedLoginService;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

public class PreAuthFilterResolveIpTest {

    @Test
    void testResolveIpWithXForwardedForAndRemoteAddr() throws Exception {
        FailedLoginService fls = new FailedLoginService(5, 1);
        PreAuthFilter filter = new PreAuthFilter(fls);

        MockHttpServletRequest req = new MockHttpServletRequest();
        req.addHeader("X-Forwarded-For", "203.0.113.5, 10.0.0.1");

        // use jakarta.servlet HttpServletRequest because project uses Jakarta API
        Method m = PreAuthFilter.class.getDeclaredMethod("resolveIp", jakarta.servlet.http.HttpServletRequest.class);
        m.setAccessible(true);
        String ip = (String) m.invoke(filter, req);
        assertEquals("203.0.113.5", ip);

        // when no X-Forwarded-For, should return remoteAddr
        MockHttpServletRequest req2 = new MockHttpServletRequest();
        req2.setRemoteAddr("192.0.2.1");
        String ip2 = (String) m.invoke(filter, req2);
        assertEquals("192.0.2.1", ip2);
    }
}
