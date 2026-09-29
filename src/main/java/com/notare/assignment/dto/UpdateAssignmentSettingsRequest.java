package com.notare.assignment.dto;

import java.util.UUID;

/**
 * Replaces an assignment's settings wholesale: topicId null means "no topic" (ungrouped), not
 * "keep", so the client always sends the current values of both fields.
 */
public record UpdateAssignmentSettingsRequest(
        boolean allowResubmission,
        UUID topicId
) {
}
