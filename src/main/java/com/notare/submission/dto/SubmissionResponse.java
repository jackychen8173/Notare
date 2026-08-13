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
}
