package com.notare.sage.chat;

import com.notare.announcement.AnnouncementService;
import com.notare.announcement.dto.CreateAnnouncementRequest;
import com.notare.assignment.Assignment;
import com.notare.assignment.AssignmentRepository;
import com.notare.assignment.AssignmentService;
import com.notare.assignment.dto.AssignmentResponse;
import com.notare.assignment.dto.CreateAssignmentRequest;
import com.notare.course.Course;
import com.notare.course.CourseRepository;
import com.notare.course.CourseService;
import com.notare.course.EnrollmentRepository;
import com.notare.course.dto.CourseResponse;
import com.notare.course.dto.UpdateCourseRequest;
import com.notare.gradecategory.GradeCategory;
import com.notare.gradecategory.GradeCategoryRepository;
import com.notare.gradecategory.GradeCategoryService;
import com.notare.gradecategory.dto.CreateGradeCategoryRequest;
import com.notare.gradecategory.dto.GradeCategoryResponse;
import com.notare.gradecategory.dto.UpdateGradeCategoryRequest;
import com.notare.material.MaterialService;
import com.notare.material.dto.CreateMaterialRequest;
import com.notare.material.dto.MaterialResponse;
import com.notare.rubric.RubricCriterion;
import com.notare.rubric.RubricCriterionRepository;
import com.notare.rubric.RubricService;
import com.notare.rubric.dto.RubricResponse;
import com.notare.sage.SageService;
import com.notare.sage.dto.PendingReviewsResponse;
import com.notare.sage.dto.ProgressSummaryResponse;
import com.notare.session.Session;
import com.notare.session.SessionRepository;
import com.notare.session.SessionService;
import com.notare.session.dto.CreateSessionRequest;
import com.notare.session.dto.SaveSessionNotesRequest;
import com.notare.session.dto.SessionNoteResponse;
import com.notare.session.dto.SessionResponse;
import com.notare.student.dto.StudentResponse;
import com.notare.submission.Submission;
import com.notare.submission.SubmissionRepository;
import com.notare.submission.SubmissionService;
import com.notare.submission.dto.ReleaseFeedbackRequest;
import com.notare.submission.dto.SubmissionResponse;
import com.notare.submission.dto.UpdateRubricScoresRequest;
import com.notare.topic.Topic;
import com.notare.topic.TopicRepository;
import com.notare.topic.TopicService;
import com.notare.topic.dto.CreateTopicRequest;
import com.notare.topic.dto.RenameTopicRequest;
import com.notare.topic.dto.TopicResponse;
import com.notare.user.User;
import com.notare.user.UserRepository;
import com.notare.user.UserRole;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Component
public class SageToolExecutor {

    public static final Set<String> WRITE_TOOL_NAMES = Set.of(
            "release_feedback", "schedule_session", "complete_session", "post_announcement",
            "update_course", "create_assignment", "create_material", "create_topic", "rename_topic",
            "create_grade_category", "update_grade_category", "save_session_notes",
            "update_rubric_scores", "draft_session_notes", "review_submission"
    );

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
    private final AssignmentService assignmentService;
    private final TopicRepository topicRepository;
    private final GradeCategoryRepository gradeCategoryRepository;
    private final RubricCriterionRepository rubricCriterionRepository;

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
            CourseService courseService,
            AssignmentService assignmentService,
            TopicRepository topicRepository,
            GradeCategoryRepository gradeCategoryRepository,
            RubricCriterionRepository rubricCriterionRepository
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
        this.assignmentService = assignmentService;
        this.topicRepository = topicRepository;
        this.gradeCategoryRepository = gradeCategoryRepository;
        this.rubricCriterionRepository = rubricCriterionRepository;
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
            case "update_course" -> updateCourse(input, tutor);
            case "create_assignment" -> createAssignment(input, tutor);
            case "create_material" -> createMaterial(input, tutor);
            case "create_topic" -> createTopic(input, tutor);
            case "rename_topic" -> renameTopic(input, tutor);
            case "create_grade_category" -> createGradeCategory(input, tutor);
            case "update_grade_category" -> updateGradeCategory(input, tutor);
            case "save_session_notes" -> saveSessionNotes(input, tutor);
            case "update_rubric_scores" -> updateRubricScores(input, tutor);
            case "draft_session_notes" -> draftSessionNotes(input, tutor);
            case "review_submission" -> reviewSubmission(input, tutor);
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
            case "update_course" -> {
                Course course = requireOwnedCourse(uuidParam(input, "courseId"), tutor);
                String descriptionClause = input.get("description") != null
                        ? " Description: \"" + input.get("description") + "\""
                        : "";
                yield "Update course \"" + course.getName() + "\" to name \"" + input.get("name")
                        + "\", subject \"" + input.get("subject") + "\"." + descriptionClause;
            }
            case "create_assignment" -> {
                Course course = requireOwnedCourse(uuidParam(input, "courseId"), tutor);
                String descClause = input.get("description") != null
                        ? " Description: \"" + input.get("description") + "\"" : "";
                yield "Create assignment \"" + input.get("title") + "\" in " + course.getName()
                        + ", due " + input.get("dueDate") + "." + descClause;
            }
            case "create_material" -> {
                Course course = requireOwnedCourse(uuidParam(input, "courseId"), tutor);
                String urlClause = input.get("url") != null ? " (" + input.get("url") + ")" : "";
                String descClause = input.get("description") != null
                        ? " Description: \"" + input.get("description") + "\"" : "";
                yield "Add material \"" + input.get("title") + "\" to " + course.getName() + "."
                        + urlClause + descClause;
            }
            case "create_topic" -> {
                Course course = requireOwnedCourse(uuidParam(input, "courseId"), tutor);
                yield "Add topic \"" + input.get("name") + "\" to " + course.getName() + ".";
            }
            case "rename_topic" -> {
                Topic topic = requireOwnedTopic(uuidParam(input, "topicId"), tutor);
                yield "Rename topic \"" + topic.getName() + "\" to \"" + input.get("name")
                        + "\" in " + topic.getCourse().getName() + ".";
            }
            case "create_grade_category" -> {
                Course course = requireOwnedCourse(uuidParam(input, "courseId"), tutor);
                yield "Add grade category \"" + input.get("name") + "\" (" + input.get("weightPercent")
                        + "%) to " + course.getName() + ".";
            }
            case "update_grade_category" -> {
                GradeCategory category = requireOwnedGradeCategory(uuidParam(input, "categoryId"), tutor);
                yield "Update grade category \"" + category.getName() + "\" to \"" + input.get("name")
                        + "\" (" + input.get("weightPercent") + "%).";
            }
            case "save_session_notes" -> {
                Session session = requireOwnedSession(uuidParam(input, "sessionId"), tutor);
                yield "Save session notes for the session with " + session.getStudent().getName()
                        + " on " + session.getDate() + ": \"" + input.get("rawNotes") + "\"";
            }
            case "update_rubric_scores" -> {
                Submission submission = requireOwnedSubmission(uuidParam(input, "submissionId"), tutor);
                yield "Set rubric scores for " + submission.getStudent().getName()
                        + "'s submission on \"" + submission.getAssignment().getTitle() + "\": "
                        + describeScores(input);
            }
            case "draft_session_notes" -> {
                Session session = requireOwnedSession(uuidParam(input, "sessionId"), tutor);
                yield "Ask Sage to draft formatted notes from the raw notes for the session with "
                        + session.getStudent().getName() + " on " + session.getDate() + ".";
            }
            case "review_submission" -> {
                Submission submission = requireOwnedSubmission(uuidParam(input, "submissionId"), tutor);
                yield "Ask Sage to generate AI feedback for " + submission.getStudent().getName()
                        + "'s submission on \"" + submission.getAssignment().getTitle()
                        + "\" (not visible to the student until you release it).";
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

    private CourseResponse updateCourse(Map<String, Object> input, User tutor) {
        UUID courseId = uuidParam(input, "courseId");
        String name = String.valueOf(input.get("name"));
        String subject = String.valueOf(input.get("subject"));
        String description = input.get("description") != null ? String.valueOf(input.get("description")) : null;
        return courseService.updateCourse(courseId, new UpdateCourseRequest(name, subject, description), tutor.getEmail());
    }

    private AssignmentResponse createAssignment(Map<String, Object> input, User tutor) {
        UUID courseId = uuidParam(input, "courseId");
        String title = String.valueOf(input.get("title"));
        String description = input.get("description") != null ? String.valueOf(input.get("description")) : null;
        LocalDate dueDate = localDateParam(input, "dueDate");
        UUID topicId = input.get("topicId") != null ? uuidParam(input, "topicId") : null;
        UUID gradeCategoryId = input.get("gradeCategoryId") != null ? uuidParam(input, "gradeCategoryId") : null;
        return assignmentService.createAssignment(courseId,
                new CreateAssignmentRequest(title, description, dueDate, topicId, gradeCategoryId), tutor.getEmail());
    }

    private MaterialResponse createMaterial(Map<String, Object> input, User tutor) {
        UUID courseId = uuidParam(input, "courseId");
        String title = String.valueOf(input.get("title"));
        String description = input.get("description") != null ? String.valueOf(input.get("description")) : null;
        String url = input.get("url") != null ? String.valueOf(input.get("url")) : null;
        UUID topicId = input.get("topicId") != null ? uuidParam(input, "topicId") : null;
        return materialService.createMaterial(courseId, new CreateMaterialRequest(title, description, url, topicId), tutor.getEmail());
    }

    private TopicResponse createTopic(Map<String, Object> input, User tutor) {
        UUID courseId = uuidParam(input, "courseId");
        return topicService.createTopic(courseId, new CreateTopicRequest(String.valueOf(input.get("name"))), tutor.getEmail());
    }

    private TopicResponse renameTopic(Map<String, Object> input, User tutor) {
        UUID topicId = uuidParam(input, "topicId");
        return topicService.renameTopic(topicId, new RenameTopicRequest(String.valueOf(input.get("name"))), tutor.getEmail());
    }

    private GradeCategoryResponse createGradeCategory(Map<String, Object> input, User tutor) {
        UUID courseId = uuidParam(input, "courseId");
        BigDecimal weightPercent = bigDecimalParam(input, "weightPercent");
        return gradeCategoryService.createCategory(courseId,
                new CreateGradeCategoryRequest(String.valueOf(input.get("name")), weightPercent), tutor.getEmail());
    }

    private GradeCategoryResponse updateGradeCategory(Map<String, Object> input, User tutor) {
        UUID categoryId = uuidParam(input, "categoryId");
        BigDecimal weightPercent = bigDecimalParam(input, "weightPercent");
        return gradeCategoryService.updateCategory(categoryId,
                new UpdateGradeCategoryRequest(String.valueOf(input.get("name")), weightPercent), tutor.getEmail());
    }

    private SessionNoteResponse saveSessionNotes(Map<String, Object> input, User tutor) {
        UUID sessionId = uuidParam(input, "sessionId");
        return sessionService.saveSessionNotes(sessionId,
                new SaveSessionNotesRequest(String.valueOf(input.get("rawNotes"))), tutor.getEmail());
    }

    @SuppressWarnings("unchecked")
    private SubmissionResponse updateRubricScores(Map<String, Object> input, User tutor) {
        UUID submissionId = uuidParam(input, "submissionId");
        List<Map<String, Object>> rawScores = (List<Map<String, Object>>) input.get("scores");
        List<UpdateRubricScoresRequest.ScoreInput> scores = rawScores.stream()
                .map(s -> new UpdateRubricScoresRequest.ScoreInput(
                        UUID.fromString(String.valueOf(s.get("criterionId"))),
                        bigDecimalParam(s, "pointsAwarded")))
                .toList();
        return submissionService.updateRubricScores(submissionId, new UpdateRubricScoresRequest(scores), tutor.getEmail());
    }

    private SessionNoteResponse draftSessionNotes(Map<String, Object> input, User tutor) {
        return sageService.draftSessionNotes(uuidParam(input, "sessionId"), tutor.getEmail());
    }

    private SubmissionResponse reviewSubmission(Map<String, Object> input, User tutor) {
        return sageService.reviewSubmission(uuidParam(input, "submissionId"), tutor.getEmail());
    }

    @SuppressWarnings("unchecked")
    private String describeScores(Map<String, Object> input) {
        List<Map<String, Object>> scores = (List<Map<String, Object>>) input.get("scores");
        StringBuilder sb = new StringBuilder();
        for (Map<String, Object> score : scores) {
            UUID criterionId = UUID.fromString(String.valueOf(score.get("criterionId")));
            RubricCriterion criterion = rubricCriterionRepository.findById(criterionId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid criterionId: " + criterionId));
            if (!sb.isEmpty()) {
                sb.append(", ");
            }
            sb.append(criterion.getName()).append(": ").append(score.get("pointsAwarded"))
                    .append("/").append(criterion.getPointsPossible());
        }
        return sb.toString();
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

    private Topic requireOwnedTopic(UUID topicId, User tutor) {
        Topic topic = topicRepository.findById(topicId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Topic not found"));
        if (!topic.getCourse().getTutor().getId().equals(tutor.getId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Topic not found");
        }
        return topic;
    }

    private GradeCategory requireOwnedGradeCategory(UUID categoryId, User tutor) {
        GradeCategory category = gradeCategoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Grade category not found"));
        if (!category.getCourse().getTutor().getId().equals(tutor.getId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Grade category not found");
        }
        return category;
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

    private BigDecimal bigDecimalParam(Map<String, Object> input, String key) {
        Object value = input.get(key);
        if (value == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Missing required parameter: " + key);
        }
        try {
            return new BigDecimal(String.valueOf(value));
        } catch (NumberFormatException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid number for " + key + ": " + value);
        }
    }

    private LocalDate localDateParam(Map<String, Object> input, String key) {
        Object value = input.get(key);
        if (value == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Missing required parameter: " + key);
        }
        try {
            return LocalDate.parse(String.valueOf(value));
        } catch (DateTimeParseException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid date for " + key + ": " + value);
        }
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to serialize tool result", e);
        }
    }
}
