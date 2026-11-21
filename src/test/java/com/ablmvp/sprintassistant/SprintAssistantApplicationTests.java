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
    void contextLoads() {
    }
}
