package com.notare.material;

import com.notare.common.ApiResponse;
import com.notare.material.dto.CreateMaterialRequest;
import com.notare.material.dto.MaterialResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@PreAuthorize("hasRole('TUTOR')")
public class MaterialController {

    private final MaterialService materialService;

    public MaterialController(MaterialService materialService) {
        this.materialService = materialService;
    }

    @PostMapping("/api/courses/{courseId}/materials")
    public ResponseEntity<ApiResponse<MaterialResponse>> createMaterial(
            @PathVariable UUID courseId,
            @Valid @RequestBody CreateMaterialRequest request,
            Authentication authentication
    ) {
        MaterialResponse response = materialService.createMaterial(courseId, request, authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    @GetMapping("/api/courses/{courseId}/materials")
    public ResponseEntity<ApiResponse<List<MaterialResponse>>> listMaterials(
            @PathVariable UUID courseId,
            Authentication authentication
    ) {
        return ResponseEntity.ok(ApiResponse.success(materialService.listMaterials(courseId, authentication.getName())));
    }

    @DeleteMapping("/api/materials/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteMaterial(
            @PathVariable UUID id,
            Authentication authentication
    ) {
        materialService.deleteMaterial(id, authentication.getName());
        return ResponseEntity.ok(ApiResponse.success("Material deleted", null));
    }
}
