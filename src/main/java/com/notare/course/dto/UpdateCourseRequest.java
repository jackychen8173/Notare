package com.notare.course.dto;

import com.notare.course.CourseColor;
import jakarta.validation.constraints.NotBlank;

public record UpdateCourseRequest(
        @NotBlank String name,
        @NotBlank String subject,
        String description,
        // Optional: null keeps the current color.
        CourseColor color
) {
}
