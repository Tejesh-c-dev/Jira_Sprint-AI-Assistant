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

    public JiraSprintMapper(JiraProperties props) {
        this.props = props;
    }

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

    public Sprint mapBare(JsonNode node) {
        return map(node, List.of());
    }

    private LocalDateTime dateTime(JsonNode n, String field) {
        JsonNode v = n.path(field);
        if (v.isNull() || v.isMissingNode()) return null;
        try {
            return LocalDateTime.parse(v.asText(), JIRA_DT);
        } catch (Exception e) {
            return null;
        }
    }
}
