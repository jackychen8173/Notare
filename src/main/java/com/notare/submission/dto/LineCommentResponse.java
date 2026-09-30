package com.notare.submission.dto;

import com.notare.submission.LineCommentSource;
import com.notare.submission.LineCommentStatus;
import com.notare.submission.SubmissionLineComment;

import java.time.LocalDateTime;
import java.util.UUID;

public record LineCommentResponse(
        UUID id,
        UUID submissionId,
        int lineNumber,
        String body,
        LineCommentSource source,
        LineCommentStatus status,
        LocalDateTime createdAt
) {
    public static LineCommentResponse from(SubmissionLineComment comment) {
        return new LineCommentResponse(
                comment.getId(),
                comment.getSubmission().getId(),
                comment.getLineNumber(),
                comment.getBody(),
                comment.getSource(),
                comment.getStatus(),
                comment.getCreatedAt()
        );
    }
}
