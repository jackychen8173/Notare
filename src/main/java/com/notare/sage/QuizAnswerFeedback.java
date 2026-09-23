package com.notare.sage;

import java.math.BigDecimal;

public record QuizAnswerFeedback(
        BigDecimal suggestedScore,
        String feedback
) {
}
