package com.notare.announcement;

import com.notare.announcement.dto.AnnouncementResponse;
import com.notare.common.ApiResponse;
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
public class StudentAnnouncementController {

    private final AnnouncementService announcementService;

    public StudentAnnouncementController(AnnouncementService announcementService) {
        this.announcementService = announcementService;
    }

    @GetMapping("/api/student/courses/{id}/announcements")
    public ResponseEntity<ApiResponse<List<AnnouncementResponse>>> listAnnouncements(
            @PathVariable UUID id,
            Authentication authentication
    ) {
        return ResponseEntity.ok(
                ApiResponse.success(announcementService.listAnnouncementsForEnrolledCourse(id, authentication.getName())));
    }
}
