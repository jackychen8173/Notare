package com.notare.quiz.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

import java.time.LocalDateTime;
import java.util.UUID;

public record UpdateQuizRequest(
        @NotBlank String title,
        String description,
        @Min(1) Integer timeLimitMinutes,
        UUID topicId,
        UUID gradeCategoryId,
        // Wall-clock time in the course's time zone; null means no due date.
        LocalDateTime dueAt
) {
}
