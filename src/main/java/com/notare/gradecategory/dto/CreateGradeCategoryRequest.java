package com.notare.gradecategory.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record CreateGradeCategoryRequest(
        @NotBlank String name,
        @NotNull @DecimalMin("0") @DecimalMax("100") BigDecimal weightPercent
) {
}
