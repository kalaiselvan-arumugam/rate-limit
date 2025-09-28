package com.example.ratelimitdemo.config;

import com.example.ratelimitdemo.service.FailedLoginService;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

public class PreAuthFilterExtraTest {

    @Test
    void testMalformedAuthorizationHeaderPassesThrough() throws Exception {
        FailedLoginService fls = new FailedLoginService(5, 1);
        PreAuthFilter filter = new PreAuthFilter(fls);

        MockHttpServletRequest req = new MockHttpServletRequest();
        MockHttpServletResponse resp = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        req.addHeader("Authorization", "Basic !!!notbase64!!!");

        filter.doFilter(req, resp, chain);

        assertEquals(200, resp.getStatus());
    }

    @Test
    void testXForwardedForUsedForIpAndBlocks() throws Exception {
        FailedLoginService fls = new FailedLoginService(1, 1);
        String ip = "9.8.7.6";
        // record a failed attempt to block the ip
        fls.recordFailed("someone", ip);

        PreAuthFilter filter = new PreAuthFilter(fls);

        MockHttpServletRequest req = new MockHttpServletRequest();
        MockHttpServletResponse resp = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        String username = "user";
        String cred = username + ":pw";
        String encoded = Base64.getEncoder().encodeToString(cred.getBytes(StandardCharsets.UTF_8));
        req.addHeader("Authorization", "Basic " + encoded);
        req.addHeader("X-Forwarded-For", ip + ", 1.2.3.4");

        filter.doFilter(req, resp, chain);

        assertEquals(429, resp.getStatus());
        // PreAuthFilter sets X-Auth-Blocked-User to the username extracted from Basic auth
        assertEquals(username, resp.getHeader("X-Auth-Blocked-User"));
    }
}
