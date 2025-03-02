package com.mongo.kitchensink.base;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Base class for integration tests.
 * Provides common configuration for all integration tests.
 */
@SpringBootTest
@ActiveProfiles("test")
public abstract class BaseIntegrationTest {
    // Common integration test setup can be added here
} 