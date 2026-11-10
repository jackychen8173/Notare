package com.notare.common;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Public, dependency-free liveness check. The landing and login pages call it as soon as they
 * load, so a backend that Railway's Serverless mode has put to sleep starts waking up while the
 * visitor is still reading, not after they click. It touches nothing (no database), so it's
 * cheap to call and safe to leave unauthenticated.
 */
@RestController
public class HealthController {

    @GetMapping("/api/health")
    public Map<String, String> health() {
        return Map.of("status", "ok");
    }
}
