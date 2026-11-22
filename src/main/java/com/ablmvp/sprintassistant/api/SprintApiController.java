package com.ablmvp.sprintassistant.api;

import com.ablmvp.sprintassistant.api.dto.*;
import com.ablmvp.sprintassistant.domain.Sprint;
import com.ablmvp.sprintassistant.jira.JiraDataService;
import com.ablmvp.sprintassistant.metrics.SprintAIAnalyzer;
import com.ablmvp.sprintassistant.metrics.SprintMetricsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/sprints")
@Tag(name = "Sprints", description = "Live JIRA sprint operations")
public class SprintApiController {

    private final JiraDataService jiraDataService;
    private final SprintMetricsService metricsService;
    private final SprintAIAnalyzer aiAnalyzer;

    public SprintApiController(JiraDataService jiraDataService,
                               SprintMetricsService metricsService,
                               SprintAIAnalyzer aiAnalyzer) {
        this.jiraDataService = jiraDataService;
        this.metricsService = metricsService;
        this.aiAnalyzer = aiAnalyzer;
    }

    @GetMapping
    @Operation(summary = "List all sprints from the configured JIRA board")
    public List<SprintSummaryResponse> listSprints() {
        return jiraDataService.listSprints().stream().map(this::toSummary).toList();
    }

    @GetMapping("/active")
    @Operation(summary = "Get the currently active sprint with issues")
    public SprintDetailResponse getActiveSprint() {
        return toDetail(jiraDataService.getActiveSprint());
    }

    @GetMapping("/{sprintId}")
    @Operation(summary = "Get sprint details by ID")
    public ResponseEntity<SprintDetailResponse> getSprint(@PathVariable String sprintId) {
        if (sprintId == null || sprintId.isBlank()) throw new InvalidSprintIdException();
        return ResponseEntity.ok(toDetail(jiraDataService.getSprint(sprintId)));
    }

    @GetMapping("/{sprintId}/analysis")
    @Operation(summary = "Full sprint analysis with story-point metrics")
    public ResponseEntity<SprintAnalysisResponse> analyze(@PathVariable String sprintId) {
        if (sprintId == null || sprintId.isBlank()) throw new InvalidSprintIdException();
        return ResponseEntity.ok(metricsService.analyze(jiraDataService.getSprint(sprintId)));
    }

    @GetMapping("/{sprintId}/health")
    @Operation(summary = "Sprint health status with recommendations")
    public ResponseEntity<SprintHealthResponse> health(@PathVariable String sprintId) {
        if (sprintId == null || sprintId.isBlank()) throw new InvalidSprintIdException();
        return ResponseEntity.ok(metricsService.health(jiraDataService.getSprint(sprintId)));
    }

    @GetMapping("/{sprintId}/prediction")
    @Operation(summary = "AI-powered sprint completion prediction")
    public ResponseEntity<SprintPredictionResponse> predict(@PathVariable String sprintId) {
        if (sprintId == null || sprintId.isBlank()) throw new InvalidSprintIdException();
        return ResponseEntity.ok(aiAnalyzer.predict(jiraDataService.getSprint(sprintId)));
    }

    @GetMapping("/{sprintId}/risks")
    @Operation(summary = "Identify at-risk issues with explanations")
    public ResponseEntity<List<RiskItemResponse>> risks(@PathVariable String sprintId) {
        if (sprintId == null || sprintId.isBlank()) throw new InvalidSprintIdException();
        return ResponseEntity.ok(aiAnalyzer.identifyRisks(jiraDataService.getSprint(sprintId)));
    }

    @GetMapping("/{sprintId}/recommendations")
    @Operation(summary = "Task assignment recommendations for unassigned issues")
    public ResponseEntity<List<AssignmentRecommendationResponse>> recommendations(
            @PathVariable String sprintId) {
        if (sprintId == null || sprintId.isBlank()) throw new InvalidSprintIdException();
        return ResponseEntity.ok(
                aiAnalyzer.recommendAssignments(jiraDataService.getSprint(sprintId)));
    }

    @PostMapping("/refresh")
    @Operation(summary = "Clear the data cache and force a refresh from JIRA")
    public ResponseEntity<Void> refresh() {
        jiraDataService.invalidateCache();
        return ResponseEntity.noContent().build();
    }

    private SprintSummaryResponse toSummary(Sprint s) {
        return new SprintSummaryResponse(s.id(), s.name(), s.state(), s.goal(),
                s.startDate(), s.endDate(), s.totalIssues(), s.completedIssues());
    }

    private SprintDetailResponse toDetail(Sprint s) {
        return new SprintDetailResponse(s.id(), s.name(), s.state(), s.goal(),
                s.startDate(), s.endDate(), s.completeDate(),
                s.totalIssues(), s.completedIssues(), s.inProgressIssues(),
                s.blockedIssues(), s.issues());
    }
}
