package com.notare.student.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateStudentRequest(
        @NotBlank String name,
        @NotBlank @Email String email,
        @NotBlank @Size(min = 8) String password
) {
}
