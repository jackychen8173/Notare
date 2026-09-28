package com.notare.demo;

import com.notare.auth.dto.AuthResponse;
import com.notare.common.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/** Public (under /api/auth/**, so no JWT needed): the landing page's "Try it" buttons call this. */
@RestController
public class DemoController {

    private final DemoService demoService;

    public DemoController(DemoService demoService) {
        this.demoService = demoService;
    }

    @PostMapping("/api/auth/demo")
    public ResponseEntity<ApiResponse<AuthResponse>> startDemo(
            @Valid @RequestBody DemoLoginRequest request,
            HttpServletRequest httpRequest
    ) {
        AuthResponse response = demoService.startDemo(request.role(), clientKey(httpRequest));
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    /**
     * The client IP as seen by Railway's proxy: the last X-Forwarded-For entry. Earlier entries are
     * whatever the client sent, so they can't be trusted for rate limiting.
     */
    private static String clientKey(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            String[] hops = forwarded.split(",");
            return hops[hops.length - 1].trim();
        }
        return request.getRemoteAddr();
    }
}
