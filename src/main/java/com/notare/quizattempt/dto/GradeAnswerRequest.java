package com.notare.quizattempt.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record GradeAnswerRequest(
        @NotNull @DecimalMin("0") BigDecimal pointsAwarded,
        String tutorFeedback
) {
}
