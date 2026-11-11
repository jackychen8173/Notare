package com.notare.coderun.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** One output check, as the tutor writes it. A null stdin means the test types nothing. */
public record TestRequest(
        @NotBlank @Size(max = 100) String name,
        @Size(max = 65_536) String stdin,
        @NotNull @Size(max = 65_536) String expectedOutput,
        @Min(0) @Max(100) int points,
        boolean hidden
) {
}
