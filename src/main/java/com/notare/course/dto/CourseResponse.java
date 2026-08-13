package com.notare.course.dto;

import com.notare.course.Course;

import java.util.UUID;

public record CourseResponse(
        UUID id,
        UUID tutorId,
        String tutorName,
        String name,
        String subject,
        String description
) {
    public static CourseResponse from(Course course) {
        return new CourseResponse(
                course.getId(),
                course.getTutor().getId(),
                course.getTutor().getName(),
                course.getName(),
                course.getSubject(),
                course.getDescription()
        );
    }
}
