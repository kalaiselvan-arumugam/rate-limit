package com.example.ratelimitdemo.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class FailedLoginServiceNullsTest {

    @Test
    void testNullInputsAndDefaults() {
        FailedLoginService svc = new FailedLoginService(3, 1);

        // calling with nulls should not throw
        svc.recordFailed(null, null);
        svc.recordSuccess(null, null);

        // remainingAttemptsForUser for unknown returns threshold
        assertEquals(3, svc.remainingAttemptsForUser("unknown_user"));

        // isUserBlocked and isIpBlocked for null should be false
        assertFalse(svc.isUserBlocked(null));
        assertFalse(svc.isIpBlocked(null));
    }
}

