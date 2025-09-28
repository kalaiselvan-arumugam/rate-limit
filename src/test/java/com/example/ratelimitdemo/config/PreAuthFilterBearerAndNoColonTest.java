package com.example.ratelimitdemo.config;

import com.example.ratelimitdemo.service.FailedLoginService;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

public class PreAuthFilterBearerAndNoColonTest {

    @Test
    void testBearerAuthHeaderPassesThrough() throws Exception {
        FailedLoginService fls = new FailedLoginService(5, 1);
        PreAuthFilter filter = new PreAuthFilter(fls);

        MockHttpServletRequest req = new MockHttpServletRequest();
        MockHttpServletResponse resp = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        req.addHeader("Authorization", "Bearer sometoken");

        filter.doFilter(req, resp, chain);

        assertEquals(200, resp.getStatus());
    }

    @Test
    void testBasicAuthNoColonDecodedUsesWholeAsUsername() throws Exception {
        FailedLoginService fls = new FailedLoginService(5, 1);
        PreAuthFilter filter = new PreAuthFilter(fls);

        MockHttpServletRequest req = new MockHttpServletRequest();
        MockHttpServletResponse resp = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        String cred = "singleusername";
        String encoded = Base64.getEncoder().encodeToString(cred.getBytes(StandardCharsets.UTF_8));
        req.addHeader("Authorization", "Basic " + encoded);

        filter.doFilter(req, resp, chain);

        assertEquals(200, resp.getStatus());
    }
}

