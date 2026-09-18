package com.notare.admin;

import com.notare.assignment.AssignmentRepository;
import com.notare.assignment.dto.AssignmentResponse;
import com.notare.course.Course;
import com.notare.course.CourseRepository;
import com.notare.course.EnrollmentRepository;
import com.notare.course.dto.CourseResponse;
import com.notare.student.dto.StudentResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class AdminCourseService {

    private final CourseRepository courseRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final AssignmentRepository assignmentRepository;

    public AdminCourseService(
            CourseRepository courseRepository,
            EnrollmentRepository enrollmentRepository,
            AssignmentRepository assignmentRepository
    ) {
        this.courseRepository = courseRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.assignmentRepository = assignmentRepository;
    }

    public List<CourseResponse> listCourses() {
        return courseRepository.findAll().stream()
                .map(CourseResponse::from)
                .toList();
    }

    public CourseResponse getCourse(UUID id) {
        return CourseResponse.from(requireCourse(id));
    }

    public List<StudentResponse> listStudents(UUID id) {
        Course course = requireCourse(id);
        return enrollmentRepository.findByCourseId(course.getId()).stream()
                .map(enrollment -> StudentResponse.from(enrollment.getStudent()))
                .toList();
    }

    public List<AssignmentResponse> listAssignments(UUID id) {
        Course course = requireCourse(id);
        return assignmentRepository.findByCourseId(course.getId()).stream()
                .map(AssignmentResponse::from)
                .toList();
    }

    private Course requireCourse(UUID id) {
        return courseRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Course not found"));
    }
}
