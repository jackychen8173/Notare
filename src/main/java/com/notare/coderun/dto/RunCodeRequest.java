package com.notare.coderun.dto;

import jakarta.validation.constraints.NotBlank;

public record RunCodeRequest(
        @NotBlank String code,
        // Not used yet - accepted now so this contract doesn't need to change when Scanner/stdin
        // support lands later.
        String stdin
) {
}
