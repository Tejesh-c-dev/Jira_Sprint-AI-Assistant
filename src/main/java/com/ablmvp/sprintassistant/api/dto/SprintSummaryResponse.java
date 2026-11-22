package com.ablmvp.sprintassistant.api.dto;

import java.time.LocalDateTime;

public record SprintSummaryResponse(
        String id,
        String name,
        String state,
        String goal,
        LocalDateTime startDate,
        LocalDateTime endDate,
        int totalIssues,
        int completedIssues) {
}
