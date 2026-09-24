package com.notare.discussion.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateThreadRequest(
        @NotBlank @Size(max = 255) String title,
        @NotBlank String body
) {
}
