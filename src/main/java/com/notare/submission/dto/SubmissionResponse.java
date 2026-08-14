package com.notare.submission.dto;

import com.notare.submission.FeedbackStatus;
import com.notare.submission.Submission;

import java.time.LocalDateTime;
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
        LocalDateTime releasedAt
) {
    public static SubmissionResponse from(Submission submission) {
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
                submission.getReleasedAt()
        );
    }

    /**
     * Student-facing view: sageFeedback/tutorFeedback/grade are withheld until
     * releasedAt is set, per the "Sage feedback is NEVER shown to students
     * without tutor approval" constraint.
     */
    public static SubmissionResponse forStudent(Submission submission) {
        boolean released = submission.getReleasedAt() != null;
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
                submission.getReleasedAt()
        );
    }
}
