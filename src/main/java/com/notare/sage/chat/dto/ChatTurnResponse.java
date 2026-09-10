package com.notare.sage.chat.dto;

import java.util.List;
import java.util.UUID;

public record ChatTurnResponse(
        UUID conversationId,
        List<SageMessageResponse> messages
) {
}
