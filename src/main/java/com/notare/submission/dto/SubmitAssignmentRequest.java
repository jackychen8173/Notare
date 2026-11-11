package com.notare.submission.dto;


public record SubmitAssignmentRequest(
        // May be blank when the student attached files (POST /api/student/assignments/{id}/files).
        String content
) {
}
