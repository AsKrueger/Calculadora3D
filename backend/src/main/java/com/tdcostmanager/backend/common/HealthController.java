package com.tdcostmanager.backend.common;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class HealthController {

    @GetMapping("/health")
    public HealthResponse getHealth() {
        return new HealthResponse("UP");
    }

    public record HealthResponse(String status) {}
}
