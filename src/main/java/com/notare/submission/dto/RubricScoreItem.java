package com.notare.submission.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record RubricScoreItem(
        UUID criterionId,
        String criterionName,
        BigDecimal pointsAwarded,
        BigDecimal pointsPossible
) {
}
