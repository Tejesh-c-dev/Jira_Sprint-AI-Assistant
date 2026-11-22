package com.ablmvp.sprintassistant.metrics;

import com.ablmvp.sprintassistant.api.dto.SprintAnalysisResponse;
import com.ablmvp.sprintassistant.api.dto.SprintHealthResponse;
import com.ablmvp.sprintassistant.domain.Issue;
import com.ablmvp.sprintassistant.domain.Sprint;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SprintMetricsServiceTest {

    private SprintMetricsService service;

    @BeforeEach
    void setUp() {
        service = new SprintMetricsService();
    }

    @Test
    void analyze_zeroIssues_returnsZeroMetrics() {
        Sprint sprint = sprint(List.of());
        SprintAnalysisResponse r = service.analyze(sprint);
        assertThat(r.totalIssues()).isZero();
        assertThat(r.completedIssues()).isZero();
        assertThat(r.completionRate()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(r.identifiedRisks()).isEmpty();
    }

    @Test
    void analyze_allCompleted_returns100PercentRate() {
        List<Issue> issues = List.of(
                issue("PROJ-1", "Done", 5),
                issue("PROJ-2", "Done", 3)
        );
        Sprint sprint = sprint(issues);
        SprintAnalysisResponse r = service.analyze(sprint);
        assertThat(r.completionRate()).isEqualByComparingTo("100.0");
        assertThat(r.completedIssues()).isEqualTo(2);
    }

    @Test
    void analyze_mixedState_correctCounts() {
        List<Issue> issues = List.of(
                issue("PROJ-1", "Done", 5),
                issue("PROJ-2", "In Progress", 3),
                issue("PROJ-3", "To Do", 2)
        );
        Sprint sprint = sprint(issues);
        SprintAnalysisResponse r = service.analyze(sprint);
        assertThat(r.totalIssues()).isEqualTo(3);
        assertThat(r.completedIssues()).isEqualTo(1);
        assertThat(r.inProgressIssues()).isEqualTo(1);
    }

    @Test
    void analyze_blockedIssue_appearsInRisks() {
        List<Issue> issues = List.of(issueBlocked("PROJ-1", 3));
        Sprint sprint = sprint(issues);
        SprintAnalysisResponse r = service.analyze(sprint);
        assertThat(r.blockedIssues()).isEqualTo(1);
        assertThat(r.identifiedRisks()).anyMatch(risk -> risk.contains("blocked"));
    }

    @Test
    void analyze_nullStoryPointsIgnored_inSum() {
        List<Issue> issues = List.of(
                issueNoSP("PROJ-1", "Done"),
                issue("PROJ-2", "Done", 5)
        );
        Sprint sprint = sprint(issues);
        SprintAnalysisResponse r = service.analyze(sprint);
        assertThat(r.totalStoryPoints()).isEqualByComparingTo("5");
        assertThat(r.completedStoryPoints()).isEqualByComparingTo("5");
    }

    @Test
    void health_highCompletionLowBlocked_isHealthy() {
        List<Issue> issues = List.of(
                issue("PROJ-1", "Done", 8),
                issue("PROJ-2", "Done", 7),
                issue("PROJ-3", "Done", 5)
        );
        SprintHealthResponse h = service.health(sprint(issues));
        assertThat(h.status()).isEqualTo("HEALTHY");
    }

    @Test
    void health_mediumCompletion_isWatch() {
        List<Issue> issues = List.of(
                issue("PROJ-1", "Done", 5),
                issue("PROJ-2", "In Progress", 5),
                issue("PROJ-3", "To Do", 5)
        );
        SprintHealthResponse h = service.health(sprint(issues));
        // 33% completion → WATCH or AT_RISK depending on blocked ratio
        assertThat(h.status()).isIn("WATCH", "AT_RISK");
    }

    @Test
    void health_highBlockedRatio_isAtRisk() {
        // 0 completed, 5 blocked out of 5 total
        List<Issue> issues = List.of(
                issueBlocked("PROJ-1", 3),
                issueBlocked("PROJ-2", 3),
                issueBlocked("PROJ-3", 3),
                issueBlocked("PROJ-4", 3),
                issueBlocked("PROJ-5", 3)
        );
        SprintHealthResponse h = service.health(sprint(issues));
        assertThat(h.status()).isEqualTo("AT_RISK");
    }

    @Test
    void health_blocked_includesRecommendation() {
        List<Issue> issues = List.of(issueBlocked("PROJ-1", 3));
        SprintHealthResponse h = service.health(sprint(issues));
        assertThat(h.recommendations()).anyMatch(r -> r.contains("blocked"));
    }

    // ---- helpers ----

    private Sprint sprint(List<Issue> issues) {
        return new Sprint("S1", "Sprint 1", "active", "Goal",
                null, null, null, 1, issues);
    }

    private Issue issue(String key, String statusCategory, int sp) {
        return new Issue(key, "10001", "Summary of " + key,
                "Story", statusCategory, statusCategory, "Medium",
                "Alice", "Bob", BigDecimal.valueOf(sp),
                null, null, null, null, null, null,
                List.of(), "S1", "Sprint 1");
    }

    private Issue issueNoSP(String key, String statusCategory) {
        return new Issue(key, "10002", "No SP issue",
                "Task", statusCategory, statusCategory, "Low",
                "Alice", "Bob", null,
                null, null, null, null, null, null,
                List.of(), "S1", "Sprint 1");
    }

    private Issue issueBlocked(String key, int sp) {
        return new Issue(key, "10003", "Blocked issue " + key,
                "Bug", "Blocked", "In Progress", "High",
                "Charlie", "Dave", BigDecimal.valueOf(sp),
                null, null, null, null, null, null,
                List.of(), "S1", "Sprint 1");
    }
}
