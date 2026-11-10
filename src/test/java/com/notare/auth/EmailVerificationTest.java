package com.notare.auth;

import com.fasterxml.jackson.databind.JsonNode;
import com.notare.support.CapturingEmailSender;
import com.notare.support.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

class EmailVerificationTest extends IntegrationTest {

    @Autowired
    CapturingEmailSender emails;

    private boolean verified(Account account) throws Exception {
        return data(post("/api/auth/login"), null, Map.of("email", account.email(), "password", "password123"), 200)
                .get("emailVerified").asBoolean();
    }

    @Test
    void signUpSendsALinkThatConfirmsTheEmail() throws Exception {
        Account student = register("STUDENT");
        String token = emails.next(student.email(), "Confirm your email").token();
        assertThat(verified(student)).isFalse();

        call(post("/api/auth/verify-email/confirm"), null, Map.of("token", token), 200);
        assertThat(verified(student)).isTrue();

        // The link is single-use.
        call(post("/api/auth/verify-email/confirm"), null, Map.of("token", token), 400);
    }

    @Test
    void resendNeedsASignedInUserAndVoidsTheEarlierLink() throws Exception {
        call(post("/api/auth/verify-email/resend"), null, null, 401);

        Account tutor = register("TUTOR");
        String first = emails.next(tutor.email(), "Confirm your email").token();
        call(post("/api/auth/verify-email/resend"), tutor.token(), null, 200);
        String second = emails.next(tutor.email(), "Confirm your email").token();

        call(post("/api/auth/verify-email/confirm"), null, Map.of("token", first), 400);
        call(post("/api/auth/verify-email/confirm"), null, Map.of("token", second), 200);
    }

    @Test
    void aResetTokenCannotConfirmAnEmail() throws Exception {
        Account tutor = register("TUTOR");
        emails.next(tutor.email(), "Confirm your email");
        call(post("/api/auth/password-reset/request"), null, Map.of("email", tutor.email()), 200);
        String resetToken = emails.next(tutor.email(), "Reset").token();

        JsonNode body = call(post("/api/auth/verify-email/confirm"), null, Map.of("token", resetToken), 400);
        assertThat(body.get("message").asText()).contains("invalid or has expired");
    }
}
