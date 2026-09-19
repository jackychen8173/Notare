package com.notare.topic.dto;

import com.notare.topic.Topic;

import java.time.LocalDateTime;
import java.util.UUID;

public record TopicResponse(
        UUID id,
        UUID courseId,
        String name,
        LocalDateTime createdAt
) {
    public static TopicResponse from(Topic topic) {
        return new TopicResponse(
                topic.getId(),
                topic.getCourse().getId(),
                topic.getName(),
                topic.getCreatedAt()
        );
    }
}
