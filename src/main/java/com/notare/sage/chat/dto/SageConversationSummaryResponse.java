package com.notare.sage.chat.dto;

import com.notare.sage.chat.SageConversation;

import java.time.LocalDateTime;
import java.util.UUID;

public record SageConversationSummaryResponse(
        UUID id,
        String preview,
        LocalDateTime updatedAt
) {
    public static SageConversationSummaryResponse from(SageConversation conversation, String preview) {
        return new SageConversationSummaryResponse(conversation.getId(), preview, conversation.getUpdatedAt());
    }
}
