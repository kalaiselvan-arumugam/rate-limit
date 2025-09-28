package com.example.ratelimitdemo.service;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.*;

public class FailedLoginServiceReflectionLockTest {

    @Test
    void testIsUserBlockedWithManualLock() throws Exception {
        FailedLoginService svc = new FailedLoginService(5, 15);
        String user = "manual";
        svc.recordFailed(user, null); // create attempt

        Field field = FailedLoginService.class.getDeclaredField("userAttempts");
        field.setAccessible(true);
        @SuppressWarnings("unchecked")
        java.util.Map<String, Object> map = (java.util.Map<String, Object>) field.get(svc);
        Object attempt = map.get(user);
        assertNotNull(attempt);
        Field lockUntil = attempt.getClass().getDeclaredField("lockUntil");
        lockUntil.setAccessible(true);
        long future = System.currentTimeMillis() + 60_000L;
        lockUntil.setLong(attempt, future);

        assertTrue(svc.isUserBlocked(user));
    }
}

