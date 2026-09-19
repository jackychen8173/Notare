package com.notare.admin.dto;

public record AdminDashboardResponse(
        long tutorCount,
        long studentCount,
        long courseCount,
        long submissionsPending,
        long submissionsReleased
) {
}
