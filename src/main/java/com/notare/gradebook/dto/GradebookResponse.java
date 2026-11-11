package com.notare.gradebook.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * The tutor's gradebook for one course: a column per assignment and published quiz, a row per
 * enrolled student, cells aligned with columns. Only released work has points, so a score the
 * tutor is still drafting never shows up here or in a course grade.
 */
public record GradebookResponse(
        String timeZone,
        List<Category> categories,
        List<Column> columns,
        List<StudentRow> students
) {

    public enum ItemKind { ASSIGNMENT, QUIZ }

    public enum CellStatus {
        /** Nothing handed in, and it isn't due yet (quizzes have no due time, so they stay here). */
        NOT_SUBMITTED,
        /** Nothing handed in, and the due time has passed. */
        MISSING,
        /** A quiz attempt that's still running. */
        IN_PROGRESS,
        /** Handed in, and the latest version's feedback isn't released yet. */
        SUBMITTED,
        /** The latest version's feedback is released. */
        RETURNED
    }

    public record Category(UUID id, String name, BigDecimal weightPercent) {
    }

    /**
     * pointsPossible is null for an assignment without points (or a quiz without questions); its
     * cells show a status and any old text grade, and it never counts toward a grade.
     * countsTowardGrade is false for those, and for work outside every category when the course
     * has categories. averagePercent is the class average of released scores (tutor view only).
     */
    public record Column(
            UUID id,
            ItemKind kind,
            String title,
            OffsetDateTime dueAt,
            OffsetDateTime opensAt,
            BigDecimal pointsPossible,
            UUID categoryId,
            boolean countsTowardGrade,
            BigDecimal averagePercent
    ) {
    }

    /**
     * points is after any late penalty; pointsBeforePenalty is what the tutor gave. Both come from
     * the latest released version, which can be older than the latest one handed in (a resubmission
     * waiting for review shows status SUBMITTED with the earlier score). grade is the old free-text
     * grade, kept for work graded before points existed. workId is the latest submission or the quiz
     * attempt, for linking to it.
     */
    public record Cell(
            CellStatus status,
            BigDecimal points,
            BigDecimal pointsBeforePenalty,
            String grade,
            boolean late,
            int daysLate,
            int latePenaltyPercent,
            UUID workId
    ) {
    }

    /** categoryPercents is aligned with categories; null where nothing in it is graded yet. */
    public record StudentRow(
            UUID studentId,
            String name,
            String email,
            List<Cell> cells,
            List<BigDecimal> categoryPercents,
            BigDecimal coursePercent
    ) {
    }
}
