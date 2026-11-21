package com.ablmvp.sprintassistant.domain;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record Issue(
        String key,
        String id,
        String summary,
        String issueType,
        String status,
        String statusCategory,
        String priority,
        String assignee,
        String reporter,
        BigDecimal storyPoints,
        BigDecimal originalEstimate,
        BigDecimal remainingEstimate,
        BigDecimal timeSpent,
        LocalDateTime created,
        LocalDateTime updated,
        LocalDateTime resolutionDate,
        List<String> labels,
        String sprintId,
        String sprintName) {

    public Issue {
        labels = labels == null ? List.of() : List.copyOf(labels);
    }

    public boolean isCompleted() {
        return "Done".equalsIgnoreCase(statusCategory);
    }

    public boolean isInProgress() {
        return "In Progress".equalsIgnoreCase(statusCategory);
    }

    public boolean isToDo() {
        return "To Do".equalsIgnoreCase(statusCategory);
    }
}
