package com.mongo.kitchensink;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Simple test to verify JaCoCo integration.
 * This test will always pass but ensures that JaCoCo generates coverage data.
 */
public class JacocoVerificationTest {

    @Test
    void verifyJacocoIntegration() {
        // This method will be executed and should appear in coverage reports
        boolean result = performCoveredOperation();
        assertTrue(result);
    }
    
    private boolean performCoveredOperation() {
        // This method should show up as covered in the JaCoCo report
        return true;
    }
} 