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
