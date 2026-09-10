package com.notare.sage.chat.block;

import com.anthropic.core.JsonValue;
import com.anthropic.models.messages.ContentBlock;
import com.anthropic.models.messages.ContentBlockParam;
import com.anthropic.models.messages.TextBlockParam;
import com.anthropic.models.messages.ToolResultBlockParam;
import com.anthropic.models.messages.ToolUseBlock;
import com.anthropic.models.messages.ToolUseBlockParam;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class BlockConverter {

    private BlockConverter() {
    }

    public static String serialize(List<StoredBlock> blocks, ObjectMapper objectMapper) {
        try {
            return objectMapper.writeValueAsString(blocks);
        } catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Failed to serialize Sage chat message content", e);
        }
    }

    public static List<StoredBlock> deserialize(String json, ObjectMapper objectMapper) {
        try {
            return objectMapper.readValue(json, new TypeReference<List<StoredBlock>>() {
            });
        } catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Failed to deserialize Sage chat message content", e);
        }
    }

    public static List<ContentBlockParam> toApiParams(List<StoredBlock> blocks) {
        List<ContentBlockParam> params = new ArrayList<>();
        for (StoredBlock block : blocks) {
            switch (block) {
                case StoredBlock.Text text -> params.add(
                        ContentBlockParam.ofText(TextBlockParam.builder().text(text.text()).build()));
                case StoredBlock.ToolUse toolUse -> {
                    ToolUseBlockParam.Input.Builder inputBuilder = ToolUseBlockParam.Input.builder();
                    toolUse.input().forEach((key, value) ->
                            inputBuilder.putAdditionalProperty(key, JsonValue.from(value)));
                    params.add(ContentBlockParam.ofToolUse(ToolUseBlockParam.builder()
                            .id(toolUse.id())
                            .name(toolUse.name())
                            .input(inputBuilder.build())
                            .build()));
                }
                case StoredBlock.ToolResult toolResult -> params.add(
                        ContentBlockParam.ofToolResult(ToolResultBlockParam.builder()
                                .toolUseId(toolResult.toolUseId())
                                .content(toolResult.content())
                                .isError(toolResult.isError())
                                .build()));
            }
        }
        return params;
    }

    public static List<StoredBlock> fromApiResponse(List<ContentBlock> blocks) {
        List<StoredBlock> stored = new ArrayList<>();
        for (ContentBlock block : blocks) {
            if (block.isText()) {
                stored.add(new StoredBlock.Text(block.asText().text()));
            } else if (block.isToolUse()) {
                ToolUseBlock toolUse = block.asToolUse();
                Map<String, Object> input = toolUse._input().convert(new TypeReference<Map<String, Object>>() {
                });
                stored.add(new StoredBlock.ToolUse(toolUse.id(), toolUse.name(), input));
            }
            // Other block types (thinking, server tool use, etc.) are never produced by this
            // tool set given the system prompt and are intentionally dropped rather than stored.
        }
        return stored;
    }
}
