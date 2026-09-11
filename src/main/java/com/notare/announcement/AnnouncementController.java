package com.notare.announcement;

import com.notare.announcement.dto.AnnouncementResponse;
import com.notare.announcement.dto.CreateAnnouncementRequest;
import com.notare.common.ApiResponse;
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
public class AnnouncementController {

    private final AnnouncementService announcementService;

    public AnnouncementController(AnnouncementService announcementService) {
        this.announcementService = announcementService;
    }

    @PostMapping("/api/courses/{courseId}/announcements")
    public ResponseEntity<ApiResponse<AnnouncementResponse>> createAnnouncement(
            @PathVariable UUID courseId,
            @Valid @RequestBody CreateAnnouncementRequest request,
            Authentication authentication
    ) {
        AnnouncementResponse response = announcementService.createAnnouncement(courseId, request, authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    @GetMapping("/api/courses/{courseId}/announcements")
    public ResponseEntity<ApiResponse<List<AnnouncementResponse>>> listAnnouncements(
            @PathVariable UUID courseId,
            Authentication authentication
    ) {
        return ResponseEntity.ok(ApiResponse.success(announcementService.listAnnouncements(courseId, authentication.getName())));
    }

    @DeleteMapping("/api/announcements/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteAnnouncement(
            @PathVariable UUID id,
            Authentication authentication
    ) {
        announcementService.deleteAnnouncement(id, authentication.getName());
        return ResponseEntity.ok(ApiResponse.success("Announcement deleted", null));
    }
}
