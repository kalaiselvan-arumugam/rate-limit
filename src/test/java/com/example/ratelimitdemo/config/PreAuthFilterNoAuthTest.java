package com.example.ratelimitdemo.config;

import com.example.ratelimitdemo.service.FailedLoginService;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.*;

public class PreAuthFilterNoAuthTest {

    @Test
    void testNoAuthorizationHeaderPassesThrough() throws Exception {
        FailedLoginService fls = new FailedLoginService(5, 1);
        PreAuthFilter filter = new PreAuthFilter(fls);

        MockHttpServletRequest req = new MockHttpServletRequest();
        MockHttpServletResponse resp = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(req, resp, chain);

        assertEquals(200, resp.getStatus());
    }
}

