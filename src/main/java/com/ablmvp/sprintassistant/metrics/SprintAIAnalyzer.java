package com.ablmvp.sprintassistant.metrics;

import com.ablmvp.sprintassistant.api.dto.AssignmentRecommendationResponse;
import com.ablmvp.sprintassistant.api.dto.RiskItemResponse;
import com.ablmvp.sprintassistant.api.dto.SprintPredictionResponse;
import com.ablmvp.sprintassistant.domain.Issue;
import com.ablmvp.sprintassistant.domain.Sprint;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Deterministic AI-style heuristics for sprint analysis.
 *
 * H1 — progress-pace ratio: (completed/total) / (elapsed/total_days).
 *       A value above 1.0 means the team is ahead of a linear pace.
 * H2 — blocked penalty: 5% per blocked issue, capped at 40%.
 * H3 — unassigned penalty: 3% per unassigned active issue, capped at 20%.
 */
@Service
public class SprintAIAnalyzer {

    public SprintPredictionResponse predict(Sprint sprint) {
        int total = sprint.totalIssues();
        int completed = sprint.completedIssues();
        int blocked = sprint.blockedIssues();
        int unassigned = sprint.unassignedIssues();

        long daysRemaining = daysRemaining(sprint);
        long totalDays = totalDays(sprint);
        long daysElapsed = totalDays - daysRemaining;

        double completionRatio = total > 0 ? (double) completed / total : 0.0;
        double elapsedFraction = totalDays > 0 ? (double) daysElapsed / totalDays : 0.5;
        double expectedRatio = elapsedFraction;

        // H1: pace ratio
        double h1 = elapsedFraction <= 0
                ? completionRatio
                : Math.min(1.0, completionRatio / Math.max(elapsedFraction, 0.01));
        // H2: blocked penalty (5% each, max 40%)
        double h2 = Math.min(0.40, blocked * 0.05);
        // H3: unassigned penalty (3% each, max 20%)
        double h3 = Math.min(0.20, unassigned * 0.03);

        double probability = Math.max(0.05, Math.min(0.98, h1 - h2 - h3));

        String trend;
        if (completionRatio > expectedRatio + 0.1) trend = "AHEAD";
        else if (completionRatio < expectedRatio - 0.1) trend = "BEHIND";
        else trend = "ON_TRACK";

        return new SprintPredictionResponse(sprint.id(), probability, daysRemaining,
                trend, insights(sprint, probability, trend),
                bottlenecks(sprint), actions(sprint, probability));
    }

    public List<RiskItemResponse> identifyRisks(Sprint sprint) {
        LocalDateTime now = LocalDateTime.now();
        List<RiskItemResponse> result = new ArrayList<>();
        for (Issue issue : sprint.issues()) {
            if (issue.isCompleted()) continue;
            double score = 0.0;
            List<String> factors = new ArrayList<>(), mits = new ArrayList<>();

            if (isBlocked(issue)) {
                score += 0.40;
                factors.add("Issue is blocked");
                mits.add("Escalate blocker in today's standup");
            }
            if (isHighPriorityNotStarted(issue)) {
                score += 0.30;
                factors.add("High-priority issue not yet started");
                mits.add("Start this issue today");
            }
            if (isStaleInProgress(issue, now)) {
                long d = ChronoUnit.DAYS.between(issue.updated(), now);
                score += 0.20;
                factors.add("In progress for " + d + " days without update");
                mits.add("Pair-program to unblock");
            }
            if (isLargeTask(issue)) {
                score += 0.15;
                factors.add("Story points > 8");
                mits.add("Split into smaller sub-tasks");
            }
            if (issue.assignee() == null || issue.assignee().isBlank()) {
                score += 0.10;
                factors.add("No assignee");
                mits.add("Assign immediately");
            }

            if (score > 0.25) {
                String level = score >= 0.65 ? "HIGH" : score >= 0.40 ? "MEDIUM" : "LOW";
                result.add(new RiskItemResponse(issue.key(), issue.summary(),
                        issue.assignee(), score, level, factors, mits));
            }
        }
        result.sort(Comparator.comparingDouble(RiskItemResponse::riskScore).reversed());
        return result;
    }

    public List<AssignmentRecommendationResponse> recommendAssignments(Sprint sprint) {
        Map<String, Long> workload = sprint.issues().stream()
                .filter(i -> !i.isCompleted())
                .filter(i -> i.assignee() != null && !i.assignee().isBlank())
                .collect(Collectors.groupingBy(Issue::assignee, Collectors.counting()));
        if (workload.isEmpty()) return List.of();

        List<AssignmentRecommendationResponse> recs = new ArrayList<>();
        for (Issue issue : sprint.issues()) {
            if (issue.assignee() != null && !issue.assignee().isBlank()) continue;
            if (issue.isCompleted()) continue;
            String best = workload.entrySet().stream()
                    .min(Comparator.comparingLong(Map.Entry<String, Long>::getValue)
                            .thenComparing(Map.Entry::getKey))
                    .map(Map.Entry::getKey).orElse(null);
            if (best == null) continue;
            long load = workload.getOrDefault(best, 0L);
            double confidence = Math.max(0.40, 1.0 - load * 0.10);
            recs.add(new AssignmentRecommendationResponse(
                    issue.key(), issue.summary(), best,
                    "Least loaded member with " + load + " active task(s)", confidence));
            workload.merge(best, 1L, Long::sum);
        }
        return recs;
    }

    long daysRemaining(Sprint sprint) {
        if (sprint.endDate() == null) return 7;
        return Math.max(0, ChronoUnit.DAYS.between(LocalDateTime.now(), sprint.endDate()));
    }

    long totalDays(Sprint sprint) {
        if (sprint.startDate() == null || sprint.endDate() == null) return 14;
        return Math.max(1, ChronoUnit.DAYS.between(sprint.startDate(), sprint.endDate()));
    }

    private boolean isBlocked(Issue i) {
        return i.status() != null && i.status().toLowerCase().contains("blocked");
    }

    private boolean isHighPriorityNotStarted(Issue i) {
        return ("Highest".equalsIgnoreCase(i.priority()) || "High".equalsIgnoreCase(i.priority()))
                && i.isToDo();
    }

    private boolean isStaleInProgress(Issue i, LocalDateTime now) {
        return i.isInProgress() && i.updated() != null
                && ChronoUnit.DAYS.between(i.updated(), now) > 3;
    }

    private boolean isLargeTask(Issue i) {
        return i.storyPoints() != null
                && i.storyPoints().compareTo(BigDecimal.valueOf(8)) > 0;
    }

    private List<String> insights(Sprint sprint, double prob, String trend) {
        List<String> list = new ArrayList<>();
        if (prob >= 0.80) list.add("High confidence sprint will complete on time.");
        else if (prob >= 0.60) list.add("Moderate risk — keep focus through the sprint.");
        else list.add("Low confidence — scope reduction or extra effort needed.");
        if ("AHEAD".equals(trend)) list.add("Velocity is above expected pace.");
        if ("BEHIND".equals(trend)) list.add("Velocity is below expected pace.");
        if (sprint.blockedIssues() > 0)
            list.add(sprint.blockedIssues() + " blocked issue(s) require attention.");
        return list;
    }

    private List<String> bottlenecks(Sprint sprint) {
        List<String> list = new ArrayList<>();
        Map<String, Long> wl = sprint.issues().stream()
                .filter(i -> !i.isCompleted())
                .filter(i -> i.assignee() != null && !i.assignee().isBlank())
                .collect(Collectors.groupingBy(Issue::assignee, Collectors.counting()));
        double avg = wl.values().stream().mapToLong(Long::longValue).average().orElse(0);
        wl.entrySet().stream().filter(e -> e.getValue() > avg * 1.5 && e.getValue() >= 3)
                .forEach(e -> list.add(e.getKey() + " has " + e.getValue() + " active tasks."));
        if (sprint.blockedIssues() > 1)
            list.add("Multiple blocked issues suggest a dependency chain.");
        return list;
    }

    private List<String> actions(Sprint sprint, double prob) {
        List<String> list = new ArrayList<>();
        if (prob < 0.60) {
            list.add("Identify items to defer to next sprint.");
            list.add("Run an emergency scope review with the product owner.");
        }
        if (sprint.blockedIssues() > 0) list.add("Resolve all blockers before end of day.");
        if (sprint.unassignedIssues() > 0) list.add("Assign all unowned issues now.");
        return list;
    }
}
