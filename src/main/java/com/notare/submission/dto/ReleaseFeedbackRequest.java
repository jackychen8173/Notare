package com.notare.submission.dto;

import jakarta.validation.constraints.NotBlank;

public record ReleaseFeedbackRequest(
        @NotBlank String tutorFeedback,
        String grade
) {
}
