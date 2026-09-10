package com.notare.course.dto;

import com.notare.course.Course;

import java.util.UUID;

public record CourseResponse(
        UUID id,
        UUID tutorId,
        String tutorName,
        String name,
        String subject,
        String description,
        String joinCode
) {
    public static CourseResponse from(Course course) {
        return new CourseResponse(
                course.getId(),
                course.getTutor().getId(),
                course.getTutor().getName(),
                course.getName(),
                course.getSubject(),
                course.getDescription(),
                course.getJoinCode()
        );
    }

    /**
     * Students don't need the join code once enrolled, so it's never included here.
     */
    public static CourseResponse forStudent(Course course) {
        return new CourseResponse(
                course.getId(),
                course.getTutor().getId(),
                course.getTutor().getName(),
                course.getName(),
                course.getSubject(),
                course.getDescription(),
                null
        );
    }
}
