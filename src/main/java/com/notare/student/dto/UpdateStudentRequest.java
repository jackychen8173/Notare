package com.notare.student.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record UpdateStudentRequest(
        @NotBlank String name,
        @NotBlank @Email String email
) {
}
