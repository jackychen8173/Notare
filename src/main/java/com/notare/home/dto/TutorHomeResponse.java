package com.notare.home.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/** Everything waiting on the tutor across all active courses: the "To review" home page. */
public record TutorHomeResponse(
        List<ReviewSubmission> submissions,
        List<ReviewQuizAttempt> quizAttempts,
        List<HomeThread> unreadDiscussions
) {
    /** An assignment submission not yet released to the student. */
    public record ReviewSubmission(
            UUID submissionId,
            UUID assignmentId,
            String assignmentTitle,
            HomeCourse course,
            String studentName,
            int attemptNumber,
            LocalDateTime submittedAt,
            boolean sageDraftReady
    ) {
    }

    /** A finished quiz attempt not yet released; needsGrading when a free-text answer has no score yet. */
    public record ReviewQuizAttempt(
            UUID attemptId,
            UUID quizId,
            String quizTitle,
            HomeCourse course,
            String studentName,
            LocalDateTime submittedAt,
            boolean needsGrading
    ) {
    }
}
