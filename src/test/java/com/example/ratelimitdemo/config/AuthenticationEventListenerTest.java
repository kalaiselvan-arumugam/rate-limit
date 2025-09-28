package com.example.ratelimitdemo.config;

import com.example.ratelimitdemo.service.FailedLoginService;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockServletContext;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.security.authentication.event.AuthenticationFailureBadCredentialsEvent;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import static org.junit.jupiter.api.Assertions.*;

public class AuthenticationEventListenerTest {

    @Test
    void testSuccessAndFailureEventsUpdateFailedLoginService() {
        FailedLoginService fls = new FailedLoginService(2, 1);
        AuthenticationEventListener listener = new AuthenticationEventListener(fls);

        // simulate a request with IP
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.setRemoteAddr("10.0.0.1");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(req));

        // success event should reset attempts (no exception)
        var auth = new UsernamePasswordAuthenticationToken("joe", "x");
        AuthenticationSuccessEvent successEvent = new AuthenticationSuccessEvent(auth);
        listener.onApplicationEvent(successEvent);
        assertFalse(fls.isUserBlocked("joe"));

        // failure event should increment attempts
        AuthenticationFailureBadCredentialsEvent failEvent = new AuthenticationFailureBadCredentialsEvent(auth, new BadCredentialsException("bad"));
        listener.onApplicationEvent(failEvent);
        assertEquals(1, fls.remainingAttemptsForUser("joe"));

        // clear attributes and ensure listener handles unknown ip
        RequestContextHolder.resetRequestAttributes();
        listener.onApplicationEvent(successEvent); // should not throw
    }
}

