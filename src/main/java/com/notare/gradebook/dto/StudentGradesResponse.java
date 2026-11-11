package com.notare.gradebook.dto;

import com.notare.gradebook.dto.GradebookResponse.Cell;
import com.notare.gradebook.dto.GradebookResponse.Column;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * A student's own grades in one course: the same columns and cells as the tutor's gradebook, for
 * work they can see, without class averages.
 */
public record StudentGradesResponse(
        String timeZone,
        List<CategoryGrade> categories,
        List<Item> items,
        BigDecimal coursePercent
) {

    public record CategoryGrade(UUID id, String name, BigDecimal weightPercent, BigDecimal percent) {
    }

    public record Item(Column column, Cell cell) {
    }
}
