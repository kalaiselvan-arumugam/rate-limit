package com.example.ratelimitdemo;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class RateLimitIntegrationTest {

    @LocalServerPort
    int port;

    @Autowired
    private TestRestTemplate restTemplate;

    private String url(String path) {
        return "http://localhost:" + port + path;
    }

    @Test
    void testNoRateLimitEndpoint() {
        ResponseEntity<String> resp = restTemplate.withBasicAuth("user", "password").getForEntity(url("/api/test2"), String.class);
        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertTrue(resp.getBody().contains("test2"));
    }

    @Test
    void testShortBurstRateLimitHeadersAndBlocking() {
        TestRestTemplate rt = restTemplate.withBasicAuth("admin", "admin");
        // perform 6 requests quickly against /api/test1
        int success = 0;
        int tooMany = 0;
        ResponseEntity<String> lastSuccess = null;
        ResponseEntity<String> last429 = null;
        for (int i = 0; i < 6; i++) {
            ResponseEntity<String> r = rt.getForEntity(url("/api/test1"), String.class);
            if (r.getStatusCode() == HttpStatus.OK) {
                success++;
                lastSuccess = r;
            } else if (r.getStatusCode().value() == 429) {
                tooMany++;
                last429 = r;
            }
        }
        assertEquals(5, success, "Expected 5 successes in burst");
        assertEquals(1, tooMany, "Expected 1 blocked request after burst");

        // check headers on a successful response
        assertNotNull(lastSuccess);
        assertTrue(lastSuccess.getHeaders().containsKey("X-RateLimit-Short-Remaining"));
        assertTrue(lastSuccess.getHeaders().containsKey("X-RateLimit-Minute-Remaining"));

        // check headers on 429 response
        assertNotNull(last429);
        assertTrue(last429.getHeaders().containsKey("Retry-After") || last429.getHeaders().containsKey("X-RateLimit-Short-Retry-After"));
    }
}
