package com.notare.announcement.dto;

import com.notare.announcement.Announcement;

import java.time.LocalDateTime;
import java.util.UUID;

public record AnnouncementResponse(
        UUID id,
        UUID courseId,
        UUID tutorId,
        String tutorName,
        String content,
        LocalDateTime createdAt
) {
    public static AnnouncementResponse from(Announcement announcement) {
        return new AnnouncementResponse(
                announcement.getId(),
                announcement.getCourse().getId(),
                announcement.getTutor().getId(),
                announcement.getTutor().getName(),
                announcement.getContent(),
                announcement.getCreatedAt()
        );
    }
}
