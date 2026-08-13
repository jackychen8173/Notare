package com.notare.session.dto;

import jakarta.validation.constraints.NotBlank;

public record SaveSessionNotesRequest(
        @NotBlank String rawNotes
) {
}
