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
