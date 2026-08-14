package com.notare.submission;

import com.notare.assignment.Assignment;
import com.notare.assignment.AssignmentRepository;
import com.notare.course.EnrollmentRepository;
import com.notare.submission.dto.ReleaseFeedbackRequest;
import com.notare.submission.dto.SubmissionResponse;
import com.notare.submission.dto.SubmitAssignmentRequest;
import com.notare.user.User;
import com.notare.user.UserRepository;
import com.notare.user.UserRole;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class SubmissionService {

    private final SubmissionRepository submissionRepository;
    private final AssignmentRepository assignmentRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final UserRepository userRepository;

    public SubmissionService(
            SubmissionRepository submissionRepository,
            AssignmentRepository assignmentRepository,
            EnrollmentRepository enrollmentRepository,
            UserRepository userRepository
    ) {
        this.submissionRepository = submissionRepository;
        this.assignmentRepository = assignmentRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.userRepository = userRepository;
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

        return SubmissionResponse.from(submission);
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
                .map(SubmissionResponse::forStudent)
                .orElse(null);
    }

    @Transactional(readOnly = true)
    public SubmissionResponse getSubmission(UUID submissionId, String tutorEmail) {
        return SubmissionResponse.from(requireOwnedSubmission(submissionId, tutorEmail));
    }

    @Transactional(readOnly = true)
    public List<SubmissionResponse> listSubmissionsForAssignment(UUID assignmentId, String tutorEmail) {
        Assignment assignment = requireOwnedAssignment(assignmentId, tutorEmail);
        return submissionRepository.findByAssignmentId(assignment.getId()).stream()
                .map(SubmissionResponse::from)
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

        return SubmissionResponse.from(submission);
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
