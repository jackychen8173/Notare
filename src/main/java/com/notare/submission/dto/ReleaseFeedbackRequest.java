package com.notare.submission.dto;

import jakarta.validation.constraints.DecimalMin;

import java.math.BigDecimal;

/**
 * pointsEarned is before any late penalty and may be above the assignment's points possible
 * (extra credit). grade is the older free-text grade; both may be given.
 */
public record ReleaseFeedbackRequest(
        String tutorFeedback,
        String grade,
        @DecimalMin("0") BigDecimal pointsEarned
) {
}
