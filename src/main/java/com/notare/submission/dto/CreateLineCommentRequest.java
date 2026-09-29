package com.notare.submission.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record CreateLineCommentRequest(
        @Min(1) int lineNumber,
        @NotBlank String body
) {
}
