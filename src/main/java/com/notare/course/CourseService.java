package com.notare.course;

import com.notare.course.dto.CourseResponse;
import com.notare.course.dto.CourseSchedule;
import com.notare.course.dto.CreateCourseRequest;
import com.notare.course.dto.DuplicateCourseRequest;
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
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Comparator;
import java.util.EnumSet;
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
    private final CourseContentCopier courseContentCopier;

    public CourseService(
            CourseRepository courseRepository,
            EnrollmentRepository enrollmentRepository,
            UserRepository userRepository,
            CourseContentCopier courseContentCopier
    ) {
        this.courseRepository = courseRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.userRepository = userRepository;
        this.courseContentCopier = courseContentCopier;
    }

    public CourseResponse createCourse(CreateCourseRequest request, String tutorEmail) {
        User tutor = requireTutor(tutorEmail);

        Course course = Course.builder()
                .tutor(tutor)
                .name(request.name())
                .subject(request.subject())
                .description(request.description())
                .joinCode(generateUniqueJoinCode())
                .color(request.color() != null ? request.color() : nextColor(tutor, null))
                .build();
        if (request.schedule() != null) {
            applySchedule(course, request.schedule());
        }

        courseRepository.save(course);

        return CourseResponse.from(course);
    }

    /**
     * Creates a new course for the same tutor with a copy of the source's content and a fresh join
     * code, for running parallel sections or reusing a course in a later year. Archived sources are
     * allowed (reusing last year's archived course is a main use); the copy always starts active.
     */
    public CourseResponse duplicateCourse(UUID courseId, DuplicateCourseRequest request, String tutorEmail) {
        Course source = requireOwnedCourse(courseId, tutorEmail);

        Course copy = courseRepository.save(Course.builder()
                .tutor(source.getTutor())
                .name(request.name())
                .subject(source.getSubject())
                .description(source.getDescription())
                .joinCode(generateUniqueJoinCode())
                // The meeting schedule isn't copied either: parallel sections meet at different times,
                // and a reused course is for a different term.
                // Never the source's color, so parallel sections are easy to tell apart.
                .color(nextColor(source.getTutor(), source.getColor()))
                .build());

        courseContentCopier.copy(source, copy);

        return CourseResponse.from(copy);
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
        if (request.color() != null) {
            course.setColor(request.color());
        }
        if (request.schedule() != null) {
            applySchedule(course, request.schedule());
        }
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

    /**
     * Replaces the course's meeting schedule. Meeting days need both times, with the end after the
     * start; times without days are rejected rather than silently dropped. Empty days clear the
     * weekly meeting; the term dates are kept or cleared independently.
     */
    private void applySchedule(Course course, CourseSchedule schedule) {
        boolean hasDays = schedule.days() != null && !schedule.days().isEmpty();
        if (hasDays) {
            if (schedule.startTime() == null || schedule.endTime() == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Meeting days need a start and end time");
            }
            if (!schedule.endTime().isAfter(schedule.startTime())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "End time must be after start time");
            }
        } else if (schedule.startTime() != null || schedule.endTime() != null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Pick at least one meeting day");
        }
        if (schedule.termStart() != null && schedule.termEnd() != null
                && schedule.termEnd().isBefore(schedule.termStart())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Term end must be on or after term start");
        }

        course.setMeetingDays(hasDays ? EnumSet.copyOf(schedule.days()) : EnumSet.noneOf(DayOfWeek.class));
        course.setMeetingStartTime(hasDays ? schedule.startTime() : null);
        course.setMeetingEndTime(hasDays ? schedule.endTime() : null);
        course.setTermStartDate(schedule.termStart());
        course.setTermEndDate(schedule.termEnd());
    }

    /**
     * The palette color used least among the tutor's active courses, earliest in the palette on ties.
     * {@code avoid} (nullable) is never picked while any other color exists.
     */
    private CourseColor nextColor(User tutor, CourseColor avoid) {
        List<Course> active = courseRepository.findByTutorIdAndArchivedAtIsNull(tutor.getId());
        return Arrays.stream(CourseColor.values())
                .filter(color -> color != avoid)
                .min(Comparator.comparingLong(color -> active.stream().filter(c -> c.getColor() == color).count()))
                .orElse(CourseColor.TEAL);
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
