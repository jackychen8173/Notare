package com.notare.sage.chat.block;

import com.anthropic.models.messages.ContentBlock;
import com.anthropic.models.messages.ContentBlockParam;
import com.anthropic.models.messages.RedactedThinkingBlock;
import com.anthropic.models.messages.TextBlock;
import com.anthropic.models.messages.ThinkingBlock;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Thinking blocks have to survive the trip into the database and back out to the API unchanged,
 * including the usual case where the thinking text is empty and only the signature is set.
 */
class BlockConverterTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void thinkingBlocksRoundTripUnchanged() {
        List<ContentBlock> response = List.of(
                ContentBlock.ofThinking(ThinkingBlock.builder().thinking("").signature("sig-abc").build()),
                ContentBlock.ofRedactedThinking(RedactedThinkingBlock.builder().data("encrypted-xyz").build()),
                ContentBlock.ofText(TextBlock.builder().text("Done.").citations(List.of()).build()));

        String json = BlockConverter.serialize(BlockConverter.fromApiResponse(response), objectMapper);
        List<StoredBlock> stored = BlockConverter.deserialize(json, objectMapper);

        assertThat(stored).containsExactly(
                new StoredBlock.Thinking("", "sig-abc"),
                new StoredBlock.RedactedThinking("encrypted-xyz"),
                new StoredBlock.Text("Done."));

        List<ContentBlockParam> params = BlockConverter.toApiParams(stored);
        assertThat(params).hasSize(3);
        assertThat(params.get(0).asThinking().signature()).isEqualTo("sig-abc");
        assertThat(params.get(0).asThinking().thinking()).isEmpty();
        assertThat(params.get(1).asRedactedThinking().data()).isEqualTo("encrypted-xyz");
        assertThat(params.get(2).asText().text()).isEqualTo("Done.");
    }
}
