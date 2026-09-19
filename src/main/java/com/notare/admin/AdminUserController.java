package com.notare.admin;

import com.notare.admin.dto.AdminUserResponse;
import com.notare.common.ApiResponse;
import com.notare.user.UserRole;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/users")
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserController {

    private final AdminUserService adminUserService;

    public AdminUserController(AdminUserService adminUserService) {
        this.adminUserService = adminUserService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<AdminUserResponse>>> listUsers(
            @RequestParam(required = false) UserRole role,
            @RequestParam(required = false) Boolean active
    ) {
        return ResponseEntity.ok(ApiResponse.success(adminUserService.listUsers(role, active)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<AdminUserResponse>> getUser(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(adminUserService.getUser(id)));
    }

    @PatchMapping("/{id}/deactivate")
    public ResponseEntity<ApiResponse<AdminUserResponse>> deactivateUser(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(adminUserService.deactivateUser(id)));
    }

    @PatchMapping("/{id}/reactivate")
    public ResponseEntity<ApiResponse<AdminUserResponse>> reactivateUser(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(adminUserService.reactivateUser(id)));
    }
}
