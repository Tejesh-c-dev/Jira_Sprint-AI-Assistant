package com.ablmvp.sprintassistant.api.dto;

import java.util.List;

public record SprintPredictionResponse(
        String sprintId,
        double completionProbability,
        long daysRemaining,
        String trend,
        List<String> insights,
        List<String> bottlenecks,
        List<String> suggestedActions) {
}
