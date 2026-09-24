package com.notare.quiz.dto;

import com.notare.quiz.QuestionType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;

public record UpdateQuestionRequest(
        @NotNull QuestionType type,
        @NotBlank String prompt,
        @NotNull @DecimalMin("0") BigDecimal pointsPossible,
        String referenceAnswer,
        @Valid List<CreateQuestionRequest.OptionInput> options,
        Boolean correctBoolean
) {
}
