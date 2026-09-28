package com.notare.home.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/** What a student could do next across their active courses: the "Next up" home page. */
public record StudentHomeResponse(
        List<ToDoAssignment> assignments,
        List<OpenQuiz> quizzes,
        List<ReturnedWork> returned,
        List<HomeThread> unreadDiscussions
) {
    /** Not yet submitted, due recently or soon (overdue ones included). */
    public record ToDoAssignment(UUID assignmentId, String title, HomeCourse course, LocalDate dueDate) {
    }

    /** A published quiz the student hasn't finished; attemptId is set when one is in progress. */
    public record OpenQuiz(
            UUID quizId,
            String title,
            HomeCourse course,
            Integer timeLimitMinutes,
            UUID attemptId,
            LocalDateTime deadlineAt
    ) {
    }

    /** Recently released feedback: an assignment (grade may be null) or a quiz attempt. */
    public record ReturnedWork(
            Kind kind,
            UUID id,
            String title,
            HomeCourse course,
            String grade,
            LocalDateTime releasedAt
    ) {
        public enum Kind { ASSIGNMENT, QUIZ }
    }
}
