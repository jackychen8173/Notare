package com.notare.session.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.LocalDateTime;
import java.util.UUID;

public record CreateSessionRequest(
        @NotNull UUID studentId,
        UUID courseId,
        @NotNull LocalDateTime date,
        @NotBlank String subject,
        @NotNull @Positive Integer duration
) {
}
