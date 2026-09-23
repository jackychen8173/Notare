package com.notare.sage.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record DraftQuizAnswerFeedbackRequest(
        @NotNull UUID attemptId,
        @NotNull UUID questionId
) {
}
