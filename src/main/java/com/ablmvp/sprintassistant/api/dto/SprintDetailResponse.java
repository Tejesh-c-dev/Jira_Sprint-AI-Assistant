package com.ablmvp.sprintassistant.api.dto;

import com.ablmvp.sprintassistant.domain.Issue;

import java.time.LocalDateTime;
import java.util.List;

public record SprintDetailResponse(
        String id,
        String name,
        String state,
        String goal,
        LocalDateTime startDate,
        LocalDateTime endDate,
        LocalDateTime completeDate,
        int totalIssues,
        int completedIssues,
        int inProgressIssues,
        int blockedIssues,
        List<Issue> issues) {
}
