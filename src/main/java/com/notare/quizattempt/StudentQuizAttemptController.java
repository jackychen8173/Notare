package com.notare.quizattempt;

import com.notare.common.ApiResponse;
import com.notare.quizattempt.dto.QuizAttemptResponse;
import com.notare.quizattempt.dto.UpsertAnswerRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@PreAuthorize("hasRole('STUDENT')")
public class StudentQuizAttemptController {

    private final QuizAttemptService quizAttemptService;

    public StudentQuizAttemptController(QuizAttemptService quizAttemptService) {
        this.quizAttemptService = quizAttemptService;
    }

    @PostMapping("/api/student/quizzes/{quizId}/attempt")
    public ResponseEntity<ApiResponse<QuizAttemptResponse>> startAttempt(
            @PathVariable UUID quizId,
            Authentication authentication
    ) {
        QuizAttemptResponse response = quizAttemptService.startAttempt(quizId, authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    @GetMapping("/api/student/quizzes/{quizId}/attempt")
    public ResponseEntity<ApiResponse<QuizAttemptResponse>> getMyAttempt(
            @PathVariable UUID quizId,
            Authentication authentication
    ) {
        return ResponseEntity.ok(
                ApiResponse.success(quizAttemptService.getMyAttempt(quizId, authentication.getName())));
    }

    @GetMapping("/api/student/quiz-attempts/{id}")
    public ResponseEntity<ApiResponse<QuizAttemptResponse>> getAttempt(
            @PathVariable UUID id,
            Authentication authentication
    ) {
        return ResponseEntity.ok(
                ApiResponse.success(quizAttemptService.getAttemptForStudent(id, authentication.getName())));
    }

    @PutMapping("/api/student/quiz-attempts/{id}/answers/{questionId}")
    public ResponseEntity<ApiResponse<QuizAttemptResponse>> upsertAnswer(
            @PathVariable UUID id,
            @PathVariable UUID questionId,
            @Valid @RequestBody UpsertAnswerRequest request,
            Authentication authentication
    ) {
        QuizAttemptResponse response =
                quizAttemptService.upsertAnswer(id, questionId, request, authentication.getName());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/api/student/quiz-attempts/{id}/submit")
    public ResponseEntity<ApiResponse<QuizAttemptResponse>> submitAttempt(
            @PathVariable UUID id,
            Authentication authentication
    ) {
        return ResponseEntity.ok(
                ApiResponse.success(quizAttemptService.submitAttempt(id, authentication.getName())));
    }
}
