package com.notare.progress.dto;

import java.util.List;
import java.util.UUID;

/**
 * The whole class's progress for the tutor: one column per unit, one row per enrolled student.
 * StudentRow.done lines up index-for-index with units.
 */
public record CourseProgressResponse(List<UnitSummary> units, List<StudentRow> students) {

    public record UnitSummary(UUID topicId, String name, int assignmentCount, int quizCount) {
        public int total() {
            return assignmentCount + quizCount;
        }
    }

    public record StudentRow(UUID studentId, String name, List<Integer> done) {
    }
}
