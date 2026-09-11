package com.notare.sage.chat.dto;

import com.notare.sage.chat.MessageRole;
import com.notare.sage.chat.SageMessage;
import com.notare.sage.chat.block.BlockConverter;
import com.notare.sage.chat.block.StoredBlock;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.stream.Collectors;

public record SageMessageResponse(
        UUID id,
        MessageRole role,
        String text,
        PendingActionResponse pendingAction,
        LocalDateTime createdAt
) {
    public static SageMessageResponse from(SageMessage message, ObjectMapper objectMapper) {
        String text = BlockConverter.deserialize(message.getContent(), objectMapper).stream()
                .filter(block -> block instanceof StoredBlock.Text)
                .map(block -> ((StoredBlock.Text) block).text())
                .collect(Collectors.joining("\n"));

        PendingActionResponse pendingAction = null;
        if (message.getPendingAction() != null) {
            try {
                PendingAction action = objectMapper.readValue(message.getPendingAction(), PendingAction.class);
                pendingAction = PendingActionResponse.from(action, message.getActionStatus().name());
            } catch (Exception e) {
                throw new IllegalStateException("Corrupt pending action JSON for message " + message.getId(), e);
            }
        }

        return new SageMessageResponse(message.getId(), message.getRole(), text, pendingAction, message.getCreatedAt());
    }
}
