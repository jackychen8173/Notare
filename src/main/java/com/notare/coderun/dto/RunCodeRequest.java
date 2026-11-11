package com.notare.coderun.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RunCodeRequest(
        @NotBlank String code,
        // What the program reads from the keyboard (System.in / Scanner). Optional: a program
        // that reads with nothing typed gets an empty input and Scanner says so.
        @Size(max = 65_536) String stdin
) {
}
