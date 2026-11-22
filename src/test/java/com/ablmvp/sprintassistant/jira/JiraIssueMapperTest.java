package com.ablmvp.sprintassistant.jira;

import com.ablmvp.sprintassistant.domain.Issue;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JiraIssueMapperTest {

    private JiraIssueMapper mapper;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        mapper = new JiraIssueMapper();
        objectMapper = new ObjectMapper();
    }

    @Test
    void storyPoints_readFromCustomfield10016() throws Exception {
        String json = """
                {
                  "key": "PROJ-1",
                  "id": "10001",
                  "fields": {
                    "summary": "Test issue",
                    "issuetype": {"name": "Story"},
                    "status": {"name": "In Progress", "statusCategory": {"name": "In Progress"}},
                    "priority": {"name": "Medium"},
                    "customfield_10016": 5,
                    "labels": []
                  }
                }
                """;
        Issue issue = mapper.map(objectMapper.readTree(json));
        assertThat(issue.storyPoints()).isEqualByComparingTo("5");
    }

    @Test
    void storyPoints_nullWhenNoCustomFieldPresent() throws Exception {
        String json = """
                {
                  "key": "PROJ-2",
                  "id": "10002",
                  "fields": {
                    "summary": "No SP",
                    "issuetype": {"name": "Task"},
                    "status": {"name": "To Do", "statusCategory": {"name": "To Do"}},
                    "labels": []
                  }
                }
                """;
        Issue issue = mapper.map(objectMapper.readTree(json));
        assertThat(issue.storyPoints()).isNull();
    }

    @Test
    void statusCategory_correctlyNested() throws Exception {
        String json = """
                {
                  "key": "PROJ-3",
                  "id": "10003",
                  "fields": {
                    "summary": "Done issue",
                    "status": {"name": "Done", "statusCategory": {"name": "Done"}},
                    "labels": []
                  }
                }
                """;
        Issue issue = mapper.map(objectMapper.readTree(json));
        assertThat(issue.statusCategory()).isEqualTo("Done");
    }

    @Test
    void isCompleted_trueWhenStatusCategoryIsDone() throws Exception {
        String json = """
                {
                  "key": "PROJ-4",
                  "id": "10004",
                  "fields": {
                    "summary": "Completed",
                    "status": {"name": "Done", "statusCategory": {"name": "Done"}},
                    "labels": []
                  }
                }
                """;
        Issue issue = mapper.map(objectMapper.readTree(json));
        assertThat(issue.isCompleted()).isTrue();
    }

    @Test
    void isCompleted_falseWhenStatusCategoryIsNotDone() throws Exception {
        String json = """
                {
                  "key": "PROJ-5",
                  "id": "10005",
                  "fields": {
                    "summary": "In progress",
                    "status": {"name": "In Progress", "statusCategory": {"name": "In Progress"}},
                    "labels": []
                  }
                }
                """;
        Issue issue = mapper.map(objectMapper.readTree(json));
        assertThat(issue.isCompleted()).isFalse();
    }

    @Test
    void labels_parsedCorrectly() throws Exception {
        String json = """
                {
                  "key": "PROJ-6",
                  "id": "10006",
                  "fields": {
                    "summary": "Labelled",
                    "status": {"name": "To Do", "statusCategory": {"name": "To Do"}},
                    "labels": ["backend", "urgent"]
                  }
                }
                """;
        Issue issue = mapper.map(objectMapper.readTree(json));
        assertThat(issue.labels()).containsExactly("backend", "urgent");
    }

    @Test
    void sprintIdAndName_fromCustomfield10020LastElement() throws Exception {
        String json = """
                {
                  "key": "PROJ-7",
                  "id": "10007",
                  "fields": {
                    "summary": "Sprint issue",
                    "status": {"name": "To Do", "statusCategory": {"name": "To Do"}},
                    "labels": [],
                    "customfield_10020": [
                      {"id": "1", "name": "Sprint 1"},
                      {"id": "2", "name": "Sprint 2"}
                    ]
                  }
                }
                """;
        Issue issue = mapper.map(objectMapper.readTree(json));
        assertThat(issue.sprintId()).isEqualTo("2");
        assertThat(issue.sprintName()).isEqualTo("Sprint 2");
    }

    @Test
    void nullSafe_forEveryOptionalField() throws Exception {
        // Minimal JSON — only required structural keys present
        String json = """
                {
                  "key": "PROJ-8",
                  "id": "10008",
                  "fields": {
                    "summary": "Minimal"
                  }
                }
                """;
        Issue issue = mapper.map(objectMapper.readTree(json));
        assertThat(issue.key()).isEqualTo("PROJ-8");
        assertThat(issue.storyPoints()).isNull();
        assertThat(issue.assignee()).isNull();
        assertThat(issue.priority()).isNull();
        assertThat(issue.labels()).isEmpty();
        assertThat(issue.sprintId()).isNull();
    }
}
