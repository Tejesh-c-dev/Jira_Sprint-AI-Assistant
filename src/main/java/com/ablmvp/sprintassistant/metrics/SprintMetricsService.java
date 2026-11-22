package com.ablmvp.sprintassistant.metrics;

import com.ablmvp.sprintassistant.api.dto.SprintAnalysisResponse;
import com.ablmvp.sprintassistant.api.dto.SprintHealthResponse;
import com.ablmvp.sprintassistant.domain.Issue;
import com.ablmvp.sprintassistant.domain.Sprint;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

@Service
public class SprintMetricsService {

    public SprintAnalysisResponse analyze(Sprint sprint) {
        List<Issue> issues = sprint.issues();
        int total = issues.size();
        int completed = sprint.completedIssues();
        int inProgress = sprint.inProgressIssues();
        int blocked = sprint.blockedIssues();
        int unassigned = sprint.unassignedIssues();

        BigDecimal totalSP = sum(issues);
        BigDecimal completedSP = sum(issues.stream().filter(Issue::isCompleted).toList());

        BigDecimal rate = totalSP.compareTo(BigDecimal.ZERO) > 0
                ? completedSP.divide(totalSP, 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100)).setScale(1, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        Map<String, Integer> workload = new TreeMap<>();
        for (Issue i : issues) {
            if (i.assignee() != null && !i.assignee().isBlank() && !i.isCompleted()) {
                workload.merge(i.assignee(), 1, Integer::sum);
            }
        }

        List<String> risks = new ArrayList<>();
        if (blocked > 0) risks.add(blocked + " issue(s) are blocked");
        if (unassigned > 0) risks.add(unassigned + " active issue(s) have no assignee");
        long large = issues.stream().filter(i -> !i.isCompleted())
                .filter(i -> i.storyPoints() != null
                          && i.storyPoints().compareTo(BigDecimal.valueOf(8)) > 0).count();
        if (large > 0) risks.add(large + " issue(s) have >8 story points");

        return new SprintAnalysisResponse(sprint.id(), sprint.name(), sprint.state(),
                total, completed, inProgress, blocked, unassigned,
                totalSP, completedSP, rate, workload, risks);
    }

    public SprintHealthResponse health(Sprint sprint) {
        SprintAnalysisResponse a = analyze(sprint);
        BigDecimal rate = a.completionRate();
        int blocked = a.blockedIssues();
        int total = a.totalIssues();
        double blockedRatio = total > 0 ? (double) blocked / total : 0.0;

        String status;
        String reason;

        if (rate.compareTo(BigDecimal.valueOf(80)) >= 0 && blockedRatio <= 0.20) {
            status = "HEALTHY";
            reason = "Completion rate >= 80% and blocked ratio <= 20%.";
        } else if (rate.compareTo(BigDecimal.valueOf(60)) >= 0 || blockedRatio <= 0.40) {
            status = "WATCH";
            reason = "Completion rate 60-79% or blocked ratio 20-40%.";
        } else {
            status = "AT_RISK";
            reason = "Completion rate < 60% or blocked ratio > 40%.";
        }

        List<String> recs = new ArrayList<>();
        if (blocked > 0) recs.add("Resolve " + blocked + " blocked issue(s) in today's standup.");
        if (sprint.unassignedIssues() > 0)
            recs.add("Assign " + sprint.unassignedIssues() + " unowned issue(s) immediately.");
        if ("AT_RISK".equals(status))
            recs.add("Review sprint scope — consider removing low-priority items.");
        if ("WATCH".equals(status))
            recs.add("Increase daily check-in frequency to catch blockers early.");

        return new SprintHealthResponse(sprint.id(), status, reason, rate,
                total, a.completedIssues(), blocked, recs);
    }

    BigDecimal sum(List<Issue> issues) {
        return issues.stream().map(Issue::storyPoints).filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
