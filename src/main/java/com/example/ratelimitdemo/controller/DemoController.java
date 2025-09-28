package com.example.ratelimitdemo.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class DemoController {

    private static final Logger log = LoggerFactory.getLogger(DemoController.class);

    @GetMapping("/test1")
    public ResponseEntity<String> test1(Authentication authentication) {
        String user = authentication != null ? authentication.getName() : "anonymous";
        log.info("Entering test1 endpoint - user={}", user);
        ResponseEntity<String> resp = ResponseEntity.ok("Hello from test1 (rate limited) to " + user);
        log.debug("test1 response for user={}: {}", user, resp.getBody());
        return resp;
    }

    @GetMapping("/test2")
    public ResponseEntity<String> test2(Authentication authentication) {
        String user = authentication != null ? authentication.getName() : "anonymous";
        log.info("Entering test2 endpoint - user={}", user);
        ResponseEntity<String> resp = ResponseEntity.ok("Hello " + user + " from test2 (no rate limit)");
        log.debug("test2 response for user={}: {}", user, resp.getBody());
        return resp;
    }

    @GetMapping("/test3")
    public ResponseEntity<String> test3(Authentication authentication) {
        String user = authentication != null ? authentication.getName() : "anonymous";
        log.info("Entering test3 endpoint - user={}", user);
        ResponseEntity<String> resp = ResponseEntity.ok("Hello from test3 (rate limited) to " + user);
        log.debug("test3 response for user={}: {}", user, resp.getBody());
        return resp;
    }
}
