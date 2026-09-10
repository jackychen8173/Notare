package com.notare.assignment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.UUID;

public record CreateAssignmentRequest(
        @NotBlank String title,
        String description,
        @NotNull LocalDate dueDate,
        UUID topicId,
        UUID gradeCategoryId
) {
}
