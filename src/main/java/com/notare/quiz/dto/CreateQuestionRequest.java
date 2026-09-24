package com.notare.quiz.dto;

import com.notare.quiz.QuestionType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;

public record CreateQuestionRequest(
        @NotNull QuestionType type,
        @NotBlank String prompt,
        @NotNull @DecimalMin("0") BigDecimal pointsPossible,
        String referenceAnswer,
        // MULTIPLE_CHOICE only: the answer options, exactly one flagged correct. Ignored for every
        // other question type.
        @Valid List<OptionInput> options,
        // TRUE_FALSE only: whether "True" is the correct answer. The service synthesizes the two
        // option rows from this rather than trusting a client-submitted options list, so a quiz
        // author can't accidentally create a true/false question with the wrong option text.
        Boolean correctBoolean
) {
    public record OptionInput(
            @NotBlank String text,
            boolean correct
    ) {
    }
}
