package com.notare.demo;

import com.fasterxml.jackson.databind.JsonNode;
import com.notare.support.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

class DemoRestrictionTest extends IntegrationTest {

    private static MockHttpServletRequestBuilder demo(String forwardedFor) {
        return post("/api/auth/demo").header("X-Forwarded-For", forwardedFor);
    }

    private static String randomIp() {
        int n = Math.abs(UUID.randomUUID().hashCode());
        return "10." + (n % 250) + "." + ((n / 250) % 250) + "." + ((n / 62500) % 250);
    }

    @Test
    void demoAccountsCannotUseSageOrCodeRun() throws Exception {
        JsonNode teacher = data(demo(randomIp()), null, Map.of("role", "TUTOR"), 201);
        assertThat(teacher.get("demo").asBoolean()).isTrue();
        String token = teacher.get("token").asText();

        call(post("/api/sage/chat"), token, Map.of("message", "hi"), 403);
        verifyNoInteractions(anthropicClient);

        JsonNode student = data(demo(randomIp()), null, Map.of("role", "STUDENT"), 201);
        call(post("/api/assignments/" + UUID.randomUUID() + "/run"), student.get("token").asText(),
                Map.of("code", "public class Main {}"), 403);
    }

    @Test
    void rateLimitKeysOnTheLastForwardedHopSoAClientCantSpoofItsWayAround() throws Exception {
        String proxySeenIp = randomIp();
        for (int i = 0; i < DemoService.MAX_DEMOS_PER_WINDOW; i++) {
            // A client rotating a fake first hop is still the same client to Railway's proxy.
            call(demo("203.0.113." + i + ", " + proxySeenIp), null, Map.of("role", "STUDENT"), 201);
        }
        call(demo("198.51.100.7, " + proxySeenIp), null, Map.of("role", "STUDENT"), 429);

        // A different client is unaffected.
        call(demo(randomIp()), null, Map.of("role", "STUDENT"), 201);
    }
}
