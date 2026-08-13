package com.notare.course.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record EnrollStudentRequest(
        @NotNull UUID studentId
) {
}
