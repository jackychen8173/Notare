package com.notare.material;

import com.notare.common.ApiResponse;
import com.notare.material.dto.MaterialResponse;
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
public class StudentMaterialController {

    private final MaterialService materialService;

    public StudentMaterialController(MaterialService materialService) {
        this.materialService = materialService;
    }

    @GetMapping("/api/student/courses/{id}/materials")
    public ResponseEntity<ApiResponse<List<MaterialResponse>>> listMaterials(
            @PathVariable UUID id,
            Authentication authentication
    ) {
        return ResponseEntity.ok(
                ApiResponse.success(materialService.listMaterialsForEnrolledCourse(id, authentication.getName())));
    }
}
