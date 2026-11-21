package com.ablmvp.sprintassistant;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class SprintAssistantApplication {

    public static void main(String[] args) {
        SpringApplication.run(SprintAssistantApplication.class, args);
    }
}
