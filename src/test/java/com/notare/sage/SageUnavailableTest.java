package com.notare.sage;

import com.anthropic.models.messages.MessageCreateParams;
import com.anthropic.models.messages.StructuredMessageCreateParams;
import com.fasterxml.jackson.databind.JsonNode;
import com.notare.support.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * When the Anthropic call fails (production's key was revoked on 2026-11-10 and nothing said so),
 * every Sage endpoint answers 502 "Sage is unavailable right now", and the cause is logged.
 */
@ExtendWith(OutputCaptureExtension.class)
class SageUnavailableTest extends IntegrationTest {

    private static final String CAUSE = "401: invalid x-api-key (test)";

    @Test
    void reviewSubmissionIs502AndLogsTheCause(CapturedOutput output) throws Exception {
        Account tutor = register("TUTOR");
        Account student = register("STUDENT");
        JsonNode course = createCourse(tutor);
        join(student, course);
        UUID assignmentId = createAssignment(tutor, course);
        UUID submissionId = submit(student, assignmentId, "public class Main {}");

        when(anthropicClient.messages().create(any(StructuredMessageCreateParams.class)))
                .thenThrow(new RuntimeException(CAUSE));

        JsonNode body = call(post("/api/sage/review-submission"), tutor.token(),
                Map.of("submissionId", submissionId.toString()), 502);
        assertThat(body.get("message").asText()).isEqualTo("Sage is unavailable right now");
        assertThat(output).contains(CAUSE);
    }

    @Test
    void chatIs502AndLogsTheCause(CapturedOutput output) throws Exception {
        Account tutor = register("TUTOR");
        when(anthropicClient.messages().create(any(MessageCreateParams.class)))
                .thenThrow(new RuntimeException(CAUSE));

        call(post("/api/sage/chat"), tutor.token(), Map.of("message", "hi"), 502);
        assertThat(output).contains(CAUSE);
    }
}
