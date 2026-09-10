package com.notare.rubric.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;

public record CreateRubricRequest(
        @NotBlank String title,
        @NotEmpty @Valid List<CriterionInput> criteria
) {
    public record CriterionInput(
            @NotBlank String name,
            String description,
            @NotNull @DecimalMin("0") BigDecimal pointsPossible
    ) {
    }
}
