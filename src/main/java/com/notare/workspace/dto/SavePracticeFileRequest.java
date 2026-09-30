package com.notare.workspace.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record SavePracticeFileRequest(
        @NotBlank @Size(max = 100) String name,
        @NotNull @Size(max = 100_000) String content
) {
}
