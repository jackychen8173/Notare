package com.notare.assignment.dto;

import com.notare.assignment.Assignment;

import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.UUID;

/**
 * Times are sent with the course time zone's offset ("2026-11-20T23:59:00-08:00"), so the same
 * value is an exact moment for display and a wall-clock time in the course's zone for editing.
 * dueDate is the due day in the course's time zone, for views that only show a date.
 */
public record AssignmentResponse(
        UUID id,
        UUID courseId,
        String courseName,
        String title,
        String description,
        LocalDate dueDate,
        OffsetDateTime dueAt,
        OffsetDateTime opensAt,
        OffsetDateTime closesAt,
        String timeZone,
        Integer pointsPossible,
        int latePenaltyPercent,
        boolean acceptingSubmissions,
        UUID topicId,
        String topicName,
        UUID gradeCategoryId,
        String gradeCategoryName,
        boolean allowResubmission
) {
    public static AssignmentResponse from(Assignment assignment) {
        ZoneId zone = assignment.getCourse().zone();
        OffsetDateTime dueAt = inZone(assignment.getDueAt(), zone);
        return new AssignmentResponse(
                assignment.getId(),
                assignment.getCourse().getId(),
                assignment.getCourse().getName(),
                assignment.getTitle(),
                assignment.getDescription(),
                dueAt.toLocalDate(),
                dueAt,
                inZone(assignment.getOpensAt(), zone),
                inZone(assignment.getClosesAt(), zone),
                assignment.getCourse().getTimeZone(),
                assignment.getPointsPossible(),
                assignment.getLatePenaltyPercent(),
                assignment.isAcceptingSubmissions(Instant.now()),
                assignment.getTopic() != null ? assignment.getTopic().getId() : null,
                assignment.getTopic() != null ? assignment.getTopic().getName() : null,
                assignment.getGradeCategory() != null ? assignment.getGradeCategory().getId() : null,
                assignment.getGradeCategory() != null ? assignment.getGradeCategory().getName() : null,
                assignment.isAllowResubmission()
        );
    }

    private static OffsetDateTime inZone(Instant instant, ZoneId zone) {
        return instant == null ? null : instant.atZone(zone).toOffsetDateTime();
    }
}
