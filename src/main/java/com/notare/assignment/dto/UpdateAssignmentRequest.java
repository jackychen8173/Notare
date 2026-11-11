package com.notare.assignment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Replaces an assignment's details wholesale (null means "none", not "keep"). Times are
 * wall-clock times in the course's time zone. Unit and resubmission stay on the settings endpoint.
 */
public record UpdateAssignmentRequest(
        @NotBlank String title,
        String description,
        @NotNull LocalDateTime dueAt,
        LocalDateTime opensAt,
        LocalDateTime closesAt,
        Integer pointsPossible,
        Integer latePenaltyPercent,
        UUID gradeCategoryId
) {
}
