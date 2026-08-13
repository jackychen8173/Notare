package com.notare.session.dto;

import com.notare.session.SessionNote;

import java.time.LocalDateTime;
import java.util.UUID;

public record SessionNoteResponse(
        UUID id,
        UUID sessionId,
        String rawNotes,
        String formattedNotes,
        LocalDateTime createdAt
) {
    public static SessionNoteResponse from(SessionNote note) {
        return new SessionNoteResponse(
                note.getId(),
                note.getSession().getId(),
                note.getRawNotes(),
                note.getFormattedNotes(),
                note.getCreatedAt()
        );
    }
}
