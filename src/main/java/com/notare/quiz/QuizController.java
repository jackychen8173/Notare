package com.notare.quiz;

import com.notare.common.ApiResponse;
import com.notare.quiz.dto.CreateQuestionRequest;
import com.notare.quiz.dto.CreateQuizRequest;
import com.notare.quiz.dto.QuizResponse;
import com.notare.quiz.dto.UpdateQuestionRequest;
import com.notare.quiz.dto.UpdateQuizRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@PreAuthorize("hasRole('TUTOR')")
public class QuizController {

    private final QuizService quizService;

    public QuizController(QuizService quizService) {
        this.quizService = quizService;
    }

    @PostMapping("/api/courses/{courseId}/quizzes")
    public ResponseEntity<ApiResponse<QuizResponse>> createQuiz(
            @PathVariable UUID courseId,
            @Valid @RequestBody CreateQuizRequest request,
            Authentication authentication
    ) {
        QuizResponse response = quizService.createQuiz(courseId, request, authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    @GetMapping("/api/courses/{courseId}/quizzes")
    public ResponseEntity<ApiResponse<List<QuizResponse>>> listQuizzes(
            @PathVariable UUID courseId,
            Authentication authentication
    ) {
        return ResponseEntity.ok(ApiResponse.success(quizService.listQuizzes(courseId, authentication.getName())));
    }

    @GetMapping("/api/quizzes/{id}")
    public ResponseEntity<ApiResponse<QuizResponse>> getQuiz(
            @PathVariable UUID id,
            Authentication authentication
    ) {
        return ResponseEntity.ok(ApiResponse.success(quizService.getQuiz(id, authentication.getName())));
    }

    @PutMapping("/api/quizzes/{id}")
    public ResponseEntity<ApiResponse<QuizResponse>> updateQuiz(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateQuizRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.ok(ApiResponse.success(quizService.updateQuiz(id, request, authentication.getName())));
    }

    @PatchMapping("/api/quizzes/{id}/publish")
    public ResponseEntity<ApiResponse<QuizResponse>> publishQuiz(
            @PathVariable UUID id,
            Authentication authentication
    ) {
        return ResponseEntity.ok(ApiResponse.success(quizService.publishQuiz(id, authentication.getName())));
    }

    @PatchMapping("/api/quizzes/{id}/unpublish")
    public ResponseEntity<ApiResponse<QuizResponse>> unpublishQuiz(
            @PathVariable UUID id,
            Authentication authentication
    ) {
        return ResponseEntity.ok(ApiResponse.success(quizService.unpublishQuiz(id, authentication.getName())));
    }

    @PostMapping("/api/quizzes/{id}/questions")
    public ResponseEntity<ApiResponse<QuizResponse>> addQuestion(
            @PathVariable UUID id,
            @Valid @RequestBody CreateQuestionRequest request,
            Authentication authentication
    ) {
        QuizResponse response = quizService.addQuestion(id, request, authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    @PutMapping("/api/quizzes/{id}/questions/{questionId}")
    public ResponseEntity<ApiResponse<QuizResponse>> updateQuestion(
            @PathVariable UUID id,
            @PathVariable UUID questionId,
            @Valid @RequestBody UpdateQuestionRequest request,
            Authentication authentication
    ) {
        QuizResponse response = quizService.updateQuestion(id, questionId, request, authentication.getName());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @DeleteMapping("/api/quizzes/{id}/questions/{questionId}")
    public ResponseEntity<ApiResponse<QuizResponse>> deleteQuestion(
            @PathVariable UUID id,
            @PathVariable UUID questionId,
            Authentication authentication
    ) {
        QuizResponse response = quizService.deleteQuestion(id, questionId, authentication.getName());
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
