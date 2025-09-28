package com.example.ratelimitdemo.config;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;

public class SecurityConfigTest {

    @Test
    void usersBeanContainsConfiguredUsers() {
        SecurityConfig cfg = new SecurityConfig();
        PasswordEncoder encoder = cfg.passwordEncoder();
        UserDetailsService uds = cfg.users(encoder);
        assertNotNull(uds);
        // verify the known users can be loaded
        var u1 = uds.loadUserByUsername("user");
        assertNotNull(u1);
        assertEquals("user", u1.getUsername());
        var a = u1.getAuthorities();
        assertFalse(a.isEmpty());

        var u2 = uds.loadUserByUsername("admin");
        assertNotNull(u2);
        assertEquals("admin", u2.getUsername());
    }
}

