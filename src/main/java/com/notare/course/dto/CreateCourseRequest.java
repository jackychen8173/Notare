package com.notare.course.dto;

import com.notare.course.CourseColor;
import jakarta.validation.constraints.NotBlank;

public record CreateCourseRequest(
        @NotBlank String name,
        @NotBlank String subject,
        String description,
        // Optional: when omitted, the least-used color among the tutor's active courses is picked.
        CourseColor color
) {
}
