package com.ablmvp.sprintassistant.api.dto;

import java.util.List;

public record RiskItemResponse(
        String issueKey,
        String summary,
        String assignee,
        double riskScore,
        String riskLevel,
        List<String> factors,
        List<String> mitigations) {
}
