package com.notare.quiz.dto;

import com.notare.quiz.Quiz;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record QuizResponse(
        UUID id,
        UUID courseId,
        String courseName,
        UUID topicId,
        String topicName,
        UUID gradeCategoryId,
        String gradeCategoryName,
        String title,
        String description,
        Integer timeLimitMinutes,
        // With the course zone's offset, like AssignmentResponse.dueAt; null when there's none.
        OffsetDateTime dueAt,
        String timeZone,
        LocalDateTime publishedAt,
        BigDecimal totalPointsPossible,
        List<QuestionResponse> questions
) {
    public static QuizResponse from(Quiz quiz, List<QuestionResponse> questions) {
        return new QuizResponse(
                quiz.getId(),
                quiz.getCourse().getId(),
                quiz.getCourse().getName(),
                quiz.getTopic() != null ? quiz.getTopic().getId() : null,
                quiz.getTopic() != null ? quiz.getTopic().getName() : null,
                quiz.getGradeCategory() != null ? quiz.getGradeCategory().getId() : null,
                quiz.getGradeCategory() != null ? quiz.getGradeCategory().getName() : null,
                quiz.getTitle(),
                quiz.getDescription(),
                quiz.getTimeLimitMinutes(),
                quiz.getDueAt() != null ? quiz.getDueAt().atZone(quiz.getCourse().zone()).toOffsetDateTime() : null,
                quiz.getCourse().getTimeZone(),
                quiz.getPublishedAt(),
                totalPointsPossible(questions),
                questions
        );
    }

    private static BigDecimal totalPointsPossible(List<QuestionResponse> questions) {
        return questions.stream()
                .map(QuestionResponse::pointsPossible)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
