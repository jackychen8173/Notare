package com.notare.submission;

import com.notare.assignment.Assignment;
import com.notare.assignment.AssignmentRepository;
import com.notare.course.EnrollmentRepository;
import com.notare.rubric.Rubric;
import com.notare.rubric.RubricCriterion;
import com.notare.rubric.RubricCriterionRepository;
import com.notare.rubric.RubricRepository;
import com.notare.submission.dto.CreateLineCommentRequest;
import com.notare.submission.dto.LineCommentResponse;
import com.notare.submission.dto.ReleaseFeedbackRequest;
import com.notare.submission.dto.RubricScoreItem;
import com.notare.submission.dto.SubmissionResponse;
import com.notare.submission.dto.SubmitAssignmentRequest;
import com.notare.submission.dto.UpdateLineCommentRequest;
import com.notare.submission.dto.UpdateRubricScoresRequest;
import com.notare.user.User;
import com.notare.user.UserRepository;
import com.notare.user.UserRole;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@Transactional
public class SubmissionService {

    private final SubmissionRepository submissionRepository;
    private final AssignmentRepository assignmentRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final UserRepository userRepository;
    private final RubricRepository rubricRepository;
    private final RubricCriterionRepository rubricCriterionRepository;
    private final SubmissionCriterionScoreRepository criterionScoreRepository;
    private final SubmissionLineCommentRepository lineCommentRepository;

    public SubmissionService(
            SubmissionRepository submissionRepository,
            AssignmentRepository assignmentRepository,
            EnrollmentRepository enrollmentRepository,
            UserRepository userRepository,
            RubricRepository rubricRepository,
            RubricCriterionRepository rubricCriterionRepository,
            SubmissionCriterionScoreRepository criterionScoreRepository,
            SubmissionLineCommentRepository lineCommentRepository
    ) {
        this.submissionRepository = submissionRepository;
        this.assignmentRepository = assignmentRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.userRepository = userRepository;
        this.rubricRepository = rubricRepository;
        this.rubricCriterionRepository = rubricCriterionRepository;
        this.criterionScoreRepository = criterionScoreRepository;
        this.lineCommentRepository = lineCommentRepository;
    }

    public SubmissionResponse submitAssignment(UUID assignmentId, SubmitAssignmentRequest request, String studentEmail) {
        User student = requireStudent(studentEmail);

        Assignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Assignment not found"));

        boolean enrolled = enrollmentRepository.existsByStudentIdAndCourseId(
                student.getId(), assignment.getCourse().getId());
        if (!enrolled) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not enrolled in this assignment's course");
        }

        // One version at a time: a new one only once the last has feedback, and only if the tutor
        // turned resubmission on for this assignment.
        int attemptNumber = submissionRepository
                .findFirstByStudentIdAndAssignmentIdOrderBySubmittedAtDesc(student.getId(), assignmentId)
                .map(latest -> {
                    if (latest.getReleasedAt() == null) {
                        throw new ResponseStatusException(HttpStatus.CONFLICT,
                                "You already submitted this assignment. Wait for feedback before submitting again");
                    }
                    if (!assignment.isAllowResubmission()) {
                        throw new ResponseStatusException(HttpStatus.CONFLICT,
                                "This assignment doesn't accept resubmissions");
                    }
                    return latest.getAttemptNumber() + 1;
                })
                .orElse(1);

        Submission submission = Submission.builder()
                .assignment(assignment)
                .student(student)
                .content(request.content())
                .attemptNumber(attemptNumber)
                .feedbackStatus(FeedbackStatus.PENDING)
                .build();

        submissionRepository.save(submission);

        return SubmissionResponse.from(submission, List.of());
    }

    @Transactional(readOnly = true)
    public SubmissionResponse getMySubmission(UUID assignmentId, String studentEmail) {
        User student = requireStudent(studentEmail);

        Assignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Assignment not found"));

        boolean enrolled = enrollmentRepository.existsByStudentIdAndCourseId(
                student.getId(), assignment.getCourse().getId());
        if (!enrolled) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not enrolled in this assignment's course");
        }

        return submissionRepository
                .findFirstByStudentIdAndAssignmentIdOrderBySubmittedAtDesc(student.getId(), assignmentId)
                .map(submission -> SubmissionResponse.forStudent(submission, loadRubricScores(submission.getId())))
                .orElse(null);
    }

    /** Every version the student has submitted for an assignment, newest first. */
    @Transactional(readOnly = true)
    public List<SubmissionResponse> listMySubmissions(UUID assignmentId, String studentEmail) {
        User student = requireStudent(studentEmail);

        Assignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Assignment not found"));

        boolean enrolled = enrollmentRepository.existsByStudentIdAndCourseId(
                student.getId(), assignment.getCourse().getId());
        if (!enrolled) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not enrolled in this assignment's course");
        }

        return submissionRepository
                .findByStudentIdAndAssignmentIdOrderByAttemptNumberDesc(student.getId(), assignmentId).stream()
                .map(submission -> SubmissionResponse.forStudent(submission, loadRubricScores(submission.getId())))
                .toList();
    }

    /**
     * The line comments a student may see on their own submission: PUBLISHED ones, and only once
     * feedback is released - the same gate as SubmissionResponse.forStudent. Sage's unreviewed
     * SUGGESTED drafts never reach a student.
     */
    @Transactional(readOnly = true)
    public List<LineCommentResponse> listMyLineComments(UUID submissionId, String studentEmail) {
        User student = requireStudent(studentEmail);
        Submission submission = submissionRepository.findById(submissionId)
                .filter(candidate -> candidate.getStudent().getId().equals(student.getId()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Submission not found"));

        if (submission.getReleasedAt() == null) {
            return List.of();
        }
        return lineCommentRepository
                .findBySubmissionIdAndStatusOrderByLineNumberAscCreatedAtAsc(submission.getId(), LineCommentStatus.PUBLISHED)
                .stream()
                .map(LineCommentResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<LineCommentResponse> listLineComments(UUID submissionId, String tutorEmail) {
        Submission submission = requireOwnedSubmission(submissionId, tutorEmail);
        return lineCommentRepository.findBySubmissionIdOrderByLineNumberAscCreatedAtAsc(submission.getId()).stream()
                .map(LineCommentResponse::from)
                .toList();
    }

    public LineCommentResponse addLineComment(UUID submissionId, CreateLineCommentRequest request, String tutorEmail) {
        Submission submission = requireOwnedSubmission(submissionId, tutorEmail);

        long lineCount = submission.getContent().lines().count();
        if (request.lineNumber() > Math.max(lineCount, 1)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Line " + request.lineNumber() + " doesn't exist");
        }

        SubmissionLineComment comment = SubmissionLineComment.builder()
                .submission(submission)
                .lineNumber(request.lineNumber())
                .body(request.body().trim())
                .source(LineCommentSource.TUTOR)
                .status(LineCommentStatus.PUBLISHED)
                .build();
        lineCommentRepository.save(comment);

        return LineCommentResponse.from(comment);
    }

    /** Edits a comment's text. On a Sage suggestion this is how the tutor accepts it: it becomes PUBLISHED. */
    public LineCommentResponse updateLineComment(UUID commentId, UpdateLineCommentRequest request, String tutorEmail) {
        SubmissionLineComment comment = requireOwnedLineComment(commentId, tutorEmail);
        comment.setBody(request.body().trim());
        comment.setStatus(LineCommentStatus.PUBLISHED);
        lineCommentRepository.save(comment);
        return LineCommentResponse.from(comment);
    }

    public void deleteLineComment(UUID commentId, String tutorEmail) {
        lineCommentRepository.delete(requireOwnedLineComment(commentId, tutorEmail));
    }

    @Transactional(readOnly = true)
    public SubmissionResponse getSubmission(UUID submissionId, String tutorEmail) {
        Submission submission = requireOwnedSubmission(submissionId, tutorEmail);
        return SubmissionResponse.from(submission, loadRubricScores(submission.getId()));
    }

    @Transactional(readOnly = true)
    public List<SubmissionResponse> listSubmissionsForAssignment(UUID assignmentId, String tutorEmail) {
        Assignment assignment = requireOwnedAssignment(assignmentId, tutorEmail);
        return submissionRepository.findByAssignmentId(assignment.getId()).stream()
                .map(submission -> SubmissionResponse.from(submission, loadRubricScores(submission.getId())))
                .toList();
    }

    public SubmissionResponse releaseFeedback(UUID submissionId, ReleaseFeedbackRequest request, String tutorEmail) {
        Submission submission = requireOwnedSubmission(submissionId, tutorEmail);

        boolean hasTutorFeedback = request.tutorFeedback() != null && !request.tutorFeedback().isBlank();
        if (!hasTutorFeedback && submission.getSageFeedback() == null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Nothing to release: no Sage feedback has been generated and no tutor feedback was provided");
        }

        if (hasTutorFeedback) {
            submission.setTutorFeedback(request.tutorFeedback());
        }
        submission.setGrade(request.grade());
        // REVISED when the tutor added their own commentary on top of (or instead of) Sage's draft, APPROVED otherwise
        submission.setFeedbackStatus(hasTutorFeedback ? FeedbackStatus.REVISED : FeedbackStatus.APPROVED);
        submission.setReleasedAt(LocalDateTime.now());

        submissionRepository.save(submission);

        return SubmissionResponse.from(submission, loadRubricScores(submission.getId()));
    }

    public SubmissionResponse updateRubricScores(UUID submissionId, UpdateRubricScoresRequest request, String tutorEmail) {
        Submission submission = requireOwnedSubmission(submissionId, tutorEmail);

        Rubric rubric = rubricRepository.findByAssignmentId(submission.getAssignment().getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "This assignment has no rubric"));

        Map<UUID, RubricCriterion> criteriaById = rubricCriterionRepository
                .findByRubricIdOrderByPositionAsc(rubric.getId()).stream()
                .collect(java.util.stream.Collectors.toMap(RubricCriterion::getId, c -> c));

        criterionScoreRepository.deleteBySubmissionId(submission.getId());
        // Force the delete to hit the DB before the inserts below are flushed. Hibernate's default
        // flush order runs entity insertions before entity deletions regardless of code order, so
        // without this, re-scoring a submission that already has rows for the same
        // (submission_id, criterion_id) pair violates the unique constraint on that pair.
        criterionScoreRepository.flush();

        for (UpdateRubricScoresRequest.ScoreInput input : request.scores()) {
            RubricCriterion criterion = criteriaById.get(input.criterionId());
            if (criterion == null) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Criterion not found on this assignment's rubric");
            }

            SubmissionCriterionScore score = SubmissionCriterionScore.builder()
                    .submission(submission)
                    .criterion(criterion)
                    .pointsAwarded(input.pointsAwarded())
                    .build();
            criterionScoreRepository.save(score);
        }

        return SubmissionResponse.from(submission, loadRubricScores(submission.getId()));
    }

    private List<RubricScoreItem> loadRubricScores(UUID submissionId) {
        return criterionScoreRepository.findBySubmissionId(submissionId).stream()
                .map(score -> new RubricScoreItem(
                        score.getCriterion().getId(),
                        score.getCriterion().getName(),
                        score.getPointsAwarded(),
                        score.getCriterion().getPointsPossible()
                ))
                .toList();
    }

    private User requireStudent(String email) {
        return userRepository.findByEmail(email)
                .filter(user -> user.getRole() == UserRole.STUDENT)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));
    }

    private User requireTutor(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));
    }

    private Submission requireOwnedSubmission(UUID submissionId, String tutorEmail) {
        User tutor = requireTutor(tutorEmail);
        Submission submission = submissionRepository.findById(submissionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Submission not found"));

        if (!submission.getAssignment().getCourse().getTutor().getId().equals(tutor.getId())) {
            // 404, not 403 - avoid confirming another tutor's submission exists
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Submission not found");
        }

        return submission;
    }

    private SubmissionLineComment requireOwnedLineComment(UUID commentId, String tutorEmail) {
        User tutor = requireTutor(tutorEmail);
        SubmissionLineComment comment = lineCommentRepository.findById(commentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Comment not found"));

        if (!comment.getSubmission().getAssignment().getCourse().getTutor().getId().equals(tutor.getId())) {
            // 404, not 403 - avoid confirming another tutor's comment exists
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Comment not found");
        }

        return comment;
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
