package com.notare.announcement.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateAnnouncementRequest(
        @NotBlank String content
) {
}
