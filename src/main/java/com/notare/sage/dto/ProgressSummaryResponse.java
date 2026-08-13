package com.notare.sage.dto;

import java.util.UUID;

public record ProgressSummaryResponse(
        UUID studentId,
        String summary
) {
}
