package com.notare.sage.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record DraftNotesRequest(
        @NotNull UUID sessionId
) {
}
