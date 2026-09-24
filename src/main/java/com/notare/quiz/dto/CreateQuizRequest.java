package com.notare.quiz.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

import java.util.UUID;

public record CreateQuizRequest(
        @NotBlank String title,
        String description,
        @Min(1) Integer timeLimitMinutes,
        UUID topicId,
        UUID gradeCategoryId
) {
}
