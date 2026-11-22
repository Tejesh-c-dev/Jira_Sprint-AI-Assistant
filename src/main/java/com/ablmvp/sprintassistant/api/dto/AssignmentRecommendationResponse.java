package com.ablmvp.sprintassistant.api.dto;

public record AssignmentRecommendationResponse(
        String issueKey,
        String issueSummary,
        String recommendedAssignee,
        String reason,
        double confidence) {
}
