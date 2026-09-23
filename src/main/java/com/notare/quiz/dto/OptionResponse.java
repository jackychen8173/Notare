package com.notare.quiz.dto;

import com.notare.quiz.QuizQuestionOption;

import java.util.UUID;

public record OptionResponse(
        UUID id,
        String text,
        Boolean correct,
        int position
) {
    public static OptionResponse from(QuizQuestionOption option) {
        return new OptionResponse(option.getId(), option.getText(), option.isCorrect(), option.getPosition());
    }

    /**
     * Student-facing view: correct is ALWAYS null, unconditionally - not release-gated. Leaking the
     * answer key while a student is actively taking the quiz is a different failure than leaking
     * results after submission (see QuizAttemptResponse.forStudent for that gate).
     */
    public static OptionResponse forStudent(QuizQuestionOption option) {
        return new OptionResponse(option.getId(), option.getText(), null, option.getPosition());
    }
}
