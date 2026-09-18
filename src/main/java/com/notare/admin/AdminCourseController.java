package com.notare.admin;

import com.notare.assignment.dto.AssignmentResponse;
import com.notare.common.ApiResponse;
import com.notare.course.dto.CourseResponse;
import com.notare.student.dto.StudentResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/courses")
@PreAuthorize("hasRole('ADMIN')")
public class AdminCourseController {

    private final AdminCourseService adminCourseService;

    public AdminCourseController(AdminCourseService adminCourseService) {
        this.adminCourseService = adminCourseService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<CourseResponse>>> listCourses() {
        return ResponseEntity.ok(ApiResponse.success(adminCourseService.listCourses()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CourseResponse>> getCourse(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(adminCourseService.getCourse(id)));
    }

    @GetMapping("/{id}/students")
    public ResponseEntity<ApiResponse<List<StudentResponse>>> listStudents(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(adminCourseService.listStudents(id)));
    }

    @GetMapping("/{id}/assignments")
    public ResponseEntity<ApiResponse<List<AssignmentResponse>>> listAssignments(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(adminCourseService.listAssignments(id)));
    }
}
