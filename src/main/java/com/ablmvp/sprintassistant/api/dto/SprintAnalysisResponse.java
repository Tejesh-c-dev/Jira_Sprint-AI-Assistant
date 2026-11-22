package com.ablmvp.sprintassistant.api.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public record SprintAnalysisResponse(
        String sprintId,
        String sprintName,
        String state,
        int totalIssues,
        int completedIssues,
        int inProgressIssues,
        int blockedIssues,
        int unassignedActiveIssues,
        BigDecimal totalStoryPoints,
        BigDecimal completedStoryPoints,
        BigDecimal completionRate,
        Map<String, Integer> workloadByAssignee,
        List<String> identifiedRisks) {
}
