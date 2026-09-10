package com.notare.submission.dto;

import com.notare.submission.FeedbackStatus;
import com.notare.submission.Submission;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record SubmissionResponse(
        UUID id,
        UUID assignmentId,
        UUID studentId,
        String studentName,
        String content,
        String sageFeedback,
        String tutorFeedback,
        FeedbackStatus feedbackStatus,
        String grade,
        LocalDateTime submittedAt,
        LocalDateTime releasedAt,
        List<RubricScoreItem> rubricScores,
        BigDecimal rubricTotalAwarded,
        BigDecimal rubricTotalPossible
) {
    public static SubmissionResponse from(Submission submission, List<RubricScoreItem> rubricScores) {
        return new SubmissionResponse(
                submission.getId(),
                submission.getAssignment().getId(),
                submission.getStudent().getId(),
                submission.getStudent().getName(),
                submission.getContent(),
                submission.getSageFeedback(),
                submission.getTutorFeedback(),
                submission.getFeedbackStatus(),
                submission.getGrade(),
                submission.getSubmittedAt(),
                submission.getReleasedAt(),
                rubricScores,
                totalAwarded(rubricScores),
                totalPossible(rubricScores)
        );
    }

    /**
     * Student-facing view: sageFeedback/tutorFeedback/grade/rubricScores are withheld until
     * releasedAt is set, per the "Sage feedback is NEVER shown to students
     * without tutor approval" constraint - rubric scores follow the same gate.
     */
    public static SubmissionResponse forStudent(Submission submission, List<RubricScoreItem> rubricScores) {
        boolean released = submission.getReleasedAt() != null;
        List<RubricScoreItem> visibleScores = released ? rubricScores : List.of();
        return new SubmissionResponse(
                submission.getId(),
                submission.getAssignment().getId(),
                submission.getStudent().getId(),
                submission.getStudent().getName(),
                submission.getContent(),
                released ? submission.getSageFeedback() : null,
                released ? submission.getTutorFeedback() : null,
                submission.getFeedbackStatus(),
                released ? submission.getGrade() : null,
                submission.getSubmittedAt(),
                submission.getReleasedAt(),
                visibleScores,
                released ? totalAwarded(rubricScores) : null,
                released ? totalPossible(rubricScores) : null
        );
    }

    private static BigDecimal totalAwarded(List<RubricScoreItem> scores) {
        if (scores.isEmpty()) {
            return null;
        }
        return scores.stream().map(RubricScoreItem::pointsAwarded).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private static BigDecimal totalPossible(List<RubricScoreItem> scores) {
        if (scores.isEmpty()) {
            return null;
        }
        return scores.stream().map(RubricScoreItem::pointsPossible).reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
