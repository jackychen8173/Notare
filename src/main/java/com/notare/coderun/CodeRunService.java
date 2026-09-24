package com.notare.coderun;

import com.notare.assignment.Assignment;
import com.notare.assignment.AssignmentRepository;
import com.notare.coderun.dto.CodeRunResponse;
import com.notare.coderun.dto.RunCodeRequest;
import com.notare.course.EnrollmentRepository;
import com.notare.user.User;
import com.notare.user.UserRepository;
import com.notare.user.UserRole;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@Service
public class CodeRunService {

    private final RestClient codeRunRestClient;
    private final AssignmentRepository assignmentRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final UserRepository userRepository;
    private final String internalSecret;

    public CodeRunService(
            RestClient codeRunRestClient,
            AssignmentRepository assignmentRepository,
            EnrollmentRepository enrollmentRepository,
            UserRepository userRepository,
            @Value("${code-run.internal-secret}") String internalSecret
    ) {
        this.codeRunRestClient = codeRunRestClient;
        this.assignmentRepository = assignmentRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.userRepository = userRepository;
        this.internalSecret = internalSecret;
    }

    // Same requireStudent + enrollment check as SubmissionService.submitAssignment - duplicated
    // rather than shared, matching this codebase's existing style (e.g. SageToolExecutor
    // duplicates ownership checks that also live on the owning service).
    public CodeRunResponse runCode(UUID assignmentId, RunCodeRequest request, String studentEmail) {
        User student = requireStudent(studentEmail);

        Assignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Assignment not found"));

        boolean enrolled = enrollmentRepository.existsByStudentIdAndCourseId(
                student.getId(), assignment.getCourse().getId());
        if (!enrolled) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "You are not enrolled in this assignment's course");
        }

        try {
            CodeRunResponse response = codeRunRestClient.post()
                    .uri("/api/code-run")
                    .header("X-Internal-Secret", internalSecret)
                    .body(request)
                    .retrieve()
                    .body(CodeRunResponse.class);
            if (response == null) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Code execution service returned no response");
            }
            return response;
        } catch (RestClientException e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Failed to run code", e);
        }
    }

    private User requireStudent(String email) {
        return userRepository.findByEmail(email)
                .filter(user -> user.getRole() == UserRole.STUDENT)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));
    }
}
