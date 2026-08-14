package com.notare.session;

import com.notare.common.ApiResponse;
import com.notare.session.dto.SessionResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/student/sessions")
@PreAuthorize("hasRole('STUDENT')")
public class StudentSessionController {

    private final SessionService sessionService;

    public StudentSessionController(SessionService sessionService) {
        this.sessionService = sessionService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<SessionResponse>>> listMySessions(Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success(sessionService.listSessionsForStudent(authentication.getName())));
    }
}
