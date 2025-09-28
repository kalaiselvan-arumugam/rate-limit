package com.example.ratelimitdemo;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class RateLimitDemoApplicationTest {

    @Test
    void mainMethodRuns() {
        // just ensure main does not throw
        RateLimitDemoApplication.main(new String[]{});
        assertTrue(true);
    }
}

