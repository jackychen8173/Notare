package com.notare.quiz.dto;

import com.notare.quiz.QuestionType;
import com.notare.quiz.QuizQuestion;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record QuestionResponse(
        UUID id,
        QuestionType type,
        String prompt,
        BigDecimal pointsPossible,
        String referenceAnswer,
        int position,
        List<OptionResponse> options
) {
    public static QuestionResponse from(QuizQuestion question, List<OptionResponse> options) {
        return new QuestionResponse(
                question.getId(),
                question.getType(),
                question.getPrompt(),
                question.getPointsPossible(),
                question.getReferenceAnswer(),
                question.getPosition(),
                options
        );
    }

    /**
     * Student-facing view (the take page): referenceAnswer is the tutor's grading guide / answer key
     * for free-text questions, so it is always withheld, and options come in already stripped of
     * correctness via OptionResponse.forStudent.
     */
    public static QuestionResponse forStudent(QuizQuestion question, List<OptionResponse> studentOptions) {
        return new QuestionResponse(
                question.getId(),
                question.getType(),
                question.getPrompt(),
                question.getPointsPossible(),
                null,
                question.getPosition(),
                studentOptions
        );
    }
}
