package com.notare.rubric.dto;

import com.notare.rubric.RubricCriterion;

import java.math.BigDecimal;
import java.util.UUID;

public record RubricCriterionResponse(
        UUID id,
        String name,
        String description,
        BigDecimal pointsPossible,
        int position
) {
    public static RubricCriterionResponse from(RubricCriterion criterion) {
        return new RubricCriterionResponse(
                criterion.getId(),
                criterion.getName(),
                criterion.getDescription(),
                criterion.getPointsPossible(),
                criterion.getPosition()
        );
    }
}
