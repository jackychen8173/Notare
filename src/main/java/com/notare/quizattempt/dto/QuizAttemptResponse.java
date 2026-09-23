package com.notare.quizattempt.dto;

import com.notare.quizattempt.AttemptStatus;
import com.notare.quizattempt.QuizAttempt;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public record QuizAttemptResponse(
        UUID id,
        UUID quizId,
        String quizTitle,
        UUID studentId,
        String studentName,
        AttemptStatus status,
        LocalDateTime startedAt,
        LocalDateTime deadlineAt,
        LocalDateTime submittedAt,
        boolean autoSubmitted,
        LocalDateTime releasedAt,
        List<QuizAnswerResponse> answers,
        BigDecimal totalPointsAwarded,
        BigDecimal totalPointsPossible
) {
    public static QuizAttemptResponse from(QuizAttempt attempt, List<QuizAnswerResponse> answers) {
        return new QuizAttemptResponse(
                attempt.getId(),
                attempt.getQuiz().getId(),
                attempt.getQuiz().getTitle(),
                attempt.getStudent().getId(),
                attempt.getStudent().getName(),
                attempt.getStatus(),
                attempt.getStartedAt(),
                attempt.getDeadlineAt(),
                attempt.getSubmittedAt(),
                attempt.isAutoSubmitted(),
                attempt.getReleasedAt(),
                answers,
                totalAwarded(answers),
                totalPossible(answers)
        );
    }

    /**
     * Student-facing view: `answers` must already be built via QuizAnswerResponse.forStudent (per-
     * answer grading gated on release). The totals here are gated a second time on top of that -
     * before release they're null outright rather than a misleading partial/zero sum.
     */
    public static QuizAttemptResponse forStudent(QuizAttempt attempt, List<QuizAnswerResponse> studentAnswers) {
        boolean released = attempt.getReleasedAt() != null;
        return new QuizAttemptResponse(
                attempt.getId(),
                attempt.getQuiz().getId(),
                attempt.getQuiz().getTitle(),
                attempt.getStudent().getId(),
                attempt.getStudent().getName(),
                attempt.getStatus(),
                attempt.getStartedAt(),
                attempt.getDeadlineAt(),
                attempt.getSubmittedAt(),
                attempt.isAutoSubmitted(),
                attempt.getReleasedAt(),
                studentAnswers,
                released ? totalAwarded(studentAnswers) : null,
                released ? totalPossible(studentAnswers) : null
        );
    }

    private static BigDecimal totalAwarded(List<QuizAnswerResponse> answers) {
        if (answers.isEmpty()) {
            return null;
        }
        return answers.stream()
                .map(QuizAnswerResponse::pointsAwarded)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private static BigDecimal totalPossible(List<QuizAnswerResponse> answers) {
        if (answers.isEmpty()) {
            return null;
        }
        return answers.stream().map(QuizAnswerResponse::pointsPossible).reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
