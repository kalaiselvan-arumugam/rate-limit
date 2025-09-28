package com.example.ratelimitdemo;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class RateLimitDemoApplicationInstanceTest {

    @Test
    void instantiateApplicationClass() {
        RateLimitDemoApplication app = new RateLimitDemoApplication();
        assertNotNull(app);
    }
}

