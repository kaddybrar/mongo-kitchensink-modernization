package com.mongo.kitchensink.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller for health check endpoints.
 * Provides basic health monitoring functionality for the application.
 */
@RestController
@RequestMapping("/api/v1/health")
public class HealthController {

    /**
     * Simple health check endpoint.
     *
     * @return a message indicating the service is running
     */
    @GetMapping
    public String healthCheck() {
        return "Service is running";
    }
} 