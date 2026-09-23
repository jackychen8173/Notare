package com.notare.quizattempt;

import com.notare.common.ApiResponse;
import com.notare.quizattempt.dto.GradeAnswerRequest;
import com.notare.quizattempt.dto.QuizAttemptResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@PreAuthorize("hasRole('TUTOR')")
public class QuizAttemptController {

    private final QuizAttemptService quizAttemptService;

    public QuizAttemptController(QuizAttemptService quizAttemptService) {
        this.quizAttemptService = quizAttemptService;
    }

    @GetMapping("/api/quizzes/{quizId}/attempts")
    public ResponseEntity<ApiResponse<List<QuizAttemptResponse>>> listAttempts(
            @PathVariable UUID quizId,
            Authentication authentication
    ) {
        return ResponseEntity.ok(
                ApiResponse.success(quizAttemptService.listAttemptsForQuiz(quizId, authentication.getName())));
    }

    @GetMapping("/api/quiz-attempts/{id}")
    public ResponseEntity<ApiResponse<QuizAttemptResponse>> getAttempt(
            @PathVariable UUID id,
            Authentication authentication
    ) {
        return ResponseEntity.ok(ApiResponse.success(quizAttemptService.getAttempt(id, authentication.getName())));
    }

    @PatchMapping("/api/quiz-attempts/{id}/answers/{questionId}/grade")
    public ResponseEntity<ApiResponse<QuizAttemptResponse>> gradeAnswer(
            @PathVariable UUID id,
            @PathVariable UUID questionId,
            @Valid @RequestBody GradeAnswerRequest request,
            Authentication authentication
    ) {
        QuizAttemptResponse response =
                quizAttemptService.gradeAnswer(id, questionId, request, authentication.getName());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PatchMapping("/api/quiz-attempts/{id}/release")
    public ResponseEntity<ApiResponse<QuizAttemptResponse>> releaseAttempt(
            @PathVariable UUID id,
            Authentication authentication
    ) {
        return ResponseEntity.ok(
                ApiResponse.success(quizAttemptService.releaseAttempt(id, authentication.getName())));
    }
}
