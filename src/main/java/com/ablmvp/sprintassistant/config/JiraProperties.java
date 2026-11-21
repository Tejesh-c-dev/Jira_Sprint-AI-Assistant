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
        @NotNull @Min(1) Integer cacheTtlMinutes) {
}
