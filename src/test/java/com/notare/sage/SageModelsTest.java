package com.notare.sage;

import com.anthropic.core.JsonValue;
import com.anthropic.models.messages.ContentBlock;
import com.anthropic.models.messages.ContentBlockParam;
import com.anthropic.models.messages.DirectCaller;
import com.anthropic.models.messages.Message;
import com.anthropic.models.messages.MessageCreateParams;
import com.anthropic.models.messages.MessageParam;
import com.anthropic.models.messages.OutputConfig;
import com.anthropic.models.messages.StopReason;
import com.anthropic.models.messages.StructuredMessage;
import com.anthropic.models.messages.StructuredMessageCreateParams;
import com.anthropic.models.messages.TextBlock;
import com.anthropic.models.messages.ThinkingBlock;
import com.anthropic.models.messages.ToolResultBlockParam;
import com.anthropic.models.messages.ToolUseBlock;
import com.fasterxml.jackson.databind.JsonNode;
import com.notare.support.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * Sage on Claude Sonnet 5.5 and Haiku 5.5: which model each job uses with which settings, and the
 * two things those models need from the chat loop. Their thinking blocks must go back to the API
 * unchanged, and the stored history must never be edited (so a turn with several tool calls is
 * kept whole and every call gets a result). Refusals come back as a 200 and must not be read as
 * an answer.
 */
class SageModelsTest extends IntegrationTest {

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
                .caller(DirectCaller.builder().build())
                .build());
    }

    private static ContentBlock text(String text) {
        return ContentBlock.ofText(TextBlock.builder().text(text).citations(List.of()).build());
    }

    private static ContentBlock thinking(String signature) {
        return ContentBlock.ofThinking(ThinkingBlock.builder().thinking("").signature(signature).build());
    }

    private List<MessageCreateParams> chatRequests() {
        ArgumentCaptor<MessageCreateParams> captor = ArgumentCaptor.forClass(MessageCreateParams.class);
        verify(anthropicClient.messages(), atLeastOnce()).create(captor.capture());
        return captor.getAllValues();
    }

    private static List<ContentBlockParam> blocks(MessageParam message) {
        return message.content().asBlockParams();
    }

    @Test
    void chatRunsOnSonnetWithCachingAndSendsThinkingBack() throws Exception {
        Account tutor = register("TUTOR");
        Message lookup = response(thinking("sig-1"), toolUse("list_courses", Map.of()));
        Message answer = response(text("You don't have any courses yet."));
        when(anthropicClient.messages().create(any(MessageCreateParams.class))).thenReturn(lookup, answer);

        data(post("/api/sage/chat"), tutor.token(), Map.of("message", "What courses do I have?"), 200);

        List<MessageCreateParams> requests = chatRequests();
        assertThat(requests).hasSize(2);

        MessageCreateParams first = requests.get(0);
        assertThat(first.model().asString()).isEqualTo(SageModels.SONNET);
        assertThat(first.cacheControl()).isPresent();
        assertThat(first.outputConfig().orElseThrow().effort()).contains(OutputConfig.Effort.MEDIUM);
        assertThat(first.maxTokens()).isGreaterThanOrEqualTo(16000L);
        assertThat(first._additionalBodyProperties().get("thinking").toString())
                .contains("adaptive").contains("drop_block");
        assertThat(String.join(",", first._headers().values("anthropic-beta")))
                .contains(SageModels.FALLBACK_BETA).contains(SageModels.THINKING_BINDING_BETA);

        // The second round trip carries the first answer back with its thinking block intact.
        List<MessageParam> history = requests.get(1).messages();
        assertThat(history).hasSize(3); // user question, assistant (thinking + tool call), tool result
        List<ContentBlockParam> assistantTurn = blocks(history.get(1));
        assertThat(assistantTurn.get(0).isThinking()).isTrue();
        assertThat(assistantTurn.get(0).asThinking().signature()).isEqualTo("sig-1");
        assertThat(assistantTurn.get(1).isToolUse()).isTrue();
    }

    @Test
    void writeTurnIsKeptWholeAndEveryToolCallGetsAResult() throws Exception {
        Account tutor = register("TUTOR");
        String courseId = createCourse(tutor).get("id").asText();

        Message proposal = response(thinking("sig-2"), text("I'll add both units."),
                toolUse("list_courses", Map.of()),
                toolUse("create_topic", Map.of("courseId", courseId, "name", "Recursion")),
                toolUse("create_topic", Map.of("courseId", courseId, "name", "Sorting")));
        when(anthropicClient.messages().create(any(MessageCreateParams.class))).thenReturn(proposal);

        JsonNode turn = data(post("/api/sage/chat"), tutor.token(), Map.of("message", "Add two units"), 200);
        String conversationId = turn.get("conversationId").asText();
        JsonNode pending = turn.get("messages").get(0);
        assertThat(pending.get("pendingAction").get("description").asText()).contains("Recursion");

        Message followUp = response(text("Added Recursion. Want me to add Sorting too?"));
        when(anthropicClient.messages().create(any(MessageCreateParams.class))).thenReturn(followUp);
        data(post("/api/sage/chat/" + conversationId + "/messages/" + pending.get("id").asText() + "/confirm"),
                tutor.token(), null, 200);

        List<MessageCreateParams> requests = chatRequests();
        List<MessageParam> history = requests.get(requests.size() - 1).messages();

        // The stored assistant turn went back exactly as the model wrote it: all three tool calls.
        List<ContentBlockParam> assistantTurn = blocks(history.get(1));
        assertThat(assistantTurn.stream().filter(ContentBlockParam::isToolUse).count()).isEqualTo(3);
        assertThat(assistantTurn.get(0).isThinking()).isTrue();

        // ...and the next message answers every call, in order.
        List<ToolResultBlockParam> results = blocks(history.get(2)).stream()
                .map(ContentBlockParam::asToolResult).toList();
        assertThat(results).hasSize(3);
        assertThat(results.get(0).isError()).isNotEqualTo(Optional.of(true)); // the read ran
        assertThat(results.get(1).isError()).isNotEqualTo(Optional.of(true)); // the confirmed write
        assertThat(results.get(2).isError()).contains(true);                   // the second write
        assertThat(results.get(2).content().orElseThrow().asString()).contains("only one action");

        // Only the confirmed write ran.
        JsonNode topics = getJson("/api/courses/" + courseId + "/topics", tutor.token(), 200).get("data");
        assertThat(topics).hasSize(1);
        assertThat(topics.get(0).get("name").asText()).isEqualTo("Recursion");
    }

    @Test
    void chatRefusalGetsAPlainReplyAndTheConversationCarriesOn() throws Exception {
        Account tutor = register("TUTOR");
        Message refused = response();
        when(refused.stopReason()).thenReturn(Optional.of(StopReason.REFUSAL));
        when(anthropicClient.messages().create(any(MessageCreateParams.class))).thenReturn(refused);

        JsonNode turn = data(post("/api/sage/chat"), tutor.token(), Map.of("message", "something odd"), 200);
        assertThat(turn.get("messages")).hasSize(1);
        assertThat(turn.get("messages").get(0).get("text").asText()).contains("can't help");

        Message normal = response(text("Sure, here's that."));
        when(anthropicClient.messages().create(any(MessageCreateParams.class))).thenReturn(normal);
        data(post("/api/sage/chat"), tutor.token(),
                Map.of("conversationId", turn.get("conversationId").asText(), "message", "something normal"), 200);
    }

    @Test
    @SuppressWarnings("unchecked")
    void reviewRunsOnSonnetAtMediumEffortAndARefusalIs422() throws Exception {
        Account tutor = register("TUTOR");
        Account student = register("STUDENT");
        JsonNode course = createCourse(tutor);
        join(student, course);
        UUID submissionId = submit(student, createAssignment(tutor, course), "public class Main {}");

        StructuredMessage<SageCodeReview> refused = mock(StructuredMessage.class);
        when(refused.stopReason()).thenReturn(Optional.of(StopReason.REFUSAL));
        when(refused.content()).thenReturn(List.of());
        when(anthropicClient.messages().create(any(StructuredMessageCreateParams.class))).thenReturn(refused);

        JsonNode body = call(post("/api/sage/review-submission"), tutor.token(),
                Map.of("submissionId", submissionId.toString()), 422);
        assertThat(body.get("message").asText()).contains("declined");

        ArgumentCaptor<StructuredMessageCreateParams> captor =
                ArgumentCaptor.forClass(StructuredMessageCreateParams.class);
        verify(anthropicClient.messages()).create(captor.capture());
        MessageCreateParams sent = captor.getValue().rawParams();
        assertThat(sent.model().asString()).isEqualTo(SageModels.SONNET);
        // Effort and the JSON format share output_config; neither may replace the other.
        OutputConfig outputConfig = sent.outputConfig().orElseThrow();
        assertThat(outputConfig.effort()).contains(OutputConfig.Effort.MEDIUM);
        assertThat(outputConfig.format()).isPresent();
        assertThat(sent._additionalBodyProperties()).containsKey("fallbacks");
    }

    @Test
    void progressSummaryRunsOnHaikuAtLowEffort() throws Exception {
        Account tutor = register("TUTOR");
        Account student = register("STUDENT");
        JsonNode course = createCourse(tutor);
        join(student, course);
        submit(student, createAssignment(tutor, course), "public class Main {}");

        // A response can open with an (empty) thinking block; the summary is the text after it.
        Message summaryResponse = response(thinking("sig-3"), text("Steady progress so far."));
        when(anthropicClient.messages().create(any(MessageCreateParams.class))).thenReturn(summaryResponse);

        JsonNode summary = data(get("/api/sage/student-progress/" + student.id()), tutor.token(), null, 200);
        assertThat(summary.get("summary").asText()).isEqualTo("Steady progress so far.");

        MessageCreateParams sent = chatRequests().get(0);
        assertThat(sent.model().asString()).isEqualTo(SageModels.HAIKU);
        assertThat(sent.outputConfig().orElseThrow().effort()).contains(OutputConfig.Effort.LOW);
        // Haiku 5.5 has no server-side fallback: sending one would be a 400.
        assertThat(sent._additionalBodyProperties()).doesNotContainKey("fallbacks");
    }
}
