package com.example.ratelimitdemo.controller;

import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

import static org.junit.jupiter.api.Assertions.*;

public class DemoControllerAnonymousTest {

    @Test
    void testAnonymousAccessUsesAnonymousName() {
        DemoController ctrl = new DemoController();
        Authentication auth = null;

        ResponseEntity<String> r1 = ctrl.test1(auth);
        assertEquals(200, r1.getStatusCodeValue());
        assertTrue(r1.getBody().contains("anonymous") || r1.getBody().contains("test1"));

        ResponseEntity<String> r2 = ctrl.test2(auth);
        assertEquals(200, r2.getStatusCodeValue());
        assertTrue(r2.getBody().contains("anonymous") || r2.getBody().contains("test2"));

        ResponseEntity<String> r3 = ctrl.test3(auth);
        assertEquals(200, r3.getStatusCodeValue());
        assertTrue(r3.getBody().contains("anonymous") || r3.getBody().contains("test3"));
    }
}

