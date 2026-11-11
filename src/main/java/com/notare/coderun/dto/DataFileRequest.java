package com.notare.coderun.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * A data file's name and text. The name is what the program opens, so it's a plain file name:
 * letters, digits, dot, dash and underscore, starting with a letter or digit (no folders).
 */
public record DataFileRequest(
        @NotBlank
        @Size(max = 100)
        @Pattern(regexp = "[A-Za-z0-9][A-Za-z0-9._-]*",
                message = "Use letters, numbers, dots, dashes or underscores, like scores.txt")
        String name,
        // 200,000 characters: comfortably a few thousand lines of CSV, and small enough to send
        // to the sandbox on every Run.
        @NotNull @Size(max = 200_000) String content
) {
}
