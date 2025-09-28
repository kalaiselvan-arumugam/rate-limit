package com.example.ratelimitdemo.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class DemoController {

    @GetMapping("/test1")
    public ResponseEntity<String> test1(Authentication authentication) {
        String user = authentication != null ? authentication.getName() : "anonymous";
        return ResponseEntity.ok("Hello from test1 (rate limited) to " + user);
    }

    @GetMapping("/test2")
    public ResponseEntity<String> test2(Authentication authentication) {
        String user = authentication != null ? authentication.getName() : "anonymous";
        return ResponseEntity.ok("Hello " + user + " from test2 (no rate limit)");
    }

    @GetMapping("/test3")
    public ResponseEntity<String> test3(Authentication authentication) {
        String user = authentication != null ? authentication.getName() : "anonymous";
        return ResponseEntity.ok("Hello from test3 (rate limited) to " + user);
    }
}
