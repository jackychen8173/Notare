package com.notare.gradecategory;

import com.notare.common.ApiResponse;
import com.notare.gradecategory.dto.CreateGradeCategoryRequest;
import com.notare.gradecategory.dto.GradeCategoryResponse;
import com.notare.gradecategory.dto.UpdateGradeCategoryRequest;
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
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@PreAuthorize("hasRole('TUTOR')")
public class GradeCategoryController {

    private final GradeCategoryService gradeCategoryService;

    public GradeCategoryController(GradeCategoryService gradeCategoryService) {
        this.gradeCategoryService = gradeCategoryService;
    }

    @PostMapping("/api/courses/{courseId}/grade-categories")
    public ResponseEntity<ApiResponse<GradeCategoryResponse>> createCategory(
            @PathVariable UUID courseId,
            @Valid @RequestBody CreateGradeCategoryRequest request,
            Authentication authentication
    ) {
        GradeCategoryResponse response = gradeCategoryService.createCategory(courseId, request, authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    @GetMapping("/api/courses/{courseId}/grade-categories")
    public ResponseEntity<ApiResponse<List<GradeCategoryResponse>>> listCategories(
            @PathVariable UUID courseId,
            Authentication authentication
    ) {
        return ResponseEntity.ok(ApiResponse.success(gradeCategoryService.listCategories(courseId, authentication.getName())));
    }

    @PatchMapping("/api/grade-categories/{id}")
    public ResponseEntity<ApiResponse<GradeCategoryResponse>> updateCategory(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateGradeCategoryRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.ok(ApiResponse.success(gradeCategoryService.updateCategory(id, request, authentication.getName())));
    }

    @DeleteMapping("/api/grade-categories/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteCategory(
            @PathVariable UUID id,
            Authentication authentication
    ) {
        gradeCategoryService.deleteCategory(id, authentication.getName());
        return ResponseEntity.ok(ApiResponse.success("Grade category deleted", null));
    }
}
