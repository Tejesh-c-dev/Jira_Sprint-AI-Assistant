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
        for (String cf : new String[]{"customfield_10016", "customfield_10028", "story_points"}) {
            JsonNode n = f.path(cf);
            if (!n.isMissingNode() && !n.isNull()) {
                try {
                    return new BigDecimal(n.asText());
                } catch (Exception ignored) {
                }
            }
        }
        return null;
    }

    private BigDecimal timetracking(JsonNode f, String sub) {
        JsonNode tt = f.path("timetracking");
        if (tt.isMissingNode() || tt.isNull()) return null;
        JsonNode v = tt.path(sub);
        if (v.isMissingNode() || v.isNull()) return null;
        try {
            return new BigDecimal(v.asText());
        } catch (Exception e) {
            return null;
        }
    }

    private LocalDateTime dateTime(JsonNode f, String field) {
        JsonNode n = f.path(field);
        if (n.isNull() || n.isMissingNode()) return null;
        try {
            return LocalDateTime.parse(n.asText(), JIRA_DT);
        } catch (Exception e) {
            return null;
        }
    }

    private List<String> labels(JsonNode f) {
        List<String> r = new ArrayList<>();
        for (JsonNode l : f.path("labels")) {
            r.add(l.asText());
        }
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
