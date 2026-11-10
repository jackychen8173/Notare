package com.notare.sage;

import com.anthropic.core.JsonValue;
import com.anthropic.models.messages.ContentBlock;
import com.anthropic.models.messages.Message;
import com.anthropic.models.messages.MessageCreateParams;
import com.anthropic.models.messages.TextBlock;
import com.anthropic.models.messages.ToolUseBlock;
import com.fasterxml.jackson.databind.JsonNode;
import com.notare.support.IntegrationTest;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * A Sage tool that fails in an ordinary, expected way (here get_rubric on an assignment with no
 * rubric) must come back to the model as an is_error tool result, not lose the whole chat turn.
 * The tool delegates into a @Transactional service that throws, inside SageChatService's own
 * transaction, which is exactly the rollback-only case flagged in CLAUDE.md.
 */
class SageChatToolErrorTest extends IntegrationTest {

    private static Message response(ContentBlock... blocks) {
        Message message = mock(Message.class);
        when(message.content()).thenReturn(List.of(blocks));
        return message;
    }

    private static ContentBlock toolUse(String name, Map<String, Object> input) {
        return ContentBlock.ofToolUse(ToolUseBlock.builder()
                .id("toolu_" + UUID.randomUUID().toString().replace("-", ""))
                .name(name)
                .input(JsonValue.from(input))
                .caller(com.anthropic.models.messages.DirectCaller.builder().build())
                .build());
    }

    private static ContentBlock text(String text) {
        return ContentBlock.ofText(TextBlock.builder().text(text).citations(List.of()).build());
    }

    @Test
    void readToolFailureBecomesToolErrorNotA500() throws Exception {
        Account tutor = register("TUTOR");
        UUID assignmentId = createAssignment(tutor, createCourse(tutor));

        Message first = response(toolUse("get_rubric", Map.of("assignmentId", assignmentId.toString())));
        Message second = response(text("That assignment doesn't have a rubric yet."));
        when(anthropicClient.messages().create(any(MessageCreateParams.class))).thenReturn(first, second);

        JsonNode turn = data(post("/api/sage/chat"), tutor.token(),
                Map.of("message", "What's the rubric for my loops assignment?"), 200);

        JsonNode messages = turn.get("messages");
        assertThat(messages).hasSize(3); // assistant tool call, tool result, final answer
        assertThat(messages.get(2).get("text").asText()).contains("doesn't have a rubric");

        // The turn was committed, not rolled back: it's there when the conversation is re-read.
        String conversationId = turn.get("conversationId").asText();
        JsonNode stored = getJson("/api/sage/chat/" + conversationId, tutor.token(), 200).get("data");
        assertThat(stored).hasSize(4); // the user's message plus the three above
    }

    @Test
    void writeToolValidationFailureBecomesToolErrorNotA500() throws Exception {
        Account tutor = register("TUTOR");

        Message first = response(toolUse("release_feedback", Map.of("submissionId", UUID.randomUUID().toString())));
        Message second = response(text("I couldn't find that submission."));
        when(anthropicClient.messages().create(any(MessageCreateParams.class))).thenReturn(first, second);

        JsonNode turn = data(post("/api/sage/chat"), tutor.token(),
                Map.of("message", "Release feedback for that submission"), 200);

        JsonNode messages = turn.get("messages");
        assertThat(messages).hasSize(3);
        assertThat(messages.get(0).get("pendingAction").isNull()).isTrue();
        assertThat(messages.get(2).get("text").asText()).contains("couldn't find");
    }

    @Test
    void confirmedWriteStaysDoneWhenTheFollowUpModelCallFails() throws Exception {
        Account tutor = register("TUTOR");
        JsonNode course = createCourse(tutor);
        String courseId = course.get("id").asText();

        Message proposal = response(text("I'll add that unit."),
                toolUse("create_topic", Map.of("courseId", courseId, "name", "Recursion")));
        when(anthropicClient.messages().create(any(MessageCreateParams.class))).thenReturn(proposal);

        JsonNode turn = data(post("/api/sage/chat"), tutor.token(), Map.of("message", "Add a Recursion unit"), 200);
        String conversationId = turn.get("conversationId").asText();
        JsonNode pending = turn.get("messages").get(0);
        assertThat(pending.get("pendingAction").get("description").asText()).contains("Recursion");

        // The tutor confirms, the write runs, then the model is unreachable for the follow-up.
        when(anthropicClient.messages().create(any(MessageCreateParams.class)))
                .thenThrow(new RuntimeException("model down"));
        String confirmPath = "/api/sage/chat/" + conversationId + "/messages/" + pending.get("id").asText() + "/confirm";
        call(post(confirmPath), tutor.token(), null, 502);

        JsonNode topics = getJson("/api/courses/" + courseId + "/topics", tutor.token(), 200).get("data");
        assertThat(topics).hasSize(1);
        assertThat(topics.get(0).get("name").asText()).isEqualTo("Recursion");

        // A retry must not run the write a second time.
        call(post(confirmPath), tutor.token(), null, 409);
        assertThat(getJson("/api/courses/" + courseId + "/topics", tutor.token(), 200).get("data")).hasSize(1);
    }
}
