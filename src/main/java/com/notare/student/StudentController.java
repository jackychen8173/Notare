package com.notare.student;

import com.notare.common.ApiResponse;
import com.notare.student.dto.StudentResponse;
import com.notare.student.dto.UpdateStudentRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/students")
@PreAuthorize("hasRole('TUTOR')")
public class StudentController {

    private final StudentService studentService;

    public StudentController(StudentService studentService) {
        this.studentService = studentService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<StudentResponse>>> listStudents(Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success(studentService.listStudents(authentication.getName())));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<StudentResponse>> getStudent(@PathVariable UUID id, Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success(studentService.getStudent(id, authentication.getName())));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<StudentResponse>> updateStudent(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateStudentRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.ok(ApiResponse.success(studentService.updateStudent(id, request, authentication.getName())));
    }
}
