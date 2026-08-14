package com.notare.course;

import com.notare.course.dto.CourseResponse;
import com.notare.course.dto.CreateCourseRequest;
import com.notare.course.dto.EnrollStudentRequest;
import com.notare.student.dto.StudentResponse;
import com.notare.user.User;
import com.notare.user.UserRepository;
import com.notare.user.UserRole;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class CourseService {

    private final CourseRepository courseRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final UserRepository userRepository;

    public CourseService(
            CourseRepository courseRepository,
            EnrollmentRepository enrollmentRepository,
            UserRepository userRepository
    ) {
        this.courseRepository = courseRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.userRepository = userRepository;
    }

    public CourseResponse createCourse(CreateCourseRequest request, String tutorEmail) {
        User tutor = requireTutor(tutorEmail);

        Course course = Course.builder()
                .tutor(tutor)
                .name(request.name())
                .subject(request.subject())
                .description(request.description())
                .build();

        courseRepository.save(course);

        return CourseResponse.from(course);
    }

    @Transactional(readOnly = true)
    public List<CourseResponse> listCourses(String tutorEmail) {
        User tutor = requireTutor(tutorEmail);
        return courseRepository.findByTutorId(tutor.getId()).stream()
                .map(CourseResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public CourseResponse getCourse(UUID courseId, String tutorEmail) {
        return CourseResponse.from(requireOwnedCourse(courseId, tutorEmail));
    }

    public void enrollStudent(UUID courseId, EnrollStudentRequest request, String tutorEmail) {
        Course course = requireOwnedCourse(courseId, tutorEmail);

        User student = userRepository.findById(request.studentId())
                .filter(user -> user.getRole() == UserRole.STUDENT)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Student not found"));

        if (enrollmentRepository.existsByStudentIdAndCourseId(student.getId(), course.getId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Student is already enrolled in this course");
        }

        Enrollment enrollment = Enrollment.builder()
                .id(new EnrollmentId(student.getId(), course.getId()))
                .student(student)
                .course(course)
                .build();

        enrollmentRepository.save(enrollment);
    }

    @Transactional(readOnly = true)
    public List<CourseResponse> listEnrolledCourses(String studentEmail) {
        User student = requireStudentUser(studentEmail);
        return enrollmentRepository.findByStudentId(student.getId()).stream()
                .map(Enrollment::getCourse)
                .map(CourseResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public CourseResponse getEnrolledCourse(UUID courseId, String studentEmail) {
        return CourseResponse.from(requireEnrolledCourse(courseId, studentEmail));
    }

    @Transactional(readOnly = true)
    public List<StudentResponse> listEnrolledStudents(UUID courseId, String tutorEmail) {
        Course course = requireOwnedCourse(courseId, tutorEmail);
        return enrollmentRepository.findByCourseId(course.getId()).stream()
                .map(enrollment -> StudentResponse.from(enrollment.getStudent()))
                .toList();
    }

    private User requireTutor(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));
    }

    private User requireStudentUser(String email) {
        return userRepository.findByEmail(email)
                .filter(user -> user.getRole() == UserRole.STUDENT)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));
    }

    private Course requireEnrolledCourse(UUID courseId, String studentEmail) {
        User student = requireStudentUser(studentEmail);
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Course not found"));

        if (!enrollmentRepository.existsByStudentIdAndCourseId(student.getId(), course.getId())) {
            // 404, not 403 - avoid confirming the course exists if the student isn't enrolled
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Course not found");
        }

        return course;
    }

    private Course requireOwnedCourse(UUID courseId, String tutorEmail) {
        User tutor = requireTutor(tutorEmail);
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Course not found"));

        if (!course.getTutor().getId().equals(tutor.getId())) {
            // 404, not 403 - avoid confirming another tutor's course exists
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Course not found");
        }

        return course;
    }
}
