package com.notare.sage.chat;

import com.notare.announcement.AnnouncementService;
import com.notare.announcement.dto.CreateAnnouncementRequest;
import com.notare.assignment.Assignment;
import com.notare.assignment.AssignmentRepository;
import com.notare.assignment.dto.AssignmentResponse;
import com.notare.course.Course;
import com.notare.course.CourseRepository;
import com.notare.course.CourseService;
import com.notare.course.EnrollmentRepository;
import com.notare.course.dto.CourseResponse;
import com.notare.gradecategory.GradeCategoryService;
import com.notare.gradecategory.dto.GradeCategoryResponse;
import com.notare.material.MaterialService;
import com.notare.material.dto.MaterialResponse;
import com.notare.rubric.RubricService;
import com.notare.rubric.dto.RubricResponse;
import com.notare.sage.SageService;
import com.notare.sage.dto.PendingReviewsResponse;
import com.notare.sage.dto.ProgressSummaryResponse;
import com.notare.session.Session;
import com.notare.session.SessionRepository;
import com.notare.session.SessionService;
import com.notare.session.dto.CreateSessionRequest;
import com.notare.session.dto.SessionNoteResponse;
import com.notare.session.dto.SessionResponse;
import com.notare.student.dto.StudentResponse;
import com.notare.submission.Submission;
import com.notare.submission.SubmissionRepository;
import com.notare.submission.SubmissionService;
import com.notare.submission.dto.ReleaseFeedbackRequest;
import com.notare.submission.dto.SubmissionResponse;
import com.notare.topic.TopicService;
import com.notare.topic.dto.TopicResponse;
import com.notare.user.User;
import com.notare.user.UserRepository;
import com.notare.user.UserRole;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Component
public class SageToolExecutor {

    public static final Set<String> WRITE_TOOL_NAMES =
            Set.of("release_feedback", "schedule_session", "complete_session", "post_announcement");

    private final CourseRepository courseRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final AssignmentRepository assignmentRepository;
    private final SubmissionRepository submissionRepository;
    private final SessionRepository sessionRepository;
    private final UserRepository userRepository;
    private final SubmissionService submissionService;
    private final SessionService sessionService;
    private final AnnouncementService announcementService;
    private final SageService sageService;
    private final ObjectMapper objectMapper;
    private final TopicService topicService;
    private final MaterialService materialService;
    private final GradeCategoryService gradeCategoryService;
    private final RubricService rubricService;
    private final CourseService courseService;

    public SageToolExecutor(
            CourseRepository courseRepository,
            EnrollmentRepository enrollmentRepository,
            AssignmentRepository assignmentRepository,
            SubmissionRepository submissionRepository,
            SessionRepository sessionRepository,
            UserRepository userRepository,
            SubmissionService submissionService,
            SessionService sessionService,
            AnnouncementService announcementService,
            SageService sageService,
            ObjectMapper objectMapper,
            TopicService topicService,
            MaterialService materialService,
            GradeCategoryService gradeCategoryService,
            RubricService rubricService,
            CourseService courseService
    ) {
        this.courseRepository = courseRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.assignmentRepository = assignmentRepository;
        this.submissionRepository = submissionRepository;
        this.sessionRepository = sessionRepository;
        this.userRepository = userRepository;
        this.submissionService = submissionService;
        this.sessionService = sessionService;
        this.announcementService = announcementService;
        this.sageService = sageService;
        this.objectMapper = objectMapper;
        this.topicService = topicService;
        this.materialService = materialService;
        this.gradeCategoryService = gradeCategoryService;
        this.rubricService = rubricService;
        this.courseService = courseService;
    }

    public String execute(String toolName, Map<String, Object> input, User tutor) {
        Object result = switch (toolName) {
            case "list_students" -> listStudents(input, tutor);
            case "get_student" -> getStudent(input, tutor);
            case "list_courses" -> listCourses(input, tutor);
            case "get_course" -> getCourse(input, tutor);
            case "list_assignments" -> listAssignments(input, tutor);
            case "get_assignment" -> getAssignment(input, tutor);
            case "list_submissions" -> listSubmissions(input, tutor);
            case "get_submission" -> getSubmission(input, tutor);
            case "list_sessions" -> listSessions(input, tutor);
            case "get_student_progress" -> getStudentProgress(input, tutor);
            case "get_session" -> getSession(input, tutor);
            case "get_session_notes" -> getSessionNotes(input, tutor);
            case "list_materials" -> listMaterials(input, tutor);
            case "list_topics" -> listTopics(input, tutor);
            case "get_rubric" -> getRubric(input, tutor);
            case "list_grade_categories" -> listGradeCategories(input, tutor);
            case "list_enrolled_students" -> listEnrolledStudents(input, tutor);
            case "list_pending_reviews" -> listPendingReviews(input, tutor);
            case "release_feedback" -> releaseFeedback(input, tutor);
            case "schedule_session" -> scheduleSession(input, tutor);
            case "complete_session" -> completeSession(input, tutor);
            case "post_announcement" -> postAnnouncement(input, tutor);
            default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown tool: " + toolName);
        };
        return toJson(result);
    }

    public String describeAction(String toolName, Map<String, Object> input, User tutor) {
        return switch (toolName) {
            case "release_feedback" -> {
                Submission submission = requireOwnedSubmission(uuidParam(input, "submissionId"), tutor);
                String gradeClause = input.get("grade") != null ? " with grade " + input.get("grade") : "";
                String feedbackClause = input.get("tutorFeedback") != null
                        ? " Tutor feedback: \"" + input.get("tutorFeedback") + "\""
                        : "";
                yield "Release feedback for " + submission.getStudent().getName()
                        + "'s submission on \"" + submission.getAssignment().getTitle() + "\"" + gradeClause + "."
                        + feedbackClause;
            }
            case "schedule_session" -> {
                User student = requireVisibleStudent(uuidParam(input, "studentId"), tutor);
                yield "Schedule a session with " + student.getName() + " on " + input.get("date")
                        + " (" + input.get("subject") + ", " + input.get("duration") + " min).";
            }
            case "complete_session" -> {
                Session session = requireOwnedSession(uuidParam(input, "sessionId"), tutor);
                yield "Mark the session with " + session.getStudent().getName()
                        + " on " + session.getDate() + " as complete.";
            }
            case "post_announcement" -> {
                Course course = requireOwnedCourse(uuidParam(input, "courseId"), tutor);
                String content = String.valueOf(input.get("content"));
                yield "Post an announcement to " + course.getName() + ": \"" + content + "\"";
            }
            default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown write tool: " + toolName);
        };
    }

    // ---- read tools ----

    private List<StudentResponse> listStudents(Map<String, Object> input, User tutor) {
        String query = input.get("query") != null ? String.valueOf(input.get("query")).toLowerCase() : null;
        return enrollmentRepository.findDistinctStudentsByCourseTutorId(tutor.getId()).stream()
                .filter(student -> query == null || student.getName().toLowerCase().contains(query))
                .map(StudentResponse::from)
                .toList();
    }

    private StudentResponse getStudent(Map<String, Object> input, User tutor) {
        return StudentResponse.from(requireVisibleStudent(uuidParam(input, "studentId"), tutor));
    }

    private List<CourseResponse> listCourses(Map<String, Object> input, User tutor) {
        boolean archived = Boolean.TRUE.equals(input.get("archived"));
        List<Course> courses = archived
                ? courseRepository.findByTutorIdAndArchivedAtIsNotNull(tutor.getId())
                : courseRepository.findByTutorIdAndArchivedAtIsNull(tutor.getId());
        return courses.stream().map(CourseResponse::from).toList();
    }

    private CourseResponse getCourse(Map<String, Object> input, User tutor) {
        return CourseResponse.from(requireOwnedCourse(uuidParam(input, "courseId"), tutor));
    }

    private List<AssignmentResponse> listAssignments(Map<String, Object> input, User tutor) {
        Course course = requireOwnedCourse(uuidParam(input, "courseId"), tutor);
        return assignmentRepository.findByCourseId(course.getId()).stream()
                .map(AssignmentResponse::from)
                .toList();
    }

    private AssignmentResponse getAssignment(Map<String, Object> input, User tutor) {
        return AssignmentResponse.from(requireOwnedAssignment(uuidParam(input, "assignmentId"), tutor));
    }

    private List<SubmissionResponse> listSubmissions(Map<String, Object> input, User tutor) {
        List<Submission> submissions;
        if (input.get("assignmentId") != null) {
            Assignment assignment = requireOwnedAssignment(uuidParam(input, "assignmentId"), tutor);
            submissions = submissionRepository.findByAssignmentId(assignment.getId());
        } else if (input.get("studentId") != null) {
            User student = requireVisibleStudent(uuidParam(input, "studentId"), tutor);
            submissions = submissionRepository.findByStudentIdAndAssignment_Course_Tutor_Id(student.getId(), tutor.getId());
        } else {
            submissions = submissionRepository.findByAssignment_Course_Tutor_Id(tutor.getId());
        }
        boolean pendingOnly = Boolean.TRUE.equals(input.get("pendingOnly"));
        return submissions.stream()
                .filter(s -> !pendingOnly || s.getReleasedAt() == null)
                .map(s -> SubmissionResponse.from(s, List.of()))
                .toList();
    }

    private SubmissionResponse getSubmission(Map<String, Object> input, User tutor) {
        return submissionService.getSubmission(uuidParam(input, "submissionId"), tutor.getEmail());
    }

    private List<SessionResponse> listSessions(Map<String, Object> input, User tutor) {
        List<Session> sessions = input.get("studentId") != null
                ? sessionRepository.findByTutorIdAndStudentId(tutor.getId(), uuidParam(input, "studentId"))
                : sessionRepository.findByTutorId(tutor.getId());
        boolean upcomingOnly = Boolean.TRUE.equals(input.get("upcomingOnly"));
        LocalDateTime now = LocalDateTime.now();
        return sessions.stream()
                .filter(s -> !upcomingOnly || s.getDate().isAfter(now))
                .map(SessionResponse::from)
                .toList();
    }

    private ProgressSummaryResponse getStudentProgress(Map<String, Object> input, User tutor) {
        return sageService.generateProgressSummary(uuidParam(input, "studentId"), tutor.getEmail());
    }

    private SessionResponse getSession(Map<String, Object> input, User tutor) {
        return sessionService.getSession(uuidParam(input, "sessionId"), tutor.getEmail());
    }

    private SessionNoteResponse getSessionNotes(Map<String, Object> input, User tutor) {
        return sessionService.getSessionNotes(uuidParam(input, "sessionId"), tutor.getEmail());
    }

    private List<MaterialResponse> listMaterials(Map<String, Object> input, User tutor) {
        return materialService.listMaterials(uuidParam(input, "courseId"), tutor.getEmail());
    }

    private List<TopicResponse> listTopics(Map<String, Object> input, User tutor) {
        return topicService.listTopics(uuidParam(input, "courseId"), tutor.getEmail());
    }

    private RubricResponse getRubric(Map<String, Object> input, User tutor) {
        return rubricService.getRubric(uuidParam(input, "assignmentId"), tutor.getEmail());
    }

    private List<GradeCategoryResponse> listGradeCategories(Map<String, Object> input, User tutor) {
        return gradeCategoryService.listCategories(uuidParam(input, "courseId"), tutor.getEmail());
    }

    private List<StudentResponse> listEnrolledStudents(Map<String, Object> input, User tutor) {
        return courseService.listEnrolledStudents(uuidParam(input, "courseId"), tutor.getEmail());
    }

    private PendingReviewsResponse listPendingReviews(Map<String, Object> input, User tutor) {
        return sageService.pendingReviewsCount(tutor.getEmail());
    }

    // ---- write tools ----

    private SubmissionResponse releaseFeedback(Map<String, Object> input, User tutor) {
        UUID submissionId = uuidParam(input, "submissionId");
        String grade = input.get("grade") != null ? String.valueOf(input.get("grade")) : null;
        String tutorFeedback = input.get("tutorFeedback") != null ? String.valueOf(input.get("tutorFeedback")) : null;
        return submissionService.releaseFeedback(submissionId,
                new ReleaseFeedbackRequest(tutorFeedback, grade), tutor.getEmail());
    }

    private SessionResponse scheduleSession(Map<String, Object> input, User tutor) {
        UUID studentId = uuidParam(input, "studentId");
        requireVisibleStudent(studentId, tutor);
        UUID courseId = input.get("courseId") != null ? uuidParam(input, "courseId") : null;
        LocalDateTime date = dateTimeParam(input, "date");
        String subject = String.valueOf(input.get("subject"));
        int duration = intParam(input, "duration");
        return sessionService.createSession(
                new CreateSessionRequest(studentId, courseId, date, subject, duration), tutor.getEmail());
    }

    private SessionResponse completeSession(Map<String, Object> input, User tutor) {
        return sessionService.completeSession(uuidParam(input, "sessionId"), tutor.getEmail());
    }

    private com.notare.announcement.dto.AnnouncementResponse postAnnouncement(Map<String, Object> input, User tutor) {
        UUID courseId = uuidParam(input, "courseId");
        String content = String.valueOf(input.get("content"));
        return announcementService.createAnnouncement(courseId, new CreateAnnouncementRequest(content), tutor.getEmail());
    }

    // ---- shared ownership checks (same 404-not-403 pattern used throughout this codebase) ----

    private User requireVisibleStudent(UUID studentId, User tutor) {
        User student = userRepository.findById(studentId)
                .filter(u -> u.getRole() == UserRole.STUDENT)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Student not found"));
        if (!enrollmentRepository.existsByStudentIdAndCourse_Tutor_Id(student.getId(), tutor.getId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Student not found");
        }
        return student;
    }

    private Course requireOwnedCourse(UUID courseId, User tutor) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Course not found"));
        if (!course.getTutor().getId().equals(tutor.getId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Course not found");
        }
        return course;
    }

    private Assignment requireOwnedAssignment(UUID assignmentId, User tutor) {
        Assignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Assignment not found"));
        if (!assignment.getCourse().getTutor().getId().equals(tutor.getId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Assignment not found");
        }
        return assignment;
    }

    private Submission requireOwnedSubmission(UUID submissionId, User tutor) {
        Submission submission = submissionRepository.findById(submissionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Submission not found"));
        if (!submission.getAssignment().getCourse().getTutor().getId().equals(tutor.getId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Submission not found");
        }
        return submission;
    }

    private Session requireOwnedSession(UUID sessionId, User tutor) {
        Session session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Session not found"));
        if (!session.getTutor().getId().equals(tutor.getId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Session not found");
        }
        return session;
    }

    private UUID uuidParam(Map<String, Object> input, String key) {
        Object value = input.get(key);
        if (value == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Missing required parameter: " + key);
        }
        try {
            return UUID.fromString(String.valueOf(value));
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid UUID for " + key + ": " + value);
        }
    }

    private LocalDateTime dateTimeParam(Map<String, Object> input, String key) {
        Object value = input.get(key);
        if (value == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Missing required parameter: " + key);
        }
        try {
            return LocalDateTime.parse(String.valueOf(value));
        } catch (DateTimeParseException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid date-time for " + key + ": " + value);
        }
    }

    private int intParam(Map<String, Object> input, String key) {
        Object value = input.get(key);
        if (value == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Missing required parameter: " + key);
        }
        if (!(value instanceof Number number)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid number for " + key + ": " + value);
        }
        return number.intValue();
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to serialize tool result", e);
        }
    }
}
