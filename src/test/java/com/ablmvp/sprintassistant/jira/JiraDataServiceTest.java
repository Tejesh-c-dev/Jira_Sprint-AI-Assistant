package com.ablmvp.sprintassistant.jira;

import com.ablmvp.sprintassistant.api.SprintNotFoundException;
import com.ablmvp.sprintassistant.config.JiraProperties;
import com.ablmvp.sprintassistant.domain.Sprint;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JiraDataServiceTest {

    private WireMockServer wireMock;
    private JiraDataService service;

    @BeforeEach
    void setUp() {
        wireMock = new WireMockServer(WireMockConfiguration.wireMockConfig().dynamicPort());
        wireMock.start();

        JiraProperties props = new JiraProperties(
                "http://localhost:" + wireMock.port(),
                "user@example.com",
                "token",
                "PROJ",
                1,
                100,
                5
        );
        ObjectMapper objectMapper = new ObjectMapper();
        JiraAgileApiClient agileClient = new JiraAgileApiClient(props, objectMapper);
        JiraIssueMapper issueMapper = new JiraIssueMapper();
        JiraSprintMapper sprintMapper = new JiraSprintMapper(props);
        service = new JiraDataService(agileClient, issueMapper, sprintMapper, props);
    }

    @AfterEach
    void tearDown() {
        wireMock.stop();
    }

    @Test
    void listSprints_returnsMappedSprints() {
        wireMock.stubFor(get(urlPathEqualTo("/rest/agile/1.0/board/1/sprint"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(sprintsJson())));

        List<Sprint> sprints = service.listSprints();

        assertThat(sprints).hasSize(2);
        assertThat(sprints.get(0).id()).isEqualTo("10");
        assertThat(sprints.get(0).name()).isEqualTo("Sprint 1");
        assertThat(sprints.get(1).state()).isEqualTo("active");
    }

    @Test
    void getSprint_returnsSprintWithIssues() {
        wireMock.stubFor(get(urlPathEqualTo("/rest/agile/1.0/board/1/sprint"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(sprintsJson())));
        wireMock.stubFor(get(urlPathEqualTo("/rest/agile/1.0/sprint/11/issue"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(issuesJson())));

        Sprint sprint = service.getSprint("11");

        assertThat(sprint.id()).isEqualTo("11");
        assertThat(sprint.state()).isEqualTo("active");
        assertThat(sprint.issues()).hasSize(1);
        assertThat(sprint.issues().get(0).key()).isEqualTo("PROJ-1");
    }

    @Test
    void getActiveSprint_returnsActiveSprintWithIssues() {
        wireMock.stubFor(get(urlPathEqualTo("/rest/agile/1.0/board/1/sprint"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(sprintsJson())));
        wireMock.stubFor(get(urlPathEqualTo("/rest/agile/1.0/sprint/11/issue"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(issuesJson())));

        Sprint active = service.getActiveSprint();

        assertThat(active.isActive()).isTrue();
        assertThat(active.id()).isEqualTo("11");
    }

    @Test
    void getSprint_unknownId_throwsSprintNotFoundException() {
        wireMock.stubFor(get(urlPathEqualTo("/rest/agile/1.0/board/1/sprint"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(sprintsJson())));

        assertThatThrownBy(() -> service.getSprint("999"))
                .isInstanceOf(SprintNotFoundException.class);
    }

    @Test
    void invalidateCache_causesCacheRefetch() {
        wireMock.stubFor(get(urlPathEqualTo("/rest/agile/1.0/board/1/sprint"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(sprintsJson())));

        service.listSprints(); // first call
        service.invalidateCache();
        service.listSprints(); // second call after invalidation

        wireMock.verify(2, getRequestedFor(urlPathEqualTo("/rest/agile/1.0/board/1/sprint")));
    }

    // ---- fixture JSON helpers ----

    private String sprintsJson() {
        return """
                {
                  "values": [
                    {"id": "10", "name": "Sprint 1", "state": "closed", "goal": ""},
                    {"id": "11", "name": "Sprint 2", "state": "active", "goal": "Finish features"}
                  ]
                }
                """;
    }

    private String issuesJson() {
        return """
                {
                  "issues": [
                    {
                      "key": "PROJ-1",
                      "id": "20001",
                      "fields": {
                        "summary": "Implement login",
                        "issuetype": {"name": "Story"},
                        "status": {"name": "In Progress", "statusCategory": {"name": "In Progress"}},
                        "priority": {"name": "High"},
                        "customfield_10016": 3,
                        "labels": []
                      }
                    }
                  ]
                }
                """;
    }
}
