package com.notare.progress.dto;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** One student's progress through a course, unit by unit (units are the course's topics). */
public record StudentProgressResponse(List<Unit> units) {

    /** topicId is null for the "No unit" bucket of work the tutor hasn't placed in a unit. */
    public record Unit(UUID topicId, String name, List<Item> items, int done, int total) {
    }

    public record Item(UUID id, ItemKind kind, String title, LocalDate dueDate, ItemStatus status) {
    }

    public enum ItemKind { ASSIGNMENT, QUIZ }

    /** NOT_STARTED and IN_PROGRESS count as not done; SUBMITTED and RETURNED count as done. */
    public enum ItemStatus { NOT_STARTED, IN_PROGRESS, SUBMITTED, RETURNED }
}
