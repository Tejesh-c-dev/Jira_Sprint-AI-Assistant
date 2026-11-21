package com.ablmvp.sprintassistant.domain;

import java.time.LocalDateTime;
import java.util.List;

public record Sprint(
        String id,
        String name,
        String state,
        String goal,
        LocalDateTime startDate,
        LocalDateTime endDate,
        LocalDateTime completeDate,
        int boardId,
        List<Issue> issues) {

    public Sprint {
        issues = issues == null ? List.of() : List.copyOf(issues);
    }

    public boolean isActive() {
        return "active".equalsIgnoreCase(state);
    }

    public int totalIssues() {
        return issues.size();
    }

    public int completedIssues() {
        return (int) issues.stream().filter(Issue::isCompleted).count();
    }

    public int inProgressIssues() {
        return (int) issues.stream().filter(Issue::isInProgress).count();
    }

    public int blockedIssues() {
        return (int) issues.stream()
                .filter(issue -> issue.status() != null
                        && issue.status().toLowerCase().contains("blocked"))
                .count();
    }

    public int unassignedIssues() {
        return (int) issues.stream()
                .filter(issue -> issue.assignee() == null || issue.assignee().isBlank())
                .filter(issue -> !issue.isCompleted())
                .count();
    }
}
