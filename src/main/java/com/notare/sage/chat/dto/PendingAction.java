package com.notare.sage.chat.dto;

import java.util.Map;

public record PendingAction(
        String toolName,
        String toolUseId,
        Map<String, Object> input,
        String description
) {
}
