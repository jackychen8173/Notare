package com.notare.student;

import com.notare.common.ApiResponse;
import com.notare.student.dto.CreateStudentRequest;
import com.notare.student.dto.StudentResponse;
import com.notare.student.dto.UpdateStudentRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
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

    @PostMapping
    public ResponseEntity<ApiResponse<StudentResponse>> createStudent(@Valid @RequestBody CreateStudentRequest request) {
        StudentResponse response = studentService.createStudent(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<StudentResponse>>> listStudents() {
        return ResponseEntity.ok(ApiResponse.success(studentService.listStudents()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<StudentResponse>> getStudent(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(studentService.getStudent(id)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<StudentResponse>> updateStudent(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateStudentRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success(studentService.updateStudent(id, request)));
    }
}
