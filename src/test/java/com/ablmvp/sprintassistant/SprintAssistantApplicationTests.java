package com.ablmvp.sprintassistant;

import com.ablmvp.sprintassistant.jira.JiraAgileApiClient;
import com.atlassian.jira.rest.client.api.JiraRestClient;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(properties = {
        "jira.base-url=https://stub.atlassian.net",
        "jira.username=test@example.com",
        "jira.api-token=stub-token",
        "jira.project-key=STUB",
        "jira.board-id=1",
        "jira.max-results=50",
        "jira.cache-ttl-minutes=5",
        "spring.main.allow-bean-definition-overriding=true"
})
class SprintAssistantApplicationTests {

    // Replace the real JIRA HTTP client beans so no outbound connection is attempted
    @MockBean
    JiraRestClient jiraRestClient;

    @MockBean
    JiraAgileApiClient jiraAgileApiClient;

    @Test
    void contextLoads() {
    }
}
