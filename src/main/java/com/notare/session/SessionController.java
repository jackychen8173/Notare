package com.notare.session;

import com.notare.common.ApiResponse;
import com.notare.session.dto.CreateSessionRequest;
import com.notare.session.dto.SaveSessionNotesRequest;
import com.notare.session.dto.SessionNoteResponse;
import com.notare.session.dto.SessionResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/sessions")
@PreAuthorize("hasRole('TUTOR')")
public class SessionController {

    private final SessionService sessionService;

    public SessionController(SessionService sessionService) {
        this.sessionService = sessionService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<SessionResponse>> createSession(
            @Valid @RequestBody CreateSessionRequest request,
            Authentication authentication
    ) {
        SessionResponse response = sessionService.createSession(request, authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<SessionResponse>>> listSessions(Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success(sessionService.listSessions(authentication.getName())));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<SessionResponse>> getSession(@PathVariable UUID id, Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success(sessionService.getSession(id, authentication.getName())));
    }

    @PatchMapping("/{id}/complete")
    public ResponseEntity<ApiResponse<SessionResponse>> completeSession(@PathVariable UUID id, Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success(sessionService.completeSession(id, authentication.getName())));
    }

    @PostMapping("/{id}/notes")
    public ResponseEntity<ApiResponse<SessionNoteResponse>> saveSessionNotes(
            @PathVariable UUID id,
            @Valid @RequestBody SaveSessionNotesRequest request,
            Authentication authentication
    ) {
        SessionNoteResponse response = sessionService.saveSessionNotes(id, request, authentication.getName());
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
