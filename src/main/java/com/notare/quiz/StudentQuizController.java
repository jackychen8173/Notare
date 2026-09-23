package com.notare.quiz;

import com.notare.common.ApiResponse;
import com.notare.quiz.dto.QuizResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@PreAuthorize("hasRole('STUDENT')")
public class StudentQuizController {

    private final QuizService quizService;

    public StudentQuizController(QuizService quizService) {
        this.quizService = quizService;
    }

    @GetMapping("/api/student/courses/{id}/quizzes")
    public ResponseEntity<ApiResponse<List<QuizResponse>>> listQuizzes(
            @PathVariable UUID id,
            Authentication authentication
    ) {
        return ResponseEntity.ok(
                ApiResponse.success(quizService.listQuizzesForEnrolledCourse(id, authentication.getName())));
    }

    @GetMapping("/api/student/quizzes/{id}")
    public ResponseEntity<ApiResponse<QuizResponse>> getQuiz(
            @PathVariable UUID id,
            Authentication authentication
    ) {
        return ResponseEntity.ok(ApiResponse.success(quizService.getQuizForStudent(id, authentication.getName())));
    }
}
