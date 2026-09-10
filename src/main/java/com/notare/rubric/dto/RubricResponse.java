package com.notare.rubric.dto;

import com.notare.rubric.Rubric;

import java.util.List;
import java.util.UUID;

public record RubricResponse(
        UUID id,
        UUID assignmentId,
        String title,
        List<RubricCriterionResponse> criteria
) {
    public static RubricResponse from(Rubric rubric, List<RubricCriterionResponse> criteria) {
        return new RubricResponse(
                rubric.getId(),
                rubric.getAssignment().getId(),
                rubric.getTitle(),
                criteria
        );
    }
}
