package com.notare.gradecategory.dto;

import com.notare.gradecategory.GradeCategory;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record GradeCategoryResponse(
        UUID id,
        UUID courseId,
        String name,
        BigDecimal weightPercent,
        LocalDateTime createdAt
) {
    public static GradeCategoryResponse from(GradeCategory category) {
        return new GradeCategoryResponse(
                category.getId(),
                category.getCourse().getId(),
                category.getName(),
                category.getWeightPercent(),
                category.getCreatedAt()
        );
    }
}
