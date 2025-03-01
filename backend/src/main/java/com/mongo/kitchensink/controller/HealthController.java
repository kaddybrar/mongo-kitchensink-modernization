package com.mongo.kitchensink.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller for health check endpoints.
 * Provides basic health monitoring functionality for the application.
 */
@RestController
@RequestMapping("/api/health")
public class HealthController {

    /**
     * Basic health check endpoint.
     *
     * @return a simple status message indicating the service is running
     */
    @GetMapping
    public String healthCheck() {
        return "Service is running";
    }
} 