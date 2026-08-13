package com.notare.submission.dto;

import jakarta.validation.constraints.NotBlank;

public record SubmitAssignmentRequest(
        @NotBlank String content
) {
}
