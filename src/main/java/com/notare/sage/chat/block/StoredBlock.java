package com.notare.sage.chat.block;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.util.Map;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = StoredBlock.Text.class, name = "text"),
        @JsonSubTypes.Type(value = StoredBlock.ToolUse.class, name = "tool_use"),
        @JsonSubTypes.Type(value = StoredBlock.ToolResult.class, name = "tool_result")
})
public sealed interface StoredBlock permits StoredBlock.Text, StoredBlock.ToolUse, StoredBlock.ToolResult {

    record Text(String text) implements StoredBlock {
    }

    record ToolUse(String id, String name, Map<String, Object> input) implements StoredBlock {
    }

    record ToolResult(String toolUseId, String content, boolean isError) implements StoredBlock {
    }
}
