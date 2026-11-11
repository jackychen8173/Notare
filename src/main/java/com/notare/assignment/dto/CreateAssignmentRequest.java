package com.notare.assignment.dto;

import jakarta.validation.constraints.NotBlank;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Times are wall-clock times in the course's time zone ("2026-11-20T23:59"). Give dueAt, or
 * dueDate alone for 11:59 PM that day. closesAt equal to dueAt locks the assignment at the due
 * time; null accepts late work any time.
 */
public record CreateAssignmentRequest(
        @NotBlank String title,
        String description,
        LocalDate dueDate,
        UUID topicId,
        UUID gradeCategoryId,
        Boolean allowResubmission,
        LocalDateTime dueAt,
        LocalDateTime opensAt,
        LocalDateTime closesAt,
        Integer pointsPossible,
        Integer latePenaltyPercent
) {
}
