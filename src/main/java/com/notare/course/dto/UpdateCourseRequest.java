package com.notare.course.dto;

import jakarta.validation.constraints.NotBlank;

public record UpdateCourseRequest(
        @NotBlank String name,
        @NotBlank String subject,
        String description
) {
}
