package com.notare.auth;

import com.notare.support.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.TestPropertySource;

import java.util.Map;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/** Runs with the account rate limits on (the other integration tests turn them off). */
@TestPropertySource(properties = "rate-limit.enabled=true")
class AuthRateLimitTest extends IntegrationTest {

    @Test
    void repeatedFailedLoginsForOneEmailAreCutOff() throws Exception {
        String email = "target-" + UUID.randomUUID() + "@test.notare";
        for (int i = 0; i < 10; i++) {
            call(post("/api/auth/login"), null, Map.of("email", email, "password", "guess" + i), 401);
        }
        call(post("/api/auth/login"), null, Map.of("email", email, "password", "guess-10"), 429);
    }

    @Test
    void resetEmailsToOneAddressAreCapped() throws Exception {
        String email = "inbox-" + UUID.randomUUID() + "@test.notare";
        for (int i = 0; i < 3; i++) {
            call(post("/api/auth/password-reset/request"), null, Map.of("email", email), 200);
        }
        call(post("/api/auth/password-reset/request"), null, Map.of("email", email), 429);
    }

    @Test
    void signUpsFromOneAddressAreCapped() throws Exception {
        for (int i = 0; i < 10; i++) {
            register("STUDENT");
        }
        call(post("/api/auth/register"), null, Map.of("name", "One Too Many",
                "email", "extra-" + UUID.randomUUID() + "@test.notare", "password", "password123", "role", "STUDENT"), 429);
    }
}
