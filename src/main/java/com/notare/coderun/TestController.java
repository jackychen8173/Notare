package com.notare.coderun;

import com.notare.coderun.dto.RunCodeRequest;
import com.notare.coderun.dto.TestRequest;
import com.notare.coderun.dto.TestResponse;
import com.notare.coderun.dto.TestRunResponse;
import com.notare.common.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
public class TestController {

    private final TestService testService;

    public TestController(TestService testService) {
        this.testService = testService;
    }

    @GetMapping("/api/assignments/{assignmentId}/tests")
    @PreAuthorize("hasRole('TUTOR')")
    public ResponseEntity<ApiResponse<List<TestResponse>>> list(@PathVariable UUID assignmentId, Authentication auth) {
        return ResponseEntity.ok(ApiResponse.success(testService.listForTutor(assignmentId, auth.getName())));
    }

    @PostMapping("/api/assignments/{assignmentId}/tests")
    @PreAuthorize("hasRole('TUTOR')")
    public ResponseEntity<ApiResponse<TestResponse>> create(
            @PathVariable UUID assignmentId, @Valid @RequestBody TestRequest request, Authentication auth) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(testService.create(assignmentId, request, auth.getName())));
    }

    @PutMapping("/api/assignments/{assignmentId}/tests/{testId}")
    @PreAuthorize("hasRole('TUTOR')")
    public ResponseEntity<ApiResponse<TestResponse>> update(
            @PathVariable UUID assignmentId, @PathVariable UUID testId,
            @Valid @RequestBody TestRequest request, Authentication auth) {
        return ResponseEntity.ok(ApiResponse.success(testService.update(assignmentId, testId, request, auth.getName())));
    }

    @DeleteMapping("/api/assignments/{assignmentId}/tests/{testId}")
    @PreAuthorize("hasRole('TUTOR')")
    public ResponseEntity<ApiResponse<Void>> delete(
            @PathVariable UUID assignmentId, @PathVariable UUID testId, Authentication auth) {
        testService.delete(assignmentId, testId, auth.getName());
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @GetMapping("/api/student/assignments/{assignmentId}/tests")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ApiResponse<List<TestResponse>>> listForStudent(
            @PathVariable UUID assignmentId, Authentication auth) {
        return ResponseEntity.ok(ApiResponse.success(testService.listForStudent(assignmentId, auth.getName())));
    }

    /** A student runs the tests on their current code (stdin in the body is ignored). */
    @PostMapping("/api/assignments/{assignmentId}/check")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ApiResponse<TestRunResponse>> check(
            @PathVariable UUID assignmentId, @Valid @RequestBody RunCodeRequest request, Authentication auth) {
        return ResponseEntity.ok(ApiResponse.success(testService.check(assignmentId, request.code(), auth.getName())));
    }

    @GetMapping("/api/submissions/{submissionId}/test-results")
    @PreAuthorize("hasRole('TUTOR')")
    public ResponseEntity<ApiResponse<TestRunResponse>> stored(@PathVariable UUID submissionId, Authentication auth) {
        return ResponseEntity.ok(ApiResponse.success(testService.storedForSubmission(submissionId, auth.getName())));
    }

    @PostMapping("/api/submissions/{submissionId}/run-tests")
    @PreAuthorize("hasRole('TUTOR')")
    public ResponseEntity<ApiResponse<TestRunResponse>> runOnSubmission(
            @PathVariable UUID submissionId, Authentication auth) {
        return ResponseEntity.ok(ApiResponse.success(testService.runOnSubmission(submissionId, auth.getName())));
    }
}
