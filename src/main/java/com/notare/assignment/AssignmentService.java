package com.notare.assignment;

import com.notare.assignment.dto.AssignmentResponse;
import com.notare.assignment.dto.CreateAssignmentRequest;
import com.notare.assignment.dto.UpdateAssignmentRequest;
import com.notare.assignment.dto.UpdateAssignmentSettingsRequest;
import com.notare.course.Course;
import com.notare.course.CourseRepository;
import com.notare.course.EnrollmentRepository;
import com.notare.gradecategory.GradeCategory;
import com.notare.gradecategory.GradeCategoryRepository;
import com.notare.topic.Topic;
import com.notare.topic.TopicRepository;
import com.notare.user.User;
import com.notare.user.UserRepository;
import com.notare.user.UserRole;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class AssignmentService {

    /** The due time when only a date is given: the end of that day. */
    public static final LocalTime DEFAULT_DUE_TIME = LocalTime.of(23, 59);

    private static final int MAX_POINTS = 10_000;

    private final AssignmentRepository assignmentRepository;
    private final CourseRepository courseRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final UserRepository userRepository;
    private final TopicRepository topicRepository;
    private final GradeCategoryRepository gradeCategoryRepository;

    public AssignmentService(
            AssignmentRepository assignmentRepository,
            CourseRepository courseRepository,
            EnrollmentRepository enrollmentRepository,
            UserRepository userRepository,
            TopicRepository topicRepository,
            GradeCategoryRepository gradeCategoryRepository
    ) {
        this.assignmentRepository = assignmentRepository;
        this.courseRepository = courseRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.userRepository = userRepository;
        this.topicRepository = topicRepository;
        this.gradeCategoryRepository = gradeCategoryRepository;
    }

    public AssignmentResponse createAssignment(UUID courseId, CreateAssignmentRequest request, String tutorEmail) {
        Course course = requireOwnedCourse(courseId, tutorEmail);

        if (course.getArchivedAt() != null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cannot add assignments to an archived course");
        }

        Topic topic = null;
        if (request.topicId() != null) {
            topic = topicRepository.findById(request.topicId())
                    .filter(candidate -> candidate.getCourse().getId().equals(course.getId()))
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Topic not found"));
        }

        GradeCategory gradeCategory = resolveGradeCategory(course, request.gradeCategoryId());

        LocalDateTime dueAt = request.dueAt() != null
                ? request.dueAt()
                : request.dueDate() != null ? request.dueDate().atTime(DEFAULT_DUE_TIME) : null;
        if (dueAt == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A due date is required");
        }

        Assignment assignment = Assignment.builder()
                .course(course)
                .topic(topic)
                .gradeCategory(gradeCategory)
                .title(request.title())
                .description(request.description())
                .allowResubmission(Boolean.TRUE.equals(request.allowResubmission()))
                .build();
        applyTiming(assignment, course.zone(), dueAt, request.opensAt(), request.closesAt(),
                request.pointsPossible(), request.latePenaltyPercent());

        assignmentRepository.save(assignment);

        return AssignmentResponse.from(assignment);
    }

    public AssignmentResponse updateAssignment(UUID assignmentId, UpdateAssignmentRequest request, String tutorEmail) {
        Assignment assignment = requireOwnedAssignment(assignmentId, tutorEmail);
        Course course = assignment.getCourse();
        if (course.getArchivedAt() != null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cannot edit assignments in an archived course");
        }

        assignment.setTitle(request.title());
        assignment.setDescription(request.description());
        assignment.setGradeCategory(resolveGradeCategory(course, request.gradeCategoryId()));
        applyTiming(assignment, course.zone(), request.dueAt(), request.opensAt(), request.closesAt(),
                request.pointsPossible(), request.latePenaltyPercent());
        assignmentRepository.save(assignment);

        return AssignmentResponse.from(assignment);
    }

    public AssignmentResponse updateSettings(UUID assignmentId, UpdateAssignmentSettingsRequest request, String tutorEmail) {
        Assignment assignment = requireOwnedAssignment(assignmentId, tutorEmail);

        Topic topic = null;
        if (request.topicId() != null) {
            topic = topicRepository.findById(request.topicId())
                    .filter(candidate -> candidate.getCourse().getId().equals(assignment.getCourse().getId()))
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Topic not found"));
        }

        assignment.setTopic(topic);
        assignment.setAllowResubmission(request.allowResubmission());
        assignmentRepository.save(assignment);

        return AssignmentResponse.from(assignment);
    }

    @Transactional(readOnly = true)
    public List<AssignmentResponse> listAssignments(UUID courseId, String tutorEmail) {
        Course course = requireOwnedCourse(courseId, tutorEmail);
        return assignmentRepository.findByCourseId(course.getId()).stream()
                .map(AssignmentResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public AssignmentResponse getAssignment(UUID assignmentId, String tutorEmail) {
        return AssignmentResponse.from(requireOwnedAssignment(assignmentId, tutorEmail));
    }

    @Transactional(readOnly = true)
    public List<AssignmentResponse> listAssignmentsForEnrolledCourse(UUID courseId, String studentEmail) {
        Course course = requireEnrolledCourseForAssignments(courseId, studentEmail);
        Instant now = Instant.now();
        return assignmentRepository.findByCourseId(course.getId()).stream()
                .filter(assignment -> assignment.isVisibleToStudents(now))
                .map(AssignmentResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public AssignmentResponse getAssignmentForStudent(UUID assignmentId, String studentEmail) {
        return AssignmentResponse.from(requireEnrolledAssignment(assignmentId, studentEmail));
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

    private Course requireEnrolledCourseForAssignments(UUID courseId, String studentEmail) {
        User student = requireStudentUser(studentEmail);
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Course not found"));

        if (!enrollmentRepository.existsByStudentIdAndCourseId(student.getId(), course.getId())) {
            // 404, not 403 - avoid confirming the course exists if the student isn't enrolled
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Course not found");
        }

        return course;
    }

    private Assignment requireEnrolledAssignment(UUID assignmentId, String studentEmail) {
        User student = requireStudentUser(studentEmail);
        Assignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Assignment not found"));

        if (!enrollmentRepository.existsByStudentIdAndCourseId(student.getId(), assignment.getCourse().getId())
                || !assignment.isVisibleToStudents(Instant.now())) {
            // 404, not 403 - avoid confirming the assignment exists if the student isn't enrolled,
            // or before it opens
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Assignment not found");
        }

        return assignment;
    }

    private GradeCategory resolveGradeCategory(Course course, UUID gradeCategoryId) {
        if (gradeCategoryId == null) {
            return null;
        }
        return gradeCategoryRepository.findById(gradeCategoryId)
                .filter(candidate -> candidate.getCourse().getId().equals(course.getId()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Grade category not found"));
    }

    /**
     * Sets the due, open and close times (wall-clock times in the course's zone) and the points.
     * Opening has to come before the due time, and closing can't come before it. A time that
     * doesn't exist in the zone (the skipped hour when clocks go forward) moves forward by the gap.
     */
    private static void applyTiming(Assignment assignment, ZoneId zone, LocalDateTime dueAt, LocalDateTime opensAt,
                                    LocalDateTime closesAt, Integer pointsPossible, Integer latePenaltyPercent) {
        Instant due = dueAt.atZone(zone).toInstant();
        Instant opens = opensAt != null ? opensAt.atZone(zone).toInstant() : null;
        Instant closes = closesAt != null ? closesAt.atZone(zone).toInstant() : null;
        if (opens != null && !opens.isBefore(due)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "The assignment has to open before it's due");
        }
        if (closes != null && closes.isBefore(due)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "The assignment can't close before it's due");
        }
        if (pointsPossible != null && (pointsPossible < 1 || pointsPossible > MAX_POINTS)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Points must be between 1 and " + MAX_POINTS);
        }
        int penalty = latePenaltyPercent != null ? latePenaltyPercent : 0;
        if (penalty < 0 || penalty > 100) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "The late penalty must be between 0 and 100 percent");
        }
        assignment.setDueAt(due);
        assignment.setOpensAt(opens);
        assignment.setClosesAt(closes);
        assignment.setPointsPossible(pointsPossible);
        assignment.setLatePenaltyPercent(penalty);
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

    private Assignment requireOwnedAssignment(UUID assignmentId, String tutorEmail) {
        User tutor = requireTutor(tutorEmail);
        Assignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Assignment not found"));

        if (!assignment.getCourse().getTutor().getId().equals(tutor.getId())) {
            // 404, not 403 - avoid confirming another tutor's assignment exists
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Assignment not found");
        }

        return assignment;
    }
}
