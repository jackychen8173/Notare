package com.notare.course;

import com.notare.common.ApiResponse;
import com.notare.course.dto.CourseResponse;
import com.notare.course.dto.CreateCourseRequest;
import com.notare.course.dto.UpdateCourseRequest;
import com.notare.student.dto.StudentResponse;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/courses")
@PreAuthorize("hasRole('TUTOR')")
public class CourseController {

    private final CourseService courseService;

    public CourseController(CourseService courseService) {
        this.courseService = courseService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<CourseResponse>> createCourse(
            @Valid @RequestBody CreateCourseRequest request,
            Authentication authentication
    ) {
        CourseResponse response = courseService.createCourse(request, authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<CourseResponse>>> listCourses(
            @RequestParam(defaultValue = "false") boolean archived,
            Authentication authentication
    ) {
        return ResponseEntity.ok(ApiResponse.success(courseService.listCourses(authentication.getName(), archived)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CourseResponse>> getCourse(@PathVariable UUID id, Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success(courseService.getCourse(id, authentication.getName())));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<ApiResponse<CourseResponse>> updateCourse(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateCourseRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.ok(ApiResponse.success(courseService.updateCourse(id, request, authentication.getName())));
    }

    @PostMapping("/{id}/archive")
    public ResponseEntity<ApiResponse<CourseResponse>> archiveCourse(
            @PathVariable UUID id,
            Authentication authentication
    ) {
        return ResponseEntity.ok(ApiResponse.success(courseService.archiveCourse(id, authentication.getName())));
    }

    @PostMapping("/{id}/unarchive")
    public ResponseEntity<ApiResponse<CourseResponse>> unarchiveCourse(
            @PathVariable UUID id,
            Authentication authentication
    ) {
        return ResponseEntity.ok(ApiResponse.success(courseService.unarchiveCourse(id, authentication.getName())));
    }

    @GetMapping("/{id}/students")
    public ResponseEntity<ApiResponse<List<StudentResponse>>> listEnrolled(
            @PathVariable UUID id,
            Authentication authentication
    ) {
        return ResponseEntity.ok(ApiResponse.success(courseService.listEnrolledStudents(id, authentication.getName())));
    }

    @PostMapping("/{id}/join-code/regenerate")
    public ResponseEntity<ApiResponse<CourseResponse>> regenerateJoinCode(
            @PathVariable UUID id,
            Authentication authentication
    ) {
        return ResponseEntity.ok(ApiResponse.success(courseService.regenerateJoinCode(id, authentication.getName())));
    }

    @DeleteMapping("/{id}/students/{studentId}")
    public ResponseEntity<ApiResponse<Void>> removeStudent(
            @PathVariable UUID id,
            @PathVariable UUID studentId,
            Authentication authentication
    ) {
        courseService.removeStudent(id, studentId, authentication.getName());
        return ResponseEntity.ok(ApiResponse.success("Student removed", null));
    }
}
