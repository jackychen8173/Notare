package com.notare.course;

import com.notare.course.dto.CourseResponse;
import com.notare.course.dto.CreateCourseRequest;
import com.notare.course.dto.UpdateCourseRequest;
import com.notare.student.dto.StudentResponse;
import com.notare.user.User;
import com.notare.user.UserRepository;
import com.notare.user.UserRole;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class CourseService {

    // Excludes 0/O and 1/I so codes read back unambiguously when shared out loud or handwritten.
    private static final String JOIN_CODE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final int JOIN_CODE_LENGTH = 6;
    private static final int MAX_JOIN_CODE_ATTEMPTS = 10;
    private static final SecureRandom RANDOM = new SecureRandom();

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
                .joinCode(generateUniqueJoinCode())
                .build();

        courseRepository.save(course);

        return CourseResponse.from(course);
    }

    public CourseResponse regenerateJoinCode(UUID courseId, String tutorEmail) {
        Course course = requireOwnedCourse(courseId, tutorEmail);
        course.setJoinCode(generateUniqueJoinCode());
        courseRepository.save(course);
        return CourseResponse.from(course);
    }

    public CourseResponse updateCourse(UUID courseId, UpdateCourseRequest request, String tutorEmail) {
        Course course = requireOwnedCourse(courseId, tutorEmail);
        requireNotArchived(course, "Cannot edit an archived course");

        course.setName(request.name());
        course.setSubject(request.subject());
        course.setDescription(request.description());
        courseRepository.save(course);

        return CourseResponse.from(course);
    }

    public CourseResponse archiveCourse(UUID courseId, String tutorEmail) {
        Course course = requireOwnedCourse(courseId, tutorEmail);
        if (course.getArchivedAt() == null) {
            course.setArchivedAt(LocalDateTime.now());
            courseRepository.save(course);
        }
        return CourseResponse.from(course);
    }

    public CourseResponse unarchiveCourse(UUID courseId, String tutorEmail) {
        Course course = requireOwnedCourse(courseId, tutorEmail);
        course.setArchivedAt(null);
        courseRepository.save(course);
        return CourseResponse.from(course);
    }

    public void joinCourseByCode(String code, String studentEmail) {
        User student = requireStudentUser(studentEmail);
        Course course = courseRepository.findByJoinCode(code.trim().toUpperCase())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Invalid join code"));

        requireNotArchived(course, "This course is no longer accepting new students");

        if (enrollmentRepository.existsByStudentIdAndCourseId(student.getId(), course.getId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Already enrolled in this course");
        }

        Enrollment enrollment = Enrollment.builder()
                .id(new EnrollmentId(student.getId(), course.getId()))
                .student(student)
                .course(course)
                .build();

        enrollmentRepository.save(enrollment);
    }

    public void removeStudent(UUID courseId, UUID studentId, String tutorEmail) {
        Course course = requireOwnedCourse(courseId, tutorEmail);

        if (!enrollmentRepository.existsByStudentIdAndCourseId(studentId, course.getId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Student not found in this course");
        }

        enrollmentRepository.deleteByStudentIdAndCourseId(studentId, course.getId());
    }

    private String generateUniqueJoinCode() {
        for (int attempt = 0; attempt < MAX_JOIN_CODE_ATTEMPTS; attempt++) {
            StringBuilder code = new StringBuilder(JOIN_CODE_LENGTH);
            for (int i = 0; i < JOIN_CODE_LENGTH; i++) {
                code.append(JOIN_CODE_ALPHABET.charAt(RANDOM.nextInt(JOIN_CODE_ALPHABET.length())));
            }
            if (!courseRepository.existsByJoinCode(code.toString())) {
                return code.toString();
            }
        }
        throw new IllegalStateException("Could not generate a unique join code");
    }

    @Transactional(readOnly = true)
    public List<CourseResponse> listCourses(String tutorEmail, boolean archived) {
        User tutor = requireTutor(tutorEmail);
        List<Course> courses = archived
                ? courseRepository.findByTutorIdAndArchivedAtIsNotNull(tutor.getId())
                : courseRepository.findByTutorIdAndArchivedAtIsNull(tutor.getId());
        return courses.stream()
                .map(CourseResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public CourseResponse getCourse(UUID courseId, String tutorEmail) {
        return CourseResponse.from(requireOwnedCourse(courseId, tutorEmail));
    }

    @Transactional(readOnly = true)
    public List<CourseResponse> listEnrolledCourses(String studentEmail) {
        User student = requireStudentUser(studentEmail);
        return enrollmentRepository.findByStudentId(student.getId()).stream()
                .map(Enrollment::getCourse)
                .map(CourseResponse::forStudent)
                .toList();
    }

    @Transactional(readOnly = true)
    public CourseResponse getEnrolledCourse(UUID courseId, String studentEmail) {
        return CourseResponse.forStudent(requireEnrolledCourse(courseId, studentEmail));
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

    private void requireNotArchived(Course course, String message) {
        if (course.getArchivedAt() != null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, message);
        }
    }
}
