package com.notare.submission;

import com.notare.assignment.Assignment;
import com.notare.assignment.AssignmentRepository;
import com.notare.course.EnrollmentRepository;
import com.notare.rubric.Rubric;
import com.notare.rubric.RubricCriterion;
import com.notare.rubric.RubricCriterionRepository;
import com.notare.rubric.RubricRepository;
import com.notare.submission.dto.ReleaseFeedbackRequest;
import com.notare.submission.dto.RubricScoreItem;
import com.notare.submission.dto.SubmissionResponse;
import com.notare.submission.dto.SubmitAssignmentRequest;
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

    public SubmissionService(
            SubmissionRepository submissionRepository,
            AssignmentRepository assignmentRepository,
            EnrollmentRepository enrollmentRepository,
            UserRepository userRepository,
            RubricRepository rubricRepository,
            RubricCriterionRepository rubricCriterionRepository,
            SubmissionCriterionScoreRepository criterionScoreRepository
    ) {
        this.submissionRepository = submissionRepository;
        this.assignmentRepository = assignmentRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.userRepository = userRepository;
        this.rubricRepository = rubricRepository;
        this.rubricCriterionRepository = rubricCriterionRepository;
        this.criterionScoreRepository = criterionScoreRepository;
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

        Submission submission = Submission.builder()
                .assignment(assignment)
                .student(student)
                .content(request.content())
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
