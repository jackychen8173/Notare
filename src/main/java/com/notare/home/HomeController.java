package com.notare.home;

import com.notare.common.ApiResponse;
import com.notare.home.dto.StudentHomeResponse;
import com.notare.home.dto.TutorHomeResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HomeController {

    private final HomeService homeService;

    public HomeController(HomeService homeService) {
        this.homeService = homeService;
    }

    @GetMapping("/api/home")
    @PreAuthorize("hasRole('TUTOR')")
    public ResponseEntity<ApiResponse<TutorHomeResponse>> tutorHome(Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success(homeService.tutorHome(authentication.getName())));
    }

    @GetMapping("/api/student/home")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ApiResponse<StudentHomeResponse>> studentHome(Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success(homeService.studentHome(authentication.getName())));
    }
}
