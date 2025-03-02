package com.mongo.kitchensink.base;

import org.junit.jupiter.api.BeforeEach;
import org.mockito.MockitoAnnotations;

/**
 * Base class for unit tests.
 * Provides common setup for all unit tests.
 */
public abstract class BaseUnitTest {

    @BeforeEach
    void baseSetUp() {
        // Simplify the mock initialization
        MockitoAnnotations.openMocks(this);
    }
} 