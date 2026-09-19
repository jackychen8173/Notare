package com.notare.admin;

import com.notare.assignment.Assignment;
import com.notare.assignment.AssignmentRepository;
import com.notare.submission.Submission;
import com.notare.submission.SubmissionCriterionScoreRepository;
import com.notare.submission.SubmissionRepository;
import com.notare.submission.dto.RubricScoreItem;
import com.notare.submission.dto.SubmissionResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class AdminSubmissionService {

    private final SubmissionRepository submissionRepository;
    private final AssignmentRepository assignmentRepository;
    private final SubmissionCriterionScoreRepository criterionScoreRepository;

    public AdminSubmissionService(
            SubmissionRepository submissionRepository,
            AssignmentRepository assignmentRepository,
            SubmissionCriterionScoreRepository criterionScoreRepository
    ) {
        this.submissionRepository = submissionRepository;
        this.assignmentRepository = assignmentRepository;
        this.criterionScoreRepository = criterionScoreRepository;
    }

    public SubmissionResponse getSubmission(UUID id) {
        Submission submission = requireSubmission(id);
        return SubmissionResponse.from(submission, loadRubricScores(submission.getId()));
    }

    public List<SubmissionResponse> listSubmissionsForAssignment(UUID assignmentId) {
        Assignment assignment = requireAssignment(assignmentId);
        return submissionRepository.findByAssignmentId(assignment.getId()).stream()
                .map(submission -> SubmissionResponse.from(submission, loadRubricScores(submission.getId())))
                .toList();
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

    private Submission requireSubmission(UUID id) {
        return submissionRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Submission not found"));
    }

    private Assignment requireAssignment(UUID id) {
        return assignmentRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Assignment not found"));
    }
}
