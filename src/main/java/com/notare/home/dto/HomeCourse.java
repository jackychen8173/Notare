package com.notare.home.dto;

import com.notare.course.Course;
import com.notare.course.CourseColor;

import java.util.UUID;

/** Just enough of a course to label and color a home-page item. */
public record HomeCourse(UUID id, String name, CourseColor color) {
    public static HomeCourse from(Course course) {
        return new HomeCourse(course.getId(), course.getName(), course.getColor());
    }
}
