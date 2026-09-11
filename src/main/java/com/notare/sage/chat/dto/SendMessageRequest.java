package com.notare.sage.chat.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.UUID;

public record SendMessageRequest(
        UUID conversationId,
        @NotBlank String message
) {
}
