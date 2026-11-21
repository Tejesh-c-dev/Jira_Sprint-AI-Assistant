# JIRA Sprint AI Assistant — Final Implementation Plan

**Course:** Software Engineering & Project Management (S5CCS02), 2026–27 ODD SEM
**Team:** Four teammates
**Deadline:** 16 December 2026
**Plan status:** Authoritative — supersedes `ABL_MVP_Project_Plan.md`,
`plan_teammate_1_improved.md`, and all `docs/` progress files.

---

## Purpose and Scope

This plan describes how to transform the current repository into a live, AI-powered JIRA
Sprint Assistant suitable for the ABL viva. It is written for execution by an AI coding
agent working top-to-bottom through each phase. Every section states exactly which files to
delete, which to keep, which to create, and what the code must do.

The current codebase is a CSV-based historical analyser. It has solid plumbing — a Spring
Boot skeleton, well-structured domain records, a working metrics layer, REST controllers,
DTOs, and 24 passing tests — but it reads from a 2020 dataset that cannot be licensed or
committed, and it has no frontend, no CI/CD, and no live JIRA connection. This plan fixes
all of that.

---

## What We Are Building

A single Spring Boot application that:

1. Connects to a real JIRA board via the JIRA REST API using an API token.
2. Fetches live sprint and issue data on demand, with a short-lived in-memory cache.
3. Calculates sprint health metrics (completion rate, blocked count, story-point velocity).
4. Applies deterministic AI-style heuristics to predict completion probability, flag
   at-risk issues, and recommend task assignments.
5. Serves a React + TypeScript single-page dashboard that shows all of the above.
6. Exposes two CLI commands (`list-sprints`, `analyze-sprint`) backed by the same service
   layer as the HTTP API.
7. Builds and tests through a Jenkins pipeline, packages into a Docker image, and deploys
   to a local Minikube cluster.

---

## Phased Delivery

```
Phase 1 — Codebase surgery          delete dead code, wire JIRA client
Phase 2 — Live JIRA data layer      fetch real sprints and issues
Phase 3 — Metrics and AI analysis   health, predictions, risks, recommendations
Phase 4 — React frontend and CLI    dashboard and command-line tools
Phase 5 — DevOps pipeline           Docker, Kubernetes, Jenkins
Phase 6 — Documentation and demo    SRS, SDS, UML, demo script, report
```

Each phase ends with a working, demo-able increment that builds with
`mvn clean test` or `mvn clean package`.

---

# PHASE 1 — Codebase Surgery

**Owner:** Teammate 1
**Duration:** 2–3 days
**Goal:** Delete every CSV and historical-data artefact, add the JIRA HTTP client, establish
the new configuration contract, and verify the application still compiles and starts.

---

## 1.1 Files to Delete Entirely

Delete all of the following. They must not exist after this phase.

```
# CSV data layer
src/main/java/com/ablmvp/sprintassistant/data/csv/CsvDataLoader.java
src/main/java/com/ablmvp/sprintassistant/data/csv/CsvFileAccess.java
src/main/java/com/ablmvp/sprintassistant/data/csv/NioCsvFileAccess.java
src/main/java/com/ablmvp/sprintassistant/data/csv/ProvisionalCsvMapping.java
src/main/java/com/ablmvp/sprintassistant/data/result/DatasetLoadOutcome.java

# CSV-specific configuration
src/main/java/com/ablmvp/sprintassistant/config/DataSourceProperties.java
src/main/java/com/ablmvp/sprintassistant/config/DatasetConfiguration.java

# API layer that depends on CSV
src/main/java/com/ablmvp/sprintassistant/api/DatasetStatusController.java
src/main/java/com/ablmvp/sprintassistant/api/SprintRepository.java
src/main/java/com/ablmvp/sprintassistant/api/HistoricalSprintService.java
src/main/java/com/ablmvp/sprintassistant/api/SprintReadService.java

# Domain types used only by CSV layer
src/main/java/com/ablmvp/sprintassistant/domain/DataKind.java
src/main/java/com/ablmvp/sprintassistant/domain/DatasetProvenance.java
src/main/java/com/ablmvp/sprintassistant/domain/FileFingerprint.java
src/main/java/com/ablmvp/sprintassistant/domain/FileRole.java
src/main/java/com/ablmvp/sprintassistant/domain/IssueSprintLink.java
src/main/java/com/ablmvp/sprintassistant/domain/SprintEstimateSums.java
src/main/java/com/ablmvp/sprintassistant/domain/SourceRef.java
src/main/java/com/ablmvp/sprintassistant/domain/UsabilityState.java

# Existing metrics layer — replace completely in Phase 3
src/main/java/com/ablmvp/sprintassistant/metrics/AssigneeConflict.java
src/main/java/com/ablmvp/sprintassistant/metrics/AssigneeDistribution.java
src/main/java/com/ablmvp/sprintassistant/metrics/HealthDimensionResult.java
src/main/java/com/ablmvp/sprintassistant/metrics/HealthDimensionStatus.java
src/main/java/com/ablmvp/sprintassistant/metrics/HealthStatus.java
src/main/java/com/ablmvp/sprintassistant/metrics/MetricAvailability.java
src/main/java/com/ablmvp/sprintassistant/metrics/MetricResult.java
src/main/java/com/ablmvp/sprintassistant/metrics/SprintHealthResult.java
src/main/java/com/ablmvp/sprintassistant/metrics/SprintHealthService.java
src/main/java/com/ablmvp/sprintassistant/metrics/SprintMetricsResult.java
src/main/java/com/ablmvp/sprintassistant/metrics/SprintMetricsService.java
src/main/java/com/ablmvp/sprintassistant/metrics/SprintSourceFacts.java

# All existing DTOs — replaced in Phase 2/3 with JIRA-shaped ones
src/main/java/com/ablmvp/sprintassistant/api/dto/AssigneeConflictResponse.java
src/main/java/com/ablmvp/sprintassistant/api/dto/AssigneeDistributionResponse.java
src/main/java/com/ablmvp/sprintassistant/api/dto/DatasetQualityResponse.java
src/main/java/com/ablmvp/sprintassistant/api/dto/DatasetStatusResponse.java
src/main/java/com/ablmvp/sprintassistant/api/dto/FileFingerprintResponse.java
src/main/java/com/ablmvp/sprintassistant/api/dto/HealthDimensionResponse.java
src/main/java/com/ablmvp/sprintassistant/api/dto/MetricResultResponse.java
src/main/java/com/ablmvp/sprintassistant/api/dto/ProvenanceResponse.java
src/main/java/com/ablmvp/sprintassistant/api/dto/QuarantinedRecordResponse.java
src/main/java/com/ablmvp/sprintassistant/api/dto/SourceRefResponse.java
src/main/java/com/ablmvp/sprintassistant/api/dto/SprintAnalysisResponse.java
src/main/java/com/ablmvp/sprintassistant/api/dto/SprintComparisonItemResponse.java
src/main/java/com/ablmvp/sprintassistant/api/dto/SprintComparisonResponse.java
src/main/java/com/ablmvp/sprintassistant/api/dto/SprintDetailResponse.java
src/main/java/com/ablmvp/sprintassistant/api/dto/SprintEstimateSumsResponse.java
src/main/java/com/ablmvp/sprintassistant/api/dto/SprintHealthResponse.java
src/main/java/com/ablmvp/sprintassistant/api/dto/SprintSourceFactsResponse.java
src/main/java/com/ablmvp/sprintassistant/api/dto/SprintSummaryResponse.java
src/main/java/com/ablmvp/sprintassistant/api/dto/TrendObservationResponse.java
src/main/java/com/ablmvp/sprintassistant/api/dto/TrendResponse.java
src/main/java/com/ablmvp/sprintassistant/api/dto/ValidationFindingResponse.java

# All existing tests — replaced with JIRA-backed tests in Phases 2-3
src/test/java/com/ablmvp/sprintassistant/api/DatasetStatusControllerTest.java
src/test/java/com/ablmvp/sprintassistant/api/HistoricalSprintServiceTest.java
src/test/java/com/ablmvp/sprintassistant/api/OpenApiIntegrationTest.java
src/test/java/com/ablmvp/sprintassistant/api/SprintApiControllerTest.java
src/test/java/com/ablmvp/sprintassistant/api/SprintReadServiceTest.java
src/test/java/com/ablmvp/sprintassistant/config/DataSourcePropertiesTest.java
src/test/java/com/ablmvp/sprintassistant/data/csv/CsvDataLoaderFixtureIntegrationTest.java
src/test/java/com/ablmvp/sprintassistant/data/csv/CsvDataLoaderTest.java
src/test/java/com/ablmvp/sprintassistant/data/csv/CsvDataValidationTest.java
src/test/java/com/ablmvp/sprintassistant/data/csv/RealDatasetIntegrityTest.java
src/test/java/com/ablmvp/sprintassistant/metrics/SprintHealthServiceTest.java
src/test/java/com/ablmvp/sprintassistant/metrics/SprintMetricsServiceTest.java

# Synthetic CSV fixtures
src/test/resources/fixtures/data-11/issues.csv
src/test/resources/fixtures/data-11/issue_sprint_summary.csv
src/test/resources/fixtures/data-11/sprints.csv
src/test/resources/fixtures/data-11/README.md

# Old documentation
docs/DATA_MAPPING.md
docs/DATA_MODEL.md
docs/BACKEND_PROGRESS.md
plan_teammate_1_improved.md
report.md
skill.md
ABL_MVP_Project_Plan.md
```

After deleting the above, check if the entire
`src/main/java/com/ablmvp/sprintassistant/data/` directory tree is now empty. If so,
delete it.

## 1.2 Files to Keep Unchanged

```
pom.xml                                        — update deps in 1.4 only
src/main/java/com/ablmvp/sprintassistant/SprintAssistantApplication.java
src/test/java/com/ablmvp/sprintassistant/SprintAssistantApplicationTests.java
src/main/java/com/ablmvp/sprintassistant/api/ApiExceptionHandler.java
src/main/java/com/ablmvp/sprintassistant/api/dto/ErrorResponse.java
src/main/java/com/ablmvp/sprintassistant/api/InvalidSprintIdException.java
src/main/java/com/ablmvp/sprintassistant/api/SprintNotFoundException.java
.gitignore
data/README.md
```

## 1.3 Domain Models to Rewrite In Place

Do not delete these files. Overwrite their content.

**`src/main/java/com/ablmvp/sprintassistant/domain/Issue.java`**

```java
package com.ablmvp.sprintassistant.domain;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * A single JIRA issue, normalised from the JIRA REST API v2 response.
 * All optional fields are nullable. Never fill nulls with fabricated defaults.
 */
public record Issue(
        String key,                   // e.g. "PROJ-42"
        String id,                    // JIRA internal numeric ID as string
        String summary,
        String issueType,             // Story | Bug | Task | Epic | Sub-task
        String status,                // raw JIRA status name, e.g. "In Progress"
        String statusCategory,        // "To Do" | "In Progress" | "Done"
        String priority,              // Highest | High | Medium | Low | Lowest
        String assignee,              // display name, null if unassigned
        String reporter,
        BigDecimal storyPoints,       // custom field story_points, null if absent
        BigDecimal originalEstimate,  // seconds as BigDecimal, null if absent
        BigDecimal remainingEstimate,
        BigDecimal timeSpent,
        LocalDateTime created,
        LocalDateTime updated,
        LocalDateTime resolutionDate,
        List<String> labels,
        String sprintId,
        String sprintName
) {
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
```

**`src/main/java/com/ablmvp/sprintassistant/domain/Sprint.java`**

```java
package com.ablmvp.sprintassistant.domain;

import java.time.LocalDateTime;
import java.util.List;

/**
 * A JIRA Scrum sprint, normalised from the JIRA Agile REST API.
 */
public record Sprint(
        String id,
        String name,
        String state,          // "future" | "active" | "closed"
        String goal,
        LocalDateTime startDate,
        LocalDateTime endDate,
        LocalDateTime completeDate,
        int boardId,
        List<Issue> issues
) {
    public Sprint {
        issues = issues == null ? List.of() : List.copyOf(issues);
    }

    public boolean isActive() { return "active".equalsIgnoreCase(state); }

    public int totalIssues() { return issues.size(); }

    public int completedIssues() {
        return (int) issues.stream().filter(Issue::isCompleted).count();
    }

    public int inProgressIssues() {
        return (int) issues.stream().filter(Issue::isInProgress).count();
    }

    public int blockedIssues() {
        return (int) issues.stream()
                .filter(i -> i.status() != null
                          && i.status().toLowerCase().contains("blocked"))
                .count();
    }

    public int unassignedIssues() {
        return (int) issues.stream()
                .filter(i -> i.assignee() == null || i.assignee().isBlank())
                .filter(i -> !i.isCompleted())
                .count();
    }
}
```

## 1.4 pom.xml Changes

**Remove:**
```xml
<dependency>
    <groupId>org.apache.commons</groupId>
    <artifactId>commons-csv</artifactId>
    <version>1.11.0</version>
</dependency>
```

**Add inside `<dependencies>`:**
```xml
<!-- JIRA REST client -->
<dependency>
    <groupId>com.atlassian.jira</groupId>
    <artifactId>jira-rest-java-client-core</artifactId>
    <version>5.2.7</version>
</dependency>
<dependency>
    <groupId>com.atlassian.jira</groupId>
    <artifactId>jira-rest-java-client-api</artifactId>
    <version>5.2.7</version>
</dependency>
<dependency>
    <groupId>io.atlassian.fugue</groupId>
    <artifactId>fugue</artifactId>
    <version>5.0.0</version>
</dependency>

<!-- In-memory caching (version managed by Spring Boot BOM) -->
<dependency>
    <groupId>com.github.ben-manes.caffeine</groupId>
    <artifactId>caffeine</artifactId>
</dependency>

<!-- Jackson (already in starter-web; listed explicitly for clarity) -->
<dependency>
    <groupId>com.fasterxml.jackson.datatype</groupId>
    <artifactId>jackson-datatype-jsr310</artifactId>
</dependency>

<!-- WireMock for JIRA integration tests -->
<dependency>
    <groupId>org.wiremock</groupId>
    <artifactId>wiremock-standalone</artifactId>
    <version>3.5.4</version>
    <scope>test</scope>
</dependency>
```

**Add `<repositories>` block** (Atlassian artifacts are not on Maven Central):
```xml
<repositories>
    <repository>
        <id>atlassian-public</id>
        <url>https://packages.atlassian.com/maven/repository/public</url>
        <snapshots><enabled>false</enabled></snapshots>
    </repository>
</repositories>
```

## 1.5 Replace application.properties

Overwrite `src/main/resources/application.properties` completely:

```properties
spring.application.name=jira-sprint-ai-assistant
server.port=8080

# JIRA connection — set via environment variables; never commit real values
jira.base-url=${JIRA_BASE_URL:https://your-domain.atlassian.net}
jira.username=${JIRA_USERNAME:your-email@example.com}
jira.api-token=${JIRA_API_TOKEN:your-api-token-here}
jira.project-key=${JIRA_PROJECT_KEY:PROJ}
jira.board-id=${JIRA_BOARD_ID:1}
jira.max-results=${JIRA_MAX_RESULTS:100}
jira.cache-ttl-minutes=${JIRA_CACHE_TTL:5}

# Actuator
management.endpoints.web.exposure.include=health,info,metrics
management.endpoint.health.show-details=always

# Jackson — ISO-8601 dates
spring.jackson.serialization.write-dates-as-timestamps=false
spring.jackson.time-zone=UTC
```

## 1.6 Create JiraProperties

**Create:** `src/main/java/com/ablmvp/sprintassistant/config/JiraProperties.java`

```java
package com.ablmvp.sprintassistant.config;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties(prefix = "jira")
@Validated
public record JiraProperties(
        @NotBlank String baseUrl,
        @NotBlank String username,
        @NotBlank String apiToken,
        @NotBlank String projectKey,
        @NotNull @Min(1) Integer boardId,
        @NotNull @Min(1) Integer maxResults,
        @NotNull @Min(1) Integer cacheTtlMinutes
) {}
```

**Update `SprintAssistantApplication.java`** to add `@ConfigurationPropertiesScan`:

```java
package com.ablmvp.sprintassistant;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class SprintAssistantApplication {
    public static void main(String[] args) {
        SpringApplication.run(SprintAssistantApplication.class, args);
    }
}
```

## 1.7 Stub SprintApiController

Replace with a stub that compiles cleanly while real implementation is built in Phase 2:

```java
package com.ablmvp.sprintassistant.api;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.Map;

/** Stub controller — replaced in Phase 2. */
@RestController
@RequestMapping("/api/v1")
public class SprintApiController {

    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("status", "UP", "phase", "1-stub");
    }
}
```

## 1.8 Rewrite ApiExceptionHandler

```java
package com.ablmvp.sprintassistant.api;

import com.ablmvp.sprintassistant.api.dto.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(SprintNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorResponse handleNotFound(SprintNotFoundException ex) {
        return new ErrorResponse(HttpStatus.NOT_FOUND.value(), ex.getMessage());
    }

    @ExceptionHandler(InvalidSprintIdException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleBadRequest(InvalidSprintIdException ex) {
        return new ErrorResponse(HttpStatus.BAD_REQUEST.value(), "Invalid sprint ID");
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ErrorResponse handleGeneric(Exception ex) {
        return new ErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "Unexpected error: " + ex.getMessage());
    }
}
```

Ensure `ErrorResponse.java` has this shape:
```java
package com.ablmvp.sprintassistant.api.dto;
public record ErrorResponse(int status, String message) {}
```

## 1.9 Update SprintAssistantApplicationTests

```java
package com.ablmvp.sprintassistant;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(properties = {
        "jira.base-url=https://stub.atlassian.net",
        "jira.username=test@example.com",
        "jira.api-token=stub-token",
        "jira.project-key=STUB",
        "jira.board-id=1",
        "jira.max-results=50",
        "jira.cache-ttl-minutes=5"
})
class SprintAssistantApplicationTests {
    @Test
    void contextLoads() {}
}
```

If the JIRA client bean attempts a real HTTP connection at startup, add a
`@TestConfiguration` that provides a mock `JiraRestClient` for the test profile.

## 1.10 Add .env.example

Create `.env.example` in the repository root:

```bash
# Copy to .env and fill in real values. NEVER commit .env.
JIRA_BASE_URL=https://your-domain.atlassian.net
JIRA_USERNAME=your-email@example.com
JIRA_API_TOKEN=your-api-token-here
JIRA_PROJECT_KEY=PROJ
JIRA_BOARD_ID=1
JIRA_MAX_RESULTS=100
JIRA_CACHE_TTL=5
```

Add to `.gitignore`:
```
.env
*.env
```

## 1.11 Phase 1 Done Criteria

- `mvn clean test` exits 0.
- `SprintAssistantApplicationTests.contextLoads` passes.
- Zero references to `CsvDataLoader`, `LoadResult`, `DataSourceProperties`,
  `DatasetConfiguration`, or any CSV fixture remain in production code.
- `GET /api/v1/health` returns `{"status":"UP","phase":"1-stub"}`.
- No Spring XD CSV or derived row is present anywhere in the repository.

---

# PHASE 2 — Live JIRA Data Layer

**Owner:** Teammate 1
**Duration:** 4–5 days
**Goal:** Implement the JIRA API client, mappers, and data service so every endpoint
fetches real data from the configured board.

---

## 2.1 JIRA Client Factory

**Create:** `src/main/java/com/ablmvp/sprintassistant/jira/JiraClientConfig.java`

```java
package com.ablmvp.sprintassistant.jira;

import com.ablmvp.sprintassistant.config.JiraProperties;
import com.atlassian.jira.rest.client.api.JiraRestClient;
import com.atlassian.jira.rest.client.internal.async.AsynchronousJiraRestClientFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.net.URI;

@Configuration
public class JiraClientConfig {

    @Bean(destroyMethod = "close")
    public JiraRestClient jiraRestClient(JiraProperties props) {
        return new AsynchronousJiraRestClientFactory()
                .createWithBasicHttpAuthentication(
                        URI.create(props.baseUrl()),
                        props.username(),
                        props.apiToken()
                );
    }
}
```

## 2.2 JIRA Agile API Client

The standard `jira-rest-java-client` does not expose Agile sprint endpoints. Use
`java.net.http.HttpClient` directly.

**Create:** `src/main/java/com/ablmvp/sprintassistant/jira/JiraAgileApiClient.java`

```java
package com.ablmvp.sprintassistant.jira;

import com.ablmvp.sprintassistant.config.JiraProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

/**
 * Thin wrapper over the JIRA Agile REST API (board/sprint endpoints).
 * Uses Java 21 java.net.http.HttpClient — no extra dependency required.
 */
@Component
public class JiraAgileApiClient {

    private final JiraProperties props;
    private final HttpClient http;
    private final ObjectMapper mapper;
    private final String authHeader;

    public JiraAgileApiClient(JiraProperties props, ObjectMapper mapper) {
        this.props = props;
        this.mapper = mapper;
        this.http = HttpClient.newHttpClient();
        String credentials = props.username() + ":" + props.apiToken();
        this.authHeader = "Basic " + Base64.getEncoder()
                .encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
    }

    /** GET /rest/agile/1.0/board/{boardId}/sprint */
    public List<JsonNode> fetchSprints() throws Exception {
        String url = props.baseUrl()
                + "/rest/agile/1.0/board/" + props.boardId() + "/sprint"
                + "?maxResults=" + props.maxResults();
        JsonNode body = get(url);
        List<JsonNode> sprints = new ArrayList<>();
        for (JsonNode n : body.path("values")) sprints.add(n);
        return sprints;
    }

    /** GET /rest/agile/1.0/sprint/{sprintId}/issue */
    public List<JsonNode> fetchIssuesForSprint(String sprintId) throws Exception {
        String url = props.baseUrl()
                + "/rest/agile/1.0/sprint/" + sprintId + "/issue"
                + "?maxResults=" + props.maxResults()
                + "&fields=summary,status,issuetype,priority,assignee,reporter,"
                + "customfield_10016,customfield_10028,customfield_10020,"
                + "timetracking,created,updated,resolutiondate,labels";
        JsonNode body = get(url);
        List<JsonNode> issues = new ArrayList<>();
        for (JsonNode n : body.path("issues")) issues.add(n);
        return issues;
    }

    private JsonNode get(String url) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Authorization", authHeader)
                .header("Accept", "application/json")
                .GET()
                .build();
        HttpResponse<String> response = http.send(request,
                HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new JiraApiException("JIRA returned " + response.statusCode()
                    + " for " + url + ": " + response.body());
        }
        return mapper.readTree(response.body());
    }
}
```

**Create:** `src/main/java/com/ablmvp/sprintassistant/jira/JiraApiException.java`

```java
package com.ablmvp.sprintassistant.jira;

public class JiraApiException extends RuntimeException {
    public JiraApiException(String message) { super(message); }
    public JiraApiException(String message, Throwable cause) { super(message, cause); }
}
```

## 2.3 JiraIssueMapper

**Create:** `src/main/java/com/ablmvp/sprintassistant/jira/JiraIssueMapper.java`

```java
package com.ablmvp.sprintassistant.jira;

import com.ablmvp.sprintassistant.domain.Issue;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Maps a raw JIRA Agile API issue JSON node to a domain Issue.
 *
 * Story points: tries customfield_10016, customfield_10028, story_points in order.
 * Sprint: reads customfield_10020 array; takes the last (most recent) entry.
 */
@Component
public class JiraIssueMapper {

    private static final DateTimeFormatter JIRA_DT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSZ");

    public Issue map(JsonNode node) {
        JsonNode f = node.path("fields");
        return new Issue(
                text(node, "key"),
                text(node, "id"),
                text(f.path("summary")),
                f.path("issuetype").path("name").asText(null),
                f.path("status").path("name").asText(null),
                f.path("status").path("statusCategory").path("name").asText(null),
                f.path("priority").path("name").asText(null),
                displayName(f, "assignee"),
                displayName(f, "reporter"),
                storyPoints(f),
                timetracking(f, "originalEstimate"),
                timetracking(f, "remainingEstimate"),
                timetracking(f, "timeSpent"),
                dateTime(f, "created"),
                dateTime(f, "updated"),
                dateTime(f, "resolutiondate"),
                labels(f),
                sprintId(f),
                sprintName(f)
        );
    }

    private String text(JsonNode n, String field) {
        JsonNode v = n.path(field);
        return v.isTextual() ? v.asText() : null;
    }

    private String text(JsonNode n) {
        return n.isTextual() ? n.asText() : null;
    }

    private String displayName(JsonNode fields, String field) {
        JsonNode n = fields.path(field);
        if (n.isNull() || n.isMissingNode()) return null;
        return n.path("displayName").asText(null);
    }

    private BigDecimal storyPoints(JsonNode f) {
        for (String cf : new String[]{"customfield_10016","customfield_10028","story_points"}) {
            JsonNode n = f.path(cf);
            if (!n.isMissingNode() && !n.isNull()) {
                try { return new BigDecimal(n.asText()); } catch (Exception ignored) {}
            }
        }
        return null;
    }

    private BigDecimal timetracking(JsonNode f, String sub) {
        JsonNode tt = f.path("timetracking");
        if (tt.isMissingNode() || tt.isNull()) return null;
        JsonNode v = tt.path(sub);
        if (v.isMissingNode() || v.isNull()) return null;
        try { return new BigDecimal(v.asText()); } catch (Exception e) { return null; }
    }

    private LocalDateTime dateTime(JsonNode f, String field) {
        JsonNode n = f.path(field);
        if (n.isNull() || n.isMissingNode()) return null;
        try { return LocalDateTime.parse(n.asText(), JIRA_DT); } catch (Exception e) { return null; }
    }

    private List<String> labels(JsonNode f) {
        List<String> r = new ArrayList<>();
        for (JsonNode l : f.path("labels")) r.add(l.asText());
        return r;
    }

    private String sprintId(JsonNode f) {
        JsonNode sf = f.path("customfield_10020");
        if (!sf.isArray() || sf.isEmpty()) return null;
        JsonNode last = sf.get(sf.size() - 1);
        return last.path("id").isMissingNode() ? null : last.path("id").asText(null);
    }

    private String sprintName(JsonNode f) {
        JsonNode sf = f.path("customfield_10020");
        if (!sf.isArray() || sf.isEmpty()) return null;
        return sf.get(sf.size() - 1).path("name").asText(null);
    }
}
```

## 2.4 JiraSprintMapper

**Create:** `src/main/java/com/ablmvp/sprintassistant/jira/JiraSprintMapper.java`

```java
package com.ablmvp.sprintassistant.jira;

import com.ablmvp.sprintassistant.config.JiraProperties;
import com.ablmvp.sprintassistant.domain.Issue;
import com.ablmvp.sprintassistant.domain.Sprint;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Component
public class JiraSprintMapper {

    private static final DateTimeFormatter JIRA_DT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSZ");

    private final JiraProperties props;

    public JiraSprintMapper(JiraProperties props) { this.props = props; }

    public Sprint map(JsonNode node, List<Issue> issues) {
        return new Sprint(
                node.path("id").asText(null),
                node.path("name").asText(null),
                node.path("state").asText(null),
                node.path("goal").asText(null),
                dateTime(node, "startDate"),
                dateTime(node, "endDate"),
                dateTime(node, "completeDate"),
                props.boardId(),
                issues
        );
    }

    public Sprint mapBare(JsonNode node) { return map(node, List.of()); }

    private LocalDateTime dateTime(JsonNode n, String field) {
        JsonNode v = n.path(field);
        if (v.isNull() || v.isMissingNode()) return null;
        try { return LocalDateTime.parse(v.asText(), JIRA_DT); } catch (Exception e) { return null; }
    }
}
```

## 2.5 JiraDataService

**Create:** `src/main/java/com/ablmvp/sprintassistant/jira/JiraDataService.java`

```java
package com.ablmvp.sprintassistant.jira;

import com.ablmvp.sprintassistant.api.SprintNotFoundException;
import com.ablmvp.sprintassistant.config.JiraProperties;
import com.ablmvp.sprintassistant.domain.Issue;
import com.ablmvp.sprintassistant.domain.Sprint;
import com.fasterxml.jackson.databind.JsonNode;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * Central service for all JIRA data access.
 * Wraps JiraAgileApiClient with a Caffeine cache (TTL from jira.cache-ttl-minutes).
 * Call invalidateCache() to force a data refresh.
 */
@Service
public class JiraDataService {

    private static final Logger log = LoggerFactory.getLogger(JiraDataService.class);

    private final JiraAgileApiClient agileClient;
    private final JiraIssueMapper issueMapper;
    private final JiraSprintMapper sprintMapper;

    private final Cache<String, List<Sprint>> sprintListCache;
    private final Cache<String, Sprint> sprintDetailCache;

    public JiraDataService(JiraAgileApiClient agileClient,
                           JiraIssueMapper issueMapper,
                           JiraSprintMapper sprintMapper,
                           JiraProperties props) {
        this.agileClient = agileClient;
        this.issueMapper = issueMapper;
        this.sprintMapper = sprintMapper;
        long ttl = props.cacheTtlMinutes();
        this.sprintListCache = Caffeine.newBuilder()
                .expireAfterWrite(ttl, TimeUnit.MINUTES).maximumSize(5).build();
        this.sprintDetailCache = Caffeine.newBuilder()
                .expireAfterWrite(ttl, TimeUnit.MINUTES).maximumSize(50).build();
    }

    /** Returns all sprints (no issues attached — fast list for the dropdown). */
    public List<Sprint> listSprints() {
        return sprintListCache.get("all", key -> {
            log.info("Fetching sprint list from JIRA");
            try {
                return agileClient.fetchSprints().stream()
                        .map(sprintMapper::mapBare).toList();
            } catch (Exception e) {
                throw new JiraApiException("Failed to fetch sprints", e);
            }
        });
    }

    /** Returns a sprint with all its issues. Throws SprintNotFoundException if absent. */
    public Sprint getSprint(String sprintId) {
        return sprintDetailCache.get(sprintId, key -> {
            log.info("Fetching sprint {} with issues from JIRA", sprintId);
            try {
                Optional<JsonNode> sprintNode = agileClient.fetchSprints().stream()
                        .filter(n -> sprintId.equals(n.path("id").asText()))
                        .findFirst();
                if (sprintNode.isEmpty()) throw new SprintNotFoundException(sprintId);
                List<Issue> issues = agileClient.fetchIssuesForSprint(sprintId)
                        .stream().map(issueMapper::map).toList();
                return sprintMapper.map(sprintNode.get(), issues);
            } catch (SprintNotFoundException e) {
                throw e;
            } catch (Exception e) {
                throw new JiraApiException("Failed to fetch sprint " + sprintId, e);
            }
        });
    }

    /** Returns the first active sprint, or throws SprintNotFoundException. */
    public Sprint getActiveSprint() {
        return listSprints().stream()
                .filter(Sprint::isActive)
                .findFirst()
                .map(bare -> getSprint(bare.id()))
                .orElseThrow(() -> new SprintNotFoundException(
                        "No active sprint found on the configured board"));
    }

    /** Clears all caches. */
    public void invalidateCache() {
        sprintListCache.invalidateAll();
        sprintDetailCache.invalidateAll();
        log.info("JIRA data cache cleared");
    }
}
```

## 2.6 New DTOs

Create all of these in `src/main/java/com/ablmvp/sprintassistant/api/dto/`.

```java
// SprintSummaryResponse.java
package com.ablmvp.sprintassistant.api.dto;
import java.time.LocalDateTime;
public record SprintSummaryResponse(
        String id, String name, String state, String goal,
        LocalDateTime startDate, LocalDateTime endDate,
        int totalIssues, int completedIssues) {}

// SprintDetailResponse.java
package com.ablmvp.sprintassistant.api.dto;
import com.ablmvp.sprintassistant.domain.Issue;
import java.time.LocalDateTime; import java.util.List;
public record SprintDetailResponse(
        String id, String name, String state, String goal,
        LocalDateTime startDate, LocalDateTime endDate, LocalDateTime completeDate,
        int totalIssues, int completedIssues, int inProgressIssues, int blockedIssues,
        List<Issue> issues) {}

// SprintHealthResponse.java
package com.ablmvp.sprintassistant.api.dto;
import java.math.BigDecimal; import java.util.List;
public record SprintHealthResponse(
        String sprintId, String status, String statusReason,
        BigDecimal completionRate, int totalIssues, int completedIssues,
        int blockedIssues, List<String> recommendations) {}

// SprintAnalysisResponse.java
package com.ablmvp.sprintassistant.api.dto;
import java.math.BigDecimal; import java.util.List; import java.util.Map;
public record SprintAnalysisResponse(
        String sprintId, String sprintName, String state,
        int totalIssues, int completedIssues, int inProgressIssues,
        int blockedIssues, int unassignedActiveIssues,
        BigDecimal totalStoryPoints, BigDecimal completedStoryPoints,
        BigDecimal completionRate,
        Map<String, Integer> workloadByAssignee,
        List<String> identifiedRisks) {}

// SprintPredictionResponse.java
package com.ablmvp.sprintassistant.api.dto;
import java.util.List;
public record SprintPredictionResponse(
        String sprintId, double completionProbability, long daysRemaining,
        String trend, List<String> insights, List<String> bottlenecks,
        List<String> suggestedActions) {}

// RiskItemResponse.java
package com.ablmvp.sprintassistant.api.dto;
import java.util.List;
public record RiskItemResponse(
        String issueKey, String summary, String assignee, double riskScore,
        String riskLevel, List<String> factors, List<String> mitigations) {}

// AssignmentRecommendationResponse.java
package com.ablmvp.sprintassistant.api.dto;
public record AssignmentRecommendationResponse(
        String issueKey, String issueSummary, String recommendedAssignee,
        String reason, double confidence) {}
```

## 2.7 Rewrite SprintApiController

Replace the Phase 1 stub. Note that `SprintMetricsService` is a stub in Phase 2 — replace
it with the full implementation in Phase 3.

```java
package com.ablmvp.sprintassistant.api;

import com.ablmvp.sprintassistant.api.dto.*;
import com.ablmvp.sprintassistant.domain.Sprint;
import com.ablmvp.sprintassistant.jira.JiraDataService;
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

    public SprintApiController(JiraDataService jiraDataService,
                               SprintMetricsService metricsService) {
        this.jiraDataService = jiraDataService;
        this.metricsService = metricsService;
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
```

## 2.8 Integration Tests (WireMock)

**Create:** `src/test/java/com/ablmvp/sprintassistant/jira/JiraDataServiceTest.java`

Use WireMock to mock the JIRA Agile API. Test:
- `listSprints()` returns mapped Sprint records from a mocked board/sprint response.
- `getSprint(id)` returns a Sprint with issues from mocked responses.
- `getActiveSprint()` returns the sprint with `state = "active"`.
- `invalidateCache()` causes a re-fetch (verify with WireMock request count).
- `getSprint("unknown-id")` throws `SprintNotFoundException`.

**Create:** `src/test/java/com/ablmvp/sprintassistant/jira/JiraIssueMapperTest.java`

Test:
- Story points read from `customfield_10016`.
- Returns null story points when no custom field present.
- `statusCategory` correctly nested.
- `isCompleted()` true only when statusCategory is "Done".
- Labels parsed correctly.
- `sprintId`/`sprintName` from `customfield_10020` last array element.
- Null-safe for every optional field (no NPE on minimal JSON).

## 2.9 Phase 2 Done Criteria

- `mvn clean test` exits 0 with all WireMock tests passing.
- `GET /api/v1/sprints` returns a JSON array.
- `GET /api/v1/sprints/active` returns the active sprint with issues.
- `GET /api/v1/sprints/{id}` returns 404 for an unknown sprint.
- `POST /api/v1/sprints/refresh` returns 204.
- Swagger UI at `/swagger-ui.html` lists all endpoints.

---

# PHASE 3 — Metrics and AI Analysis

**Owner:** Teammate 2
**Duration:** 4–5 days
**Goal:** Implement the full metrics layer, health classification, sprint prediction, risk
assessment, and task-assignment recommendations. All logic is deterministic and rule-based
— no external AI API key is required.

---

## 3.1 SprintMetricsService

**Create:** `src/main/java/com/ablmvp/sprintassistant/metrics/SprintMetricsService.java`

```java
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
            if (i.assignee() != null && !i.assignee().isBlank() && !i.isCompleted())
                workload.merge(i.assignee(), 1, Integer::sum);
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
```

## 3.2 SprintAIAnalyzer

**Create:** `src/main/java/com/ablmvp/sprintassistant/metrics/SprintAIAnalyzer.java`

The three heuristics used for prediction (documented in code comments so they can be
explained at the viva):

**H1** — progress-pace ratio: (completed/total) divided by (elapsed/total_days). A value
above 1.0 means the team is ahead of a linear pace.

**H2** — blocked penalty: 5% per blocked issue, capped at 40%.

**H3** — unassigned penalty: 3% per unassigned active issue, capped at 20%.

```java
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
                score += 0.40; factors.add("Issue is blocked");
                mits.add("Escalate blocker in today's standup");
            }
            if (isHighPriorityNotStarted(issue)) {
                score += 0.30; factors.add("High-priority issue not yet started");
                mits.add("Start this issue today");
            }
            if (isStaleInProgress(issue, now)) {
                long d = ChronoUnit.DAYS.between(issue.updated(), now);
                score += 0.20; factors.add("In progress for " + d + " days without update");
                mits.add("Pair-program to unblock");
            }
            if (isLargeTask(issue)) {
                score += 0.15; factors.add("Story points > 8");
                mits.add("Split into smaller sub-tasks");
            }
            if (issue.assignee() == null || issue.assignee().isBlank()) {
                score += 0.10; factors.add("No assignee");
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

    private long daysRemaining(Sprint sprint) {
        if (sprint.endDate() == null) return 7;
        return Math.max(0, ChronoUnit.DAYS.between(LocalDateTime.now(), sprint.endDate()));
    }

    private long totalDays(Sprint sprint) {
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
```

## 3.3 Add AI Endpoints to SprintApiController

Add `SprintAIAnalyzer` to the constructor and add these three methods:

```java
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
```

## 3.4 Unit Tests

**`src/test/java/com/ablmvp/sprintassistant/metrics/SprintMetricsServiceTest.java`**

Cover: zero issues, all completed, mixed state, blocked issues. Each health band (HEALTHY,
WATCH, AT_RISK). Story-point sum ignoring nulls. Recommendations present when blocked > 0.

**`src/test/java/com/ablmvp/sprintassistant/metrics/SprintAIAnalyzerTest.java`**

Cover: high probability when sprint is ahead. Low probability when mostly blocked.
Risk HIGH for blocked+unassigned. Risk LOW for small assigned in-progress issue.
Recommendations only for unassigned issues. `daysRemaining = 0` when end date is past.

## 3.5 Phase 3 Done Criteria

- `mvn clean test` exits 0.
- All eight endpoints return correct data.
- `/health` returns `HEALTHY`, `WATCH`, or `AT_RISK` with an explanation string.
- `/prediction` returns probability between 0.05 and 0.98.
- `/risks` returns issues sorted by risk score descending.
- `/recommendations` returns only unassigned, non-Done issues.
- Swagger lists all eight endpoints with descriptions.

---

# PHASE 4 — React Frontend and CLI

**Owner:** Teammate 3
**Duration:** 4–5 days
**Goal:** Build the React dashboard and CLI tools.

---

## 4.1 Frontend Project Bootstrap

```bash
mkdir -p src/main/frontend
cd src/main/frontend
npm create vite@latest . -- --template react-ts
npm install axios react-router-dom recharts lucide-react
npm install -D tailwindcss postcss autoprefixer
npx tailwindcss init -p
```

**`vite.config.ts`:**
```ts
import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
export default defineConfig({
  plugins: [react()],
  build: { outDir: 'dist', emptyOutDir: true },
  server: { proxy: { '/api': 'http://localhost:8080' } },
})
```

## 4.2 Add Frontend Maven Build

Add to `pom.xml` inside `<build><plugins>`:

```xml
<plugin>
    <groupId>com.github.eirslett</groupId>
    <artifactId>frontend-maven-plugin</artifactId>
    <version>1.15.0</version>
    <configuration>
        <workingDirectory>src/main/frontend</workingDirectory>
        <installDirectory>target</installDirectory>
    </configuration>
    <executions>
        <execution>
            <id>install-node-and-npm</id>
            <goals><goal>install-node-and-npm</goal></goals>
            <configuration>
                <nodeVersion>v20.11.0</nodeVersion>
                <npmVersion>10.2.4</npmVersion>
            </configuration>
        </execution>
        <execution>
            <id>npm-install</id>
            <goals><goal>npm</goal></goals>
            <configuration><arguments>install</arguments></configuration>
        </execution>
        <execution>
            <id>npm-build</id>
            <goals><goal>npm</goal></goals>
            <configuration><arguments>run build</arguments></configuration>
            <phase>generate-resources</phase>
        </execution>
    </executions>
</plugin>
<plugin>
    <artifactId>maven-resources-plugin</artifactId>
    <executions>
        <execution>
            <id>copy-frontend</id>
            <phase>prepare-package</phase>
            <goals><goal>copy-resources</goal></goals>
            <configuration>
                <outputDirectory>target/classes/static</outputDirectory>
                <resources>
                    <resource><directory>src/main/frontend/dist</directory></resource>
                </resources>
            </configuration>
        </execution>
    </executions>
</plugin>
```

## 4.3 Frontend Structure

```
src/main/frontend/src/
├── main.tsx
├── App.tsx
├── api/
│   └── client.ts          — axios wrapper for all API calls
├── types/
│   └── sprint.ts          — TypeScript interfaces matching backend DTOs
├── components/
│   ├── SprintSelector.tsx  — dropdown of all sprints
│   ├── HealthBadge.tsx     — colour-coded HEALTHY/WATCH/AT_RISK badge
│   ├── MetricCard.tsx      — reusable stat card
│   ├── IssueTable.tsx      — paginated issue list
│   ├── CompletionChart.tsx — Recharts bar chart: completed vs remaining SP
│   ├── RiskList.tsx        — ordered risk items with expand/collapse
│   └── RecommendationList.tsx
└── pages/
    ├── Dashboard.tsx       — active sprint overview (default page)
    ├── SprintAnalysis.tsx  — full analysis for a selected sprint
    └── AIInsights.tsx      — prediction + risks + recommendations
```

## 4.4 TypeScript API Client

**`src/main/frontend/src/api/client.ts`:**
```ts
import axios from 'axios';

const api = axios.create({ baseURL: '/api/v1' });

export const sprintApi = {
  listSprints:        ()           => api.get('/sprints').then(r => r.data),
  getActiveSprint:    ()           => api.get('/sprints/active').then(r => r.data),
  getSprint:          (id: string) => api.get(`/sprints/${id}`).then(r => r.data),
  getAnalysis:        (id: string) => api.get(`/sprints/${id}/analysis`).then(r => r.data),
  getHealth:          (id: string) => api.get(`/sprints/${id}/health`).then(r => r.data),
  getPrediction:      (id: string) => api.get(`/sprints/${id}/prediction`).then(r => r.data),
  getRisks:           (id: string) => api.get(`/sprints/${id}/risks`).then(r => r.data),
  getRecommendations: (id: string) => api.get(`/sprints/${id}/recommendations`).then(r => r.data),
  refresh:            ()           => api.post('/sprints/refresh'),
};
```

## 4.5 Required Page Behaviours

**Dashboard page** shows:
- Active sprint name, state, and date range.
- Health badge (green = HEALTHY, amber = WATCH, red = AT_RISK).
- Four metric cards: Total Issues, Completed, In Progress, Blocked.
- Completion percentage bar filled proportionally.
- AI recommendations list (from `/health`).
- "Refresh" button that calls `POST /refresh` and reloads data.

**SprintAnalysis page** shows:
- Sprint selector dropdown from `GET /sprints`.
- Same four metric cards.
- Story-point bar chart (Recharts): completed vs remaining.
- Workload-by-assignee table (sorted by task count descending).
- Full issue table: Key, Summary, Status, Priority, Assignee, Story Points.
- Loading spinner and error state.

**AIInsights page** shows:
- Completion probability as large percentage with trend badge (AHEAD / ON_TRACK / BEHIND).
- Days remaining.
- Insights and suggested actions as bullet lists.
- Risk table sorted by risk level: columns Issue, Summary, Level badge, Factors, Mitigations.
- Assignment recommendations table: Issue, Recommended Assignee, Reason, Confidence %.

## 4.6 CLI Implementation

**Create:** `src/main/java/com/ablmvp/sprintassistant/cli/SprintCliRunner.java`

Activated only when `cli.enabled=true` is set (via `-Dcli.enabled=true` JVM arg), so the
normal web server startup is not affected.

```java
package com.ablmvp.sprintassistant.cli;

import com.ablmvp.sprintassistant.api.dto.SprintAnalysisResponse;
import com.ablmvp.sprintassistant.api.dto.SprintHealthResponse;
import com.ablmvp.sprintassistant.domain.Sprint;
import com.ablmvp.sprintassistant.jira.JiraDataService;
import com.ablmvp.sprintassistant.metrics.SprintMetricsService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import java.util.List;

@Component
@ConditionalOnProperty(name = "cli.enabled", havingValue = "true")
public class SprintCliRunner implements CommandLineRunner {

    private final JiraDataService jiraDataService;
    private final SprintMetricsService metricsService;

    public SprintCliRunner(JiraDataService jiraDataService,
                           SprintMetricsService metricsService) {
        this.jiraDataService = jiraDataService;
        this.metricsService = metricsService;
    }

    @Override
    public void run(String... args) {
        if (args.length == 0) { printUsage(); System.exit(0); }
        switch (args[0]) {
            case "list-sprints"   -> { doListSprints(); System.exit(0); }
            case "analyze-sprint" -> {
                if (args.length < 2) {
                    System.err.println("Error: provide a sprint ID.");
                    System.exit(1);
                }
                doAnalyzeSprint(args[1]);
                System.exit(0);
            }
            default -> { printUsage(); System.exit(1); }
        }
    }

    private void doListSprints() {
        List<Sprint> sprints = jiraDataService.listSprints();
        System.out.printf("%n%-10s  %-40s  %-8s  %5s%n", "ID","Name","State","Issues");
        System.out.println("-".repeat(70));
        sprints.forEach(s -> System.out.printf("%-10s  %-40s  %-8s  %5d%n",
                s.id(), s.name(), s.state(), s.totalIssues()));
        System.out.println("\nTotal: " + sprints.size() + " sprints");
    }

    private void doAnalyzeSprint(String sprintId) {
        Sprint sprint = jiraDataService.getSprint(sprintId);
        SprintAnalysisResponse a = metricsService.analyze(sprint);
        SprintHealthResponse h = metricsService.health(sprint);

        System.out.println("\n=== " + sprint.name() + " (" + sprint.state() + ") ===");
        System.out.println("ID: " + sprint.id());
        System.out.printf("Period: %s to %s%n", sprint.startDate(), sprint.endDate());
        System.out.println("\nIssues:");
        System.out.println("  Total       : " + a.totalIssues());
        System.out.println("  Completed   : " + a.completedIssues());
        System.out.println("  In Progress : " + a.inProgressIssues());
        System.out.println("  Blocked     : " + a.blockedIssues());
        System.out.println("  Unassigned  : " + a.unassignedActiveIssues());
        System.out.println("\nStory Points:");
        System.out.println("  Total       : " + a.totalStoryPoints());
        System.out.println("  Completed   : " + a.completedStoryPoints());
        System.out.println("  Completion  : " + a.completionRate() + "%");
        System.out.println("\nHealth: " + h.status());
        System.out.println("Reason: " + h.statusReason());
        if (!h.recommendations().isEmpty()) {
            System.out.println("\nRecommendations:");
            h.recommendations().forEach(r -> System.out.println("  * " + r));
        }
        if (!a.identifiedRisks().isEmpty()) {
            System.out.println("\nRisks:");
            a.identifiedRisks().forEach(r -> System.out.println("  ! " + r));
        }
    }

    private void printUsage() {
        System.out.println("""

            JIRA Sprint AI Assistant - CLI
            Usage (run with -Dcli.enabled=true):
              list-sprints                List all sprints on the board
              analyze-sprint <sprintId>   Full analysis of a specific sprint

            Examples:
              java -Dcli.enabled=true -jar sprint-assistant.jar list-sprints
              java -Dcli.enabled=true -jar sprint-assistant.jar analyze-sprint 42
            """);
    }
}
```

## 4.7 Phase 4 Done Criteria

- `mvn clean package` builds the full JAR with bundled React assets.
- `GET /` serves the React dashboard.
- Dashboard renders active sprint health with real JIRA data.
- Sprint selector switches sprints and all metrics update.
- AIInsights page shows prediction, risk table, and recommendations.
- `java -Dcli.enabled=true -jar target/sprint-assistant.jar list-sprints` prints a table.
- `java -Dcli.enabled=true -jar target/sprint-assistant.jar analyze-sprint <id>` prints
  analysis matching the dashboard.

---

# PHASE 5 — DevOps: Docker, Kubernetes, Jenkins

**Owner:** Teammate 4
**Duration:** 4–5 days

---

## 5.1 Dockerfile

Create `Dockerfile` in the repository root:

```dockerfile
# Stage 1: Build
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -B -q
COPY src ./src
RUN mvn clean package -DskipTests -B -q

# Stage 2: Runtime
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN addgroup -g 1001 spring && adduser -u 1001 -G spring -s /bin/sh -D spring
COPY --from=build /app/target/sprint-assistant-*.jar app.jar
RUN chown spring:spring app.jar
USER spring:spring
EXPOSE 8080
HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
    CMD wget -qO- http://localhost:8080/actuator/health || exit 1
ENV JAVA_OPTS="-Xmx512m -Xms256m"
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
```

Create `.dockerignore`:
```
target/
node_modules/
.git/
*.md
Jenkinsfile
kubernetes/
.env
```

## 5.2 Kubernetes Manifests

Create directory `kubernetes/` in the repository root.

**`kubernetes/namespace.yaml`**
```yaml
apiVersion: v1
kind: Namespace
metadata:
  name: sprint-assistant
```

**`kubernetes/secret.yaml`** — fill in and apply manually; do not commit real values:
```yaml
apiVersion: v1
kind: Secret
metadata:
  name: jira-credentials
  namespace: sprint-assistant
type: Opaque
stringData:
  JIRA_BASE_URL: "https://your-domain.atlassian.net"
  JIRA_USERNAME: "your-email@example.com"
  JIRA_API_TOKEN: "your-token"
  JIRA_PROJECT_KEY: "PROJ"
  JIRA_BOARD_ID: "1"
```

**`kubernetes/deployment.yaml`**
```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: sprint-assistant
  namespace: sprint-assistant
  labels:
    app: sprint-assistant
spec:
  replicas: 1
  selector:
    matchLabels:
      app: sprint-assistant
  template:
    metadata:
      labels:
        app: sprint-assistant
    spec:
      containers:
      - name: sprint-assistant
        image: sprint-assistant:latest
        imagePullPolicy: IfNotPresent
        ports:
        - containerPort: 8080
        envFrom:
        - secretRef:
            name: jira-credentials
        resources:
          requests:
            memory: "512Mi"
            cpu: "250m"
          limits:
            memory: "1Gi"
            cpu: "1000m"
        livenessProbe:
          httpGet:
            path: /actuator/health
            port: 8080
          initialDelaySeconds: 60
          periodSeconds: 15
        readinessProbe:
          httpGet:
            path: /actuator/health
            port: 8080
          initialDelaySeconds: 30
          periodSeconds: 10
```

**`kubernetes/service.yaml`**
```yaml
apiVersion: v1
kind: Service
metadata:
  name: sprint-assistant
  namespace: sprint-assistant
spec:
  type: NodePort
  selector:
    app: sprint-assistant
  ports:
  - port: 8080
    targetPort: 8080
    nodePort: 30080
```

## 5.3 Jenkinsfile

Create `Jenkinsfile` in the repository root:

```groovy
pipeline {
    agent any
    environment {
        IMAGE_NAME = 'sprint-assistant'
        IMAGE_TAG  = "${BUILD_NUMBER}"
        K8S_NS     = 'sprint-assistant'
    }
    stages {
        stage('Checkout') {
            steps {
                checkout scm
                script {
                    env.GIT_SHORT = sh(
                        script: 'git rev-parse --short HEAD',
                        returnStdout: true).trim()
                }
            }
        }
        stage('Build & Test') {
            steps { sh 'mvn clean test -B' }
            post { always { junit '**/target/surefire-reports/*.xml' } }
        }
        stage('Package') {
            steps { sh 'mvn package -DskipTests -B' }
        }
        stage('Docker Build') {
            steps {
                script {
                    docker.build("${IMAGE_NAME}:${IMAGE_TAG}")
                    docker.build("${IMAGE_NAME}:latest")
                }
            }
        }
        stage('Deploy to Kubernetes') {
            steps {
                sh "minikube image load ${IMAGE_NAME}:${IMAGE_TAG}"
                sh """
                    kubectl apply -f kubernetes/namespace.yaml
                    kubectl apply -f kubernetes/secret.yaml -n ${K8S_NS}
                    kubectl apply -f kubernetes/deployment.yaml -n ${K8S_NS}
                    kubectl apply -f kubernetes/service.yaml -n ${K8S_NS}
                    kubectl set image deployment/sprint-assistant \
                        sprint-assistant=${IMAGE_NAME}:${IMAGE_TAG} -n ${K8S_NS}
                    kubectl rollout status deployment/sprint-assistant \
                        -n ${K8S_NS} --timeout=5m
                """
            }
        }
        stage('Smoke Test') {
            steps {
                sh """
                    POD=\$(kubectl get pod -l app=sprint-assistant \
                          -n ${K8S_NS} -o jsonpath='{.items[0].metadata.name}')
                    kubectl exec \$POD -n ${K8S_NS} -- \
                        wget -qO- http://localhost:8080/actuator/health
                """
            }
        }
    }
    post {
        success { echo "Build ${IMAGE_TAG} deployed successfully." }
        failure { echo "Pipeline failed — check logs." }
        always  { cleanWs() }
    }
}
```

## 5.4 Local Deployment Script

Create `scripts/deploy-local.sh`:

```bash
#!/usr/bin/env bash
set -euo pipefail

echo "=== Building application ==="
mvn clean package -DskipTests -B

echo "=== Building Docker image ==="
docker build -t sprint-assistant:latest .

echo "=== Loading image into Minikube ==="
minikube image load sprint-assistant:latest

echo "=== Applying Kubernetes manifests ==="
kubectl apply -f kubernetes/namespace.yaml
kubectl apply -f kubernetes/secret.yaml -n sprint-assistant
kubectl apply -f kubernetes/deployment.yaml -n sprint-assistant
kubectl apply -f kubernetes/service.yaml -n sprint-assistant

echo "=== Waiting for rollout ==="
kubectl rollout status deployment/sprint-assistant \
    -n sprint-assistant --timeout=5m

echo ""
echo "=== Done ==="
echo "URL: http://$(minikube ip):30080"
```

## 5.5 Phase 5 Done Criteria

- `docker build` and `docker run --env-file .env sprint-assistant:latest` work.
- `GET /actuator/health` returns `{"status":"UP"}` from Docker.
- `./scripts/deploy-local.sh` completes on a Minikube machine.
- `kubectl get pods -n sprint-assistant` shows one Running pod.
- `http://$(minikube ip):30080` loads the React dashboard.
- Jenkins completes all five stages (Checkout, Build & Test, Package, Docker Build,
  Deploy to Kubernetes, Smoke Test) on `main`.

---

# PHASE 6 — Documentation and Demo

**Owner:** Teammate 4 (coordinates); all teammates contribute their sections.
**Duration:** 3–4 days

---

## 6.1 README.md

Replace the root `README.md`. It must contain these ten sections:

1. What it does (one paragraph).
2. Prerequisites (Java 21, Maven 3.9, Docker, Minikube, JIRA Cloud account).
3. Local quick-start (five numbered commands from clone to open browser).
4. JIRA setup (how to create an API token, find the board ID).
5. Environment variables table (`JIRA_BASE_URL`, `JIRA_USERNAME`, `JIRA_API_TOKEN`,
   `JIRA_PROJECT_KEY`, `JIRA_BOARD_ID`, `JIRA_MAX_RESULTS`, `JIRA_CACHE_TTL`).
6. API reference table (method, path, one-line description for all eight endpoints).
7. CLI reference with example output for `list-sprints` and `analyze-sprint`.
8. Docker run command.
9. Kubernetes deployment via `./scripts/deploy-local.sh`.
10. Team table (name, USN, assigned phase).

## 6.2 Technical Documents

**`docs/SRS.md`** — Software Requirements Specification covering:
Introduction, System Scope, User Roles, Functional Requirements FR-01 through FR-15,
Non-Functional Requirements (performance, security, usability), Constraints, Assumptions.

**`docs/SDS.md`** — Software Design Specification covering:
Architecture Overview, Package Structure, Domain Model, API Design (request/response
shapes for all eight endpoints), Data Flow, Security Design (API token handling, no
secrets in code or logs), Technology Choices with rationale.

**`docs/UML_DIAGRAMS.md`** — Four diagrams as Mermaid blocks:

1. **Use Case Diagram** — actors: User, CI/CD System; use cases: view dashboard, select
   sprint, view AI insights, run CLI, deploy via Jenkins.
2. **Activity Diagram** — flow from browser open through sprint selection to analysis.
3. **Sequence Diagram** — Browser → React → Spring Boot → JIRA API → response.
4. **Deployment Diagram** — Jenkins → Docker image → Minikube pod → NodePort → browser.

## 6.3 Demo Script

Create `docs/DEMO_SCRIPT.md` with the following structure:

```
PRE-DEMO CHECKLIST
  - Minikube running
  - Pod in Running state
  - Browser tabs open: Dashboard, Swagger UI, Jenkins
  - Terminal ready for CLI demo
  - JIRA board open in another tab

DEMO STEPS (15 minutes)

1. Introduction (2 min)
   - State the problem: teams lack a quick view of sprint health.
   - Show architecture: JIRA API -> Spring Boot -> React.
   - Point to JIRA board with live data.

2. Dashboard (3 min)
   - Open http://<minikube-ip>:30080
   - Show health badge and explain thresholds (>=80% = HEALTHY etc.)
   - Show four metric cards; explain each.
   - Click Refresh; show data reloading from JIRA.

3. Sprint Analysis (3 min)
   - Open Sprint Analysis; select a sprint from the dropdown.
   - Show story-point chart: completed vs remaining.
   - Show workload table; explain load distribution.

4. AI Insights (4 min)
   - Open AI Insights.
   - Show completion probability; explain the three heuristics
     (H1 progress pace, H2 blocked penalty, H3 unassigned penalty).
   - Expand a HIGH-risk issue; explain its factors.
   - Show assignment recommendations; explain the least-load algorithm.

5. CLI (1 min)
   java -Dcli.enabled=true -jar sprint-assistant.jar list-sprints
   java -Dcli.enabled=true -jar sprint-assistant.jar analyze-sprint <id>
   Show output matches dashboard numbers.

6. DevOps (2 min)
   - Open Jenkins; show five pipeline stages.
   - kubectl get pods -n sprint-assistant
   - Point to image tag matching the Jenkins build number.
```

## 6.4 Test Evidence

Create `docs/TEST_EVIDENCE.md` containing:
- Screenshot of `mvn clean test` output showing all tests pass and count.
- Table: test class, number of tests, what each covers.
- Screenshot of Swagger UI with all endpoints expanded.
- Screenshot of Kubernetes pod in Running state.

## 6.5 Contribution Log

Create `docs/CONTRIBUTION_LOG.md` with one table per teammate:

| Task | Branch | PR | Status |
|------|--------|----|--------|
| Phase 1 cleanup | feat/PROJ-1-remove-csv-layer | #1 | Merged |
...

## 6.6 Phase 6 Done Criteria

- README covers all ten sections.
- SRS, SDS, and UML documents are non-trivial (SRS has >=15 FRs, SDS has all eight endpoint
  shapes).
- Demo script rehearsed end-to-end at least once by the full team.
- Test evidence collected and committed.
- Contribution log shows at least one PR per teammate.
- Hard copy of ABL report ready for 16 December 2026.

---

# Final File Inventory (after all phases complete)

```
Root
├── .dockerignore
├── .env.example
├── .gitignore
├── Dockerfile
├── FINAL_IMPLEMENTATION_PLAN.md
├── Jenkinsfile
├── README.md
├── pom.xml

data/
└── README.md

docs/
├── CONTRIBUTION_LOG.md
├── DEMO_SCRIPT.md
├── SDS.md
├── SRS.md
├── TEST_EVIDENCE.md
└── UML_DIAGRAMS.md

kubernetes/
├── deployment.yaml
├── namespace.yaml
├── secret.yaml
└── service.yaml

scripts/
└── deploy-local.sh

src/main/frontend/              (React app — Vite + TypeScript)

src/main/java/com/ablmvp/sprintassistant/
├── SprintAssistantApplication.java
├── api/
│   ├── ApiExceptionHandler.java
│   ├── InvalidSprintIdException.java
│   ├── SprintApiController.java
│   ├── SprintNotFoundException.java
│   └── dto/
│       ├── AssignmentRecommendationResponse.java
│       ├── ErrorResponse.java
│       ├── RiskItemResponse.java
│       ├── SprintAnalysisResponse.java
│       ├── SprintDetailResponse.java
│       ├── SprintHealthResponse.java
│       ├── SprintPredictionResponse.java
│       └── SprintSummaryResponse.java
├── cli/
│   └── SprintCliRunner.java
├── config/
│   └── JiraProperties.java
├── domain/
│   ├── Issue.java
│   └── Sprint.java
├── jira/
│   ├── JiraAgileApiClient.java
│   ├── JiraApiException.java
│   ├── JiraClientConfig.java
│   ├── JiraDataService.java
│   ├── JiraIssueMapper.java
│   └── JiraSprintMapper.java
└── metrics/
    ├── SprintAIAnalyzer.java
    └── SprintMetricsService.java

src/main/resources/
└── application.properties

src/test/java/com/ablmvp/sprintassistant/
├── SprintAssistantApplicationTests.java
├── jira/
│   ├── JiraDataServiceTest.java
│   └── JiraIssueMapperTest.java
└── metrics/
    ├── SprintAIAnalyzerTest.java
    └── SprintMetricsServiceTest.java
```

---

# Team Work Allocation

| Phase | Owner | Key Deliverables |
|-------|-------|-----------------|
| 1 — Cleanup | Teammate 1 | Delete CSV layer, add JiraProperties, rewrite domain models, app compiles |
| 2 — JIRA data | Teammate 1 | JiraAgileApiClient, JiraDataService, mappers, WireMock tests, all 6 base endpoints live |
| 3 — Metrics/AI | Teammate 2 | SprintMetricsService, SprintAIAnalyzer, 3 AI endpoints, unit tests |
| 4 — Frontend/CLI | Teammate 3 | React dashboard (3 pages), API client, SprintCliRunner, Maven frontend build |
| 5 — DevOps | Teammate 4 | Dockerfile, Kubernetes manifests, Jenkinsfile, deploy-local.sh |
| 6 — Docs/Demo | Teammate 4 (all) | README, SRS, SDS, UML, demo script, test evidence, contribution log |

---

# Key Decisions

**No external AI API required.** The "AI" in `SprintAIAnalyzer` is deterministic heuristics.
Every formula is documented in code comments. If the team later wants to add an LLM, the
`SprintAIAnalyzer` service is the right place to add an optional call that falls back to the
existing heuristics when no API key is set.

**JIRA API token, not OAuth.** For a classroom project a personal API token is correct. Set
it in `.env` locally and in the Kubernetes secret for deployment. Never commit it.

**Single Minikube node.** One replica is sufficient for the demo.

**Frontend proxies to backend.** The `vite.config.ts` proxy forwards `/api` calls to
`localhost:8080` during development. In production the React build is served as static files
from Spring Boot — no proxy needed.

**Cache TTL default is 5 minutes.** Keeps the demo responsive without hitting JIRA rate
limits. The refresh button bypasses the cache on demand.

**Branch naming:** `feat/PROJ-<n>-short-description` for features, `fix/PROJ-<n>-...` for
fixes. Every PR must reference a JIRA issue key.

---

# Risk Register

| Risk | Likelihood | Mitigation |
|------|-----------|------------|
| JIRA API rate limit hit during demo | Low | 5-min cache; demo board has ~10 issues |
| Atlassian Maven repository unreachable in CI | Medium | Use `mvn dependency:go-offline`; cache in Jenkins |
| Minikube fails to pull image | Medium | Use `minikube image load` instead of a registry |
| JIRA board has zero active sprint | Low | Add a test sprint before the demo |
| Frontend build fails on CI agent | Low | Pin node/npm versions in Maven plugin config |
| Sprint end-date null in prediction | Low | `daysRemaining()` returns 7 when `endDate` is null |

---

*End of FINAL_IMPLEMENTATION_PLAN.md*
