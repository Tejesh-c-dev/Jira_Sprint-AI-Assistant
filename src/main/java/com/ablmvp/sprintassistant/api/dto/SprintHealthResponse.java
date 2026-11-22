package com.ablmvp.sprintassistant.api.dto;

import java.math.BigDecimal;
import java.util.List;

public record SprintHealthResponse(
        String sprintId,
        String status,
        String statusReason,
        BigDecimal completionRate,
        int totalIssues,
        int completedIssues,
        int blockedIssues,
        List<String> recommendations) {
}
