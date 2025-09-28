package com.example.ratelimitdemo.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class FailedLoginServiceTest {

    @Test
    void testRecordFailedAndBlockAndReset() throws Exception {
        // threshold 2, lockMinutes 1
        FailedLoginService svc = new FailedLoginService(2, 1);
        String user = "alice";
        String ip = "1.2.3.4";

        assertFalse(svc.isUserBlocked(user));
        assertFalse(svc.isIpBlocked(ip));
        assertEquals(2, svc.remainingAttemptsForUser(user));

        svc.recordFailed(user, ip);
        assertFalse(svc.isUserBlocked(user));
        assertEquals(1, svc.remainingAttemptsForUser(user));

        svc.recordFailed(user, ip);
        assertTrue(svc.isUserBlocked(user));
        assertEquals(0, svc.remainingAttemptsForUser(user));

        // record success should reset
        svc.recordSuccess(user, ip);
        assertFalse(svc.isUserBlocked(user));
        assertFalse(svc.isIpBlocked(ip));
        assertEquals(2, svc.remainingAttemptsForUser(user));
    }
}

