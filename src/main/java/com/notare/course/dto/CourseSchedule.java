package com.notare.course.dto;

import com.notare.course.Course;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.EnumSet;
import java.util.List;

/**
 * A course's weekly meeting schedule, used in both requests and responses. {@code days} empty means
 * "no schedule". The term dates are optional and bound the recurrence on the calendar.
 */
public record CourseSchedule(
        List<DayOfWeek> days,
        LocalTime startTime,
        LocalTime endTime,
        LocalDate termStart,
        LocalDate termEnd
) {
    /** Null when the course has no meeting days and no term dates. */
    public static CourseSchedule from(Course course) {
        boolean hasDays = course.getMeetingDays() != null && !course.getMeetingDays().isEmpty();
        if (!hasDays && course.getTermStartDate() == null && course.getTermEndDate() == null) {
            return null;
        }
        return new CourseSchedule(
                hasDays ? List.copyOf(EnumSet.copyOf(course.getMeetingDays())) : List.of(),
                course.getMeetingStartTime(),
                course.getMeetingEndTime(),
                course.getTermStartDate(),
                course.getTermEndDate()
        );
    }
}
