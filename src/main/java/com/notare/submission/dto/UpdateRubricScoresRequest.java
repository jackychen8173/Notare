package com.notare.submission.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record UpdateRubricScoresRequest(
        @NotEmpty @Valid List<ScoreInput> scores
) {
    public record ScoreInput(
            @NotNull UUID criterionId,
            @NotNull @DecimalMin("0") BigDecimal pointsAwarded
    ) {
    }
}
