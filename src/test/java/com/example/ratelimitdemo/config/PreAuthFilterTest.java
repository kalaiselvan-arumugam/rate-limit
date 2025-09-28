package com.example.ratelimitdemo.config;

import com.example.ratelimitdemo.service.FailedLoginService;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

public class PreAuthFilterTest {

    @Test
    void testBlockedUserGets429() throws Exception {
        FailedLoginService fls = new FailedLoginService(1, 1); // threshold=1 to block quickly
        String user = "bob";
        String ip = "127.0.0.1";
        // cause a failure to block the user
        fls.recordFailed(user, ip);

        PreAuthFilter filter = new PreAuthFilter(fls);

        MockHttpServletRequest req = new MockHttpServletRequest();
        MockHttpServletResponse resp = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        String cred = user + ":x";
        String encoded = Base64.getEncoder().encodeToString(cred.getBytes(StandardCharsets.UTF_8));
        req.addHeader("Authorization", "Basic " + encoded);
        req.setRemoteAddr(ip);

        filter.doFilter(req, resp, chain);

        assertEquals(429, resp.getStatus());
        assertEquals(user, resp.getHeader("X-Auth-Blocked-User"));
    }

    @Test
    void testAllowedRequestPassesThrough() throws Exception {
        FailedLoginService fls = new FailedLoginService(5, 1);
        PreAuthFilter filter = new PreAuthFilter(fls);

        MockHttpServletRequest req = new MockHttpServletRequest();
        MockHttpServletResponse resp = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        String cred = "alice:pwd";
        String encoded = Base64.getEncoder().encodeToString(cred.getBytes(StandardCharsets.UTF_8));
        req.addHeader("Authorization", "Basic " + encoded);

        filter.doFilter(req, resp, chain);

        assertEquals(200, resp.getStatus()); // MockFilterChain sets 200 when chain proceeds
    }
}

