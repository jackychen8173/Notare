package com.notare.sage.chat.dto;

public record PendingActionResponse(
        String toolName,
        String description,
        String actionStatus
) {
    public static PendingActionResponse from(PendingAction action, String actionStatus) {
        return new PendingActionResponse(action.toolName(), action.description(), actionStatus);
    }
}
