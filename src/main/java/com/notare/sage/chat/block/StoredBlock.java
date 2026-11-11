package com.notare.sage.chat.block;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.util.Map;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = StoredBlock.Text.class, name = "text"),
        @JsonSubTypes.Type(value = StoredBlock.ToolUse.class, name = "tool_use"),
        @JsonSubTypes.Type(value = StoredBlock.ToolResult.class, name = "tool_result"),
        @JsonSubTypes.Type(value = StoredBlock.Thinking.class, name = "thinking"),
        @JsonSubTypes.Type(value = StoredBlock.RedactedThinking.class, name = "redacted_thinking")
})
public sealed interface StoredBlock permits StoredBlock.Text, StoredBlock.ToolUse, StoredBlock.ToolResult,
        StoredBlock.Thinking, StoredBlock.RedactedThinking {

    record Text(String text) implements StoredBlock {
    }

    record ToolUse(String id, String name, Map<String, Object> input) implements StoredBlock {
    }

    record ToolResult(String toolUseId, String content, boolean isError) implements StoredBlock {
    }

    /**
     * The model's thinking, kept so it can be sent back unchanged on the next request (the model
     * needs it to carry its reasoning through a tool loop). The text is usually empty: the API
     * omits it by default and the signature carries it. Never shown to anyone, never edited.
     */
    record Thinking(String thinking, String signature) implements StoredBlock {
    }

    /** Thinking the API returned encrypted. Same rule: stored as is, sent back as is. */
    record RedactedThinking(String data) implements StoredBlock {
    }
}
