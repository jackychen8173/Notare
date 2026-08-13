package com.notare.session.dto;

import com.notare.session.Session;
import com.notare.session.SessionStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public record SessionResponse(
        UUID id,
        UUID tutorId,
        UUID studentId,
        String studentName,
        UUID courseId,
        String courseName,
        LocalDateTime date,
        String subject,
        Integer duration,
        SessionStatus status
) {
    public static SessionResponse from(Session session) {
        return new SessionResponse(
                session.getId(),
                session.getTutor().getId(),
                session.getStudent().getId(),
                session.getStudent().getName(),
                session.getCourse() != null ? session.getCourse().getId() : null,
                session.getCourse() != null ? session.getCourse().getName() : null,
                session.getDate(),
                session.getSubject(),
                session.getDuration(),
                session.getStatus()
        );
    }
}
