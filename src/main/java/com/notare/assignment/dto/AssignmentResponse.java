package com.notare.assignment.dto;

import com.notare.assignment.Assignment;

import java.time.LocalDate;
import java.util.UUID;

public record AssignmentResponse(
        UUID id,
        UUID courseId,
        String courseName,
        String title,
        String description,
        LocalDate dueDate
) {
    public static AssignmentResponse from(Assignment assignment) {
        return new AssignmentResponse(
                assignment.getId(),
                assignment.getCourse().getId(),
                assignment.getCourse().getName(),
                assignment.getTitle(),
                assignment.getDescription(),
                assignment.getDueDate()
        );
    }
}
