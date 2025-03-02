package com.mongo.kitchensink.controller;

import com.mongo.kitchensink.base.BaseControllerTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Unit tests for the HealthController class.
 * These tests verify the behavior of the health check endpoint.
 */
public class HealthControllerTest extends BaseControllerTest {

    @InjectMocks
    private HealthController healthController;

    @BeforeEach
    void setUp() {
        setupMockMvc(healthController);
    }

    @Test
    void healthCheck_ReturnsServiceRunningMessage() throws Exception {
        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(content().string("Service is running"));
    }
} 