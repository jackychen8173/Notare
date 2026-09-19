package com.notare.admin;

import com.notare.assignment.dto.AssignmentResponse;
import com.notare.common.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/admin/assignments")
@PreAuthorize("hasRole('ADMIN')")
public class AdminAssignmentController {

    private final AdminAssignmentService adminAssignmentService;

    public AdminAssignmentController(AdminAssignmentService adminAssignmentService) {
        this.adminAssignmentService = adminAssignmentService;
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<AssignmentResponse>> getAssignment(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(adminAssignmentService.getAssignment(id)));
    }
}
