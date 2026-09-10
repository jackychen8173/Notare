package com.notare.course.dto;

import jakarta.validation.constraints.NotBlank;

public record JoinCourseRequest(
        @NotBlank String code
) {
}
