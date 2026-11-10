package com.notare.auth;

import com.fasterxml.jackson.databind.JsonNode;
import com.notare.support.CapturingEmailSender;
import com.notare.support.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

class PasswordResetTest extends IntegrationTest {

    @Autowired
    CapturingEmailSender emails;

    private void requestReset(String email) throws Exception {
        JsonNode body = call(post("/api/auth/password-reset/request"), null, Map.of("email", email), 200);
        assertThat(body.get("message").asText()).isEqualTo("If an account uses that email, a reset link is on its way.");
    }

    @Test
    void resetLinkSetsANewPasswordAndSignsOutOldSessions() throws Exception {
        Account tutor = register("TUTOR");
        requestReset(tutor.email());
        String token = emails.next(tutor.email(), "Reset your Notare password").token();

        // Old sessions are issued at least a second before the change, so they end up older than it.
        Thread.sleep(1100);
        call(post("/api/auth/password-reset/confirm"), null, Map.of("token", token, "newPassword", "brand-new-pass"), 200);

        call(post("/api/auth/login"), null, Map.of("email", tutor.email(), "password", "password123"), 401);
        JsonNode login = data(post("/api/auth/login"), null, Map.of("email", tutor.email(), "password", "brand-new-pass"), 200);
        assertThat(login.get("emailVerified").asBoolean()).isTrue();

        call(get("/api/courses"), tutor.token(), null, 401);
        call(get("/api/courses"), login.get("token").asText(), null, 200);
    }

    @Test
    void aResetLinkWorksOnlyOnce() throws Exception {
        Account student = register("STUDENT");
        requestReset(student.email());
        String token = emails.next(student.email(), "Reset").token();

        call(post("/api/auth/password-reset/confirm"), null, Map.of("token", token, "newPassword", "first-new-pass"), 200);
        JsonNode reuse = call(post("/api/auth/password-reset/confirm"), null, Map.of("token", token, "newPassword", "second-new-pass"), 400);
        assertThat(reuse.get("message").asText()).contains("invalid or has expired");
    }

    @Test
    void requestingAgainVoidsTheEarlierLink() throws Exception {
        Account tutor = register("TUTOR");
        requestReset(tutor.email());
        String first = emails.next(tutor.email(), "Reset").token();
        requestReset(tutor.email());
        String second = emails.next(tutor.email(), "Reset").token();

        call(post("/api/auth/password-reset/confirm"), null, Map.of("token", first, "newPassword", "some-new-pass"), 400);
        call(post("/api/auth/password-reset/confirm"), null, Map.of("token", second, "newPassword", "some-new-pass"), 200);
    }

    @Test
    void unknownEmailGetsTheSameAnswerAndNoEmail() throws Exception {
        String nobody = "nobody-" + UUID.randomUUID() + "@test.notare";
        requestReset(nobody);
        assertThat(emails.noneTo(nobody)).isTrue();
    }

    @Test
    void madeUpTokensAndShortPasswordsAreRejected() throws Exception {
        call(post("/api/auth/password-reset/confirm"), null, Map.of("token", "not-a-real-token", "newPassword", "whatever-pass"), 400);

        Account tutor = register("TUTOR");
        requestReset(tutor.email());
        String token = emails.next(tutor.email(), "Reset").token();
        call(post("/api/auth/password-reset/confirm"), null, Map.of("token", token, "newPassword", "short"), 400);
    }
}
