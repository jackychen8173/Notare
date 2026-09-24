package com.notare.quizattempt.dto;

import com.notare.quiz.QuestionType;
import com.notare.quiz.QuizQuestion;
import com.notare.quiz.QuizQuestionOption;
import com.notare.quiz.dto.OptionResponse;
import com.notare.quizattempt.QuizAnswer;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;

/**
 * One question's worth of a QuizAttempt, denormalizing the question's prompt/type/points/options so
 * the tutor grading page (and the student results page) can render picked-vs-correct without a
 * second fetch. answer is null when the student never answered this question.
 */
public record QuizAnswerResponse(
        UUID questionId,
        String questionPrompt,
        QuestionType questionType,
        BigDecimal pointsPossible,
        List<OptionResponse> options,
        UUID selectedOptionId,
        String textResponse,
        Boolean correct,
        BigDecimal pointsAwarded,
        String tutorFeedback,
        String sageSuggestion
) {
    public static QuizAnswerResponse from(QuizQuestion question, QuizAnswer answer, List<QuizQuestionOption> rawOptions) {
        return build(question, answer, rawOptions, OptionResponse::from, true);
    }

    /**
     * Student-facing view: correct/pointsAwarded/tutorFeedback/sageSuggestion, and whether each
     * option is correct, are all withheld until the owning attempt is released - same
     * all-or-nothing reveal as SubmissionResponse.forStudent. The student's own selectedOptionId/
     * textResponse are always visible; it's their own answer, not a grading result.
     */
    public static QuizAnswerResponse forStudent(
            QuizQuestion question, QuizAnswer answer, List<QuizQuestionOption> rawOptions, boolean released
    ) {
        return build(question, answer, rawOptions, released ? OptionResponse::from : OptionResponse::forStudent, released);
    }

    private static QuizAnswerResponse build(
            QuizQuestion question,
            QuizAnswer answer,
            List<QuizQuestionOption> rawOptions,
            Function<QuizQuestionOption, OptionResponse> optionMapper,
            boolean revealGrading
    ) {
        List<OptionResponse> options = rawOptions.stream().map(optionMapper).toList();
        return new QuizAnswerResponse(
                question.getId(),
                question.getPrompt(),
                question.getType(),
                question.getPointsPossible(),
                options,
                answer != null && answer.getSelectedOption() != null ? answer.getSelectedOption().getId() : null,
                answer != null ? answer.getTextResponse() : null,
                revealGrading && answer != null ? answer.getCorrect() : null,
                revealGrading && answer != null ? answer.getPointsAwarded() : null,
                revealGrading && answer != null ? answer.getTutorFeedback() : null,
                revealGrading && answer != null ? answer.getSageSuggestion() : null
        );
    }
}
