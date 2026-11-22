package com.ablmvp.sprintassistant.metrics;

import com.ablmvp.sprintassistant.api.dto.AssignmentRecommendationResponse;
import com.ablmvp.sprintassistant.api.dto.RiskItemResponse;
import com.ablmvp.sprintassistant.api.dto.SprintPredictionResponse;
import com.ablmvp.sprintassistant.domain.Issue;
import com.ablmvp.sprintassistant.domain.Sprint;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SprintAIAnalyzerTest {

    private SprintAIAnalyzer analyzer;

    @BeforeEach
    void setUp() {
        analyzer = new SprintAIAnalyzer();
    }

    @Test
    void predict_highProbability_whenSprintIsAhead() {
        // Sprint started 2 days ago, ends in 12 days, 8/10 issues done
        LocalDateTime start = LocalDateTime.now().minusDays(2);
        LocalDateTime end = LocalDateTime.now().plusDays(12);
        List<Issue> issues = List.of(
                issue("P-1", "Done", "High", null, null),
                issue("P-2", "Done", "High", null, null),
                issue("P-3", "Done", "High", null, null),
                issue("P-4", "Done", "High", null, null),
                issue("P-5", "Done", "High", null, null),
                issue("P-6", "Done", "High", null, null),
                issue("P-7", "Done", "High", null, null),
                issue("P-8", "Done", "High", null, null),
                issue("P-9", "In Progress", "Medium", "Alice", null),
                issue("P-10", "To Do", "Low", "Bob", null)
        );
        Sprint sprint = new Sprint("S1", "Sprint", "active", null, start, end, null, 1, issues);
        SprintPredictionResponse p = analyzer.predict(sprint);
        assertThat(p.completionProbability()).isGreaterThan(0.70);
        assertThat(p.trend()).isEqualTo("AHEAD");
    }

    @Test
    void predict_lowProbability_whenMostlyBlocked() {
        LocalDateTime start = LocalDateTime.now().minusDays(10);
        LocalDateTime end = LocalDateTime.now().plusDays(2);
        List<Issue> issues = List.of(
                issue("P-1", "Blocked", "High", "Alice", null),
                issue("P-2", "Blocked", "High", "Bob", null),
                issue("P-3", "Blocked", "High", "Charlie", null),
                issue("P-4", "Blocked", "High", "Dave", null),
                issue("P-5", "Blocked", "High", "Eve", null),
                issue("P-6", "To Do", "Medium", null, null)
        );
        Sprint sprint = new Sprint("S2", "Sprint", "active", null, start, end, null, 1, issues);
        SprintPredictionResponse p = analyzer.predict(sprint);
        assertThat(p.completionProbability()).isLessThan(0.50);
    }

    @Test
    void predict_probabilityBetween005And098() {
        Sprint sprint = new Sprint("S3", "Sprint", "active", null, null, null, null, 1, List.of());
        SprintPredictionResponse p = analyzer.predict(sprint);
        assertThat(p.completionProbability()).isBetween(0.05, 0.98);
    }

    @Test
    void identifyRisks_highRisk_forBlockedHighPriorityUnassigned() {
        // blocked (0.40) + high-priority-not-started — but "Blocked" status maps statusCategory
        // to "In Progress" in the helper, so isToDo() is false. Use large story points (0.15)
        // + blocked (0.40) + no-assignee (0.10) + stale-in-progress (0.20) = 0.85 → HIGH
        Issue blocked = new Issue("P-1", "10001", "Blocked stale large issue",
                "Story", "Blocked", "In Progress", "High",
                null, "Reporter",
                BigDecimal.valueOf(13), // > 8 → large task
                null, null, null,
                LocalDateTime.now().minusDays(10),
                LocalDateTime.now().minusDays(5), // stale: >3 days ago
                null,
                List.of(), "S4", "Sprint");
        Sprint sprint = new Sprint("S4", "Sprint", "active", null, null, null, null, 1, List.of(blocked));
        List<RiskItemResponse> risks = analyzer.identifyRisks(sprint);
        assertThat(risks).hasSize(1);
        assertThat(risks.get(0).riskLevel()).isEqualTo("HIGH");
    }

    @Test
    void identifyRisks_lowRisk_smallAssignedInProgress() {
        Issue small = issue("P-2", "In Progress", "Low", "Alice",
                LocalDateTime.now().minusHours(1));
        Sprint sprint = new Sprint("S5", "Sprint", "active", null, null, null, null, 1, List.of(small));
        List<RiskItemResponse> risks = analyzer.identifyRisks(sprint);
        // score: only possible factor is small SP (not > 8), no blockers, assigned, fresh update
        assertThat(risks).isEmpty();
    }

    @Test
    void identifyRisks_completedIssues_notFlagged() {
        Issue done = issue("P-3", "Done", "High", null, null);
        Sprint sprint = new Sprint("S6", "Sprint", "active", null, null, null, null, 1, List.of(done));
        List<RiskItemResponse> risks = analyzer.identifyRisks(sprint);
        assertThat(risks).isEmpty();
    }

    @Test
    void identifyRisks_sortedByScoreDescending() {
        Issue highRisk = issue("P-1", "Blocked", "High", null, null);  // blocked + no assignee
        Issue medRisk = issue("P-2", "To Do", "High", null, null);      // high priority not started + no assignee
        Sprint sprint = new Sprint("S7", "Sprint", "active", null, null, null, null, 1, List.of(medRisk, highRisk));
        List<RiskItemResponse> risks = analyzer.identifyRisks(sprint);
        if (risks.size() >= 2) {
            assertThat(risks.get(0).riskScore()).isGreaterThanOrEqualTo(risks.get(1).riskScore());
        }
    }

    @Test
    void recommendAssignments_onlyForUnassignedNonDoneIssues() {
        List<Issue> issues = List.of(
                issue("P-1", "Done", "High", null, null),         // done — skip
                issue("P-2", "To Do", "Medium", "Alice", null),   // assigned — skip
                issue("P-3", "In Progress", "Medium", null, null) // unassigned active — recommend
        );
        Sprint sprint = new Sprint("S8", "Sprint", "active", null, null, null, null, 1, issues);
        List<AssignmentRecommendationResponse> recs = analyzer.recommendAssignments(sprint);
        assertThat(recs).hasSize(1);
        assertThat(recs.get(0).issueKey()).isEqualTo("P-3");
        assertThat(recs.get(0).recommendedAssignee()).isEqualTo("Alice");
    }

    @Test
    void daysRemaining_zeroWhenEndDateInPast() {
        Sprint sprint = new Sprint("S9", "Sprint", "active", null,
                LocalDateTime.now().minusDays(14), LocalDateTime.now().minusDays(1),
                null, 1, List.of());
        assertThat(analyzer.daysRemaining(sprint)).isZero();
    }

    // ---- helpers ----

    private Issue issue(String key, String statusCategory, String priority,
                        String assignee, LocalDateTime updated) {
        boolean isDone = "Done".equalsIgnoreCase(statusCategory);
        String sc = isDone ? "Done"
                : statusCategory.equalsIgnoreCase("In Progress") ? "In Progress"
                : statusCategory.equalsIgnoreCase("Blocked") ? "In Progress"
                : "To Do";
        return new Issue(key, "10001", "Summary of " + key,
                "Story", statusCategory, sc, priority,
                assignee, "Reporter",
                BigDecimal.valueOf(3), null, null, null,
                LocalDateTime.now().minusDays(5),
                updated != null ? updated : LocalDateTime.now().minusDays(1),
                isDone ? LocalDateTime.now().minusDays(1) : null,
                List.of(), "S1", "Sprint 1");
    }
}
