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
        for (JsonNode n : body.path("values")) {
            sprints.add(n);
        }
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
        for (JsonNode n : body.path("issues")) {
            issues.add(n);
        }
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
