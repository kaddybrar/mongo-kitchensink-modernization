package com.mongo.kitchensink.model;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the MongoMember model validation constraints.
 * These tests verify that the validation annotations work as expected.
 */
public class MongoMemberValidationTest {

    private Validator validator;
    private MongoMember validMember;

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();

        validMember = MongoMember.builder()
                .name("Test User")
                .email("test@example.com")
                .phoneNumber("+12345678901")
                .build();
    }

    @Test
    void validMember_NoViolations() {
        // Act
        Set<ConstraintViolation<MongoMember>> violations = validator.validate(validMember);

        // Assert
        assertTrue(violations.isEmpty());
    }

    @Test
    void nullName_HasViolation() {
        // Arrange
        validMember.setName(null);

        // Act
        Set<ConstraintViolation<MongoMember>> violations = validator.validate(validMember);

        // Assert
        assertEquals(1, violations.size());
        assertEquals("Name is required", violations.iterator().next().getMessage());
    }

    // Add more tests similar to JpaMemberValidationTest
    // ...
} 