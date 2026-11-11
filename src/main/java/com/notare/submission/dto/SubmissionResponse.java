package com.notare.submission.dto;

import com.notare.assignment.Lateness;
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
        int attemptNumber,
        String sageFeedback,
        String tutorFeedback,
        FeedbackStatus feedbackStatus,
        String grade,
        LocalDateTime submittedAt,
        LocalDateTime releasedAt,
        List<RubricScoreItem> rubricScores,
        BigDecimal rubricTotalAwarded,
        BigDecimal rubricTotalPossible,
        // Points: earned is before the late penalty, afterPenalty is what counts.
        BigDecimal pointsEarned,
        Integer pointsPossible,
        BigDecimal pointsAfterPenalty,
        // Lateness is about when the student handed it in, so the student always sees it.
        boolean late,
        int daysLate,
        int latePenaltyPercent
) {
    public static SubmissionResponse from(Submission submission, List<RubricScoreItem> rubricScores) {
        Lateness lateness = Lateness.of(submission.getAssignment(), submission.submittedInstant());
        return new SubmissionResponse(
                submission.getId(),
                submission.getAssignment().getId(),
                submission.getStudent().getId(),
                submission.getStudent().getName(),
                submission.getContent(),
                submission.getAttemptNumber(),
                submission.getSageFeedback(),
                submission.getTutorFeedback(),
                submission.getFeedbackStatus(),
                submission.getGrade(),
                submission.getSubmittedAt(),
                submission.getReleasedAt(),
                rubricScores,
                totalAwarded(rubricScores),
                totalPossible(rubricScores),
                submission.getPointsEarned(),
                submission.getAssignment().getPointsPossible(),
                lateness.apply(submission.getPointsEarned()),
                lateness.late(),
                lateness.daysLate(),
                lateness.penaltyPercent()
        );
    }

    /**
     * Student-facing view: sageFeedback/tutorFeedback/grade/rubricScores/points are withheld until
     * releasedAt is set, per the "Sage feedback is NEVER shown to students
     * without tutor approval" constraint - rubric scores follow the same gate.
     */
    public static SubmissionResponse forStudent(Submission submission, List<RubricScoreItem> rubricScores) {
        boolean released = submission.getReleasedAt() != null;
        List<RubricScoreItem> visibleScores = released ? rubricScores : List.of();
        Lateness lateness = Lateness.of(submission.getAssignment(), submission.submittedInstant());
        return new SubmissionResponse(
                submission.getId(),
                submission.getAssignment().getId(),
                submission.getStudent().getId(),
                submission.getStudent().getName(),
                submission.getContent(),
                submission.getAttemptNumber(),
                released ? submission.getSageFeedback() : null,
                released ? submission.getTutorFeedback() : null,
                submission.getFeedbackStatus(),
                released ? submission.getGrade() : null,
                submission.getSubmittedAt(),
                submission.getReleasedAt(),
                visibleScores,
                released ? totalAwarded(rubricScores) : null,
                released ? totalPossible(rubricScores) : null,
                released ? submission.getPointsEarned() : null,
                submission.getAssignment().getPointsPossible(),
                released ? lateness.apply(submission.getPointsEarned()) : null,
                lateness.late(),
                lateness.daysLate(),
                lateness.penaltyPercent()
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
