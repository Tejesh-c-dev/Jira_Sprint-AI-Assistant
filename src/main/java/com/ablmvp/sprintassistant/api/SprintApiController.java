package com.ablmvp.sprintassistant.api;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/** Stub controller — replaced in Phase 2. */
@RestController
@RequestMapping("/api/v1")
public class SprintApiController {

    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("status", "UP", "phase", "1-stub");
    }
}
