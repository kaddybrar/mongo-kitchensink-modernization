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
 * Unit tests for the Member model validation constraints.
 * These tests verify that the validation annotations work as expected.
 */
public class MemberValidationTest {

    private Validator validator;
    private Member validMember;

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();

        validMember = Member.builder()
                .name("Test User")
                .email("test@example.com")
                .phoneNumber("+12345678901")
                .build();
    }

    @Test
    void validMember_NoViolations() {
        // Act
        Set<ConstraintViolation<Member>> violations = validator.validate(validMember);

        // Assert
        assertTrue(violations.isEmpty());
    }

    @Test
    void nullName_HasViolation() {
        // Arrange
        validMember.setName(null);

        // Act
        Set<ConstraintViolation<Member>> violations = validator.validate(validMember);

        // Assert
        assertEquals(1, violations.size());
        assertEquals("Name is required", violations.iterator().next().getMessage());
    }

    @Test
    void emptyName_HasViolation() {
        // Arrange
        validMember.setName("");

        // Act
        Set<ConstraintViolation<Member>> violations = validator.validate(validMember);

        // Assert
        assertEquals(1, violations.size());
        assertEquals("Name is required", violations.iterator().next().getMessage());
    }

    @Test
    void nullEmail_HasViolation() {
        // Arrange
        validMember.setEmail(null);

        // Act
        Set<ConstraintViolation<Member>> violations = validator.validate(validMember);

        // Assert
        assertEquals(1, violations.size());
        assertEquals("Email is required", violations.iterator().next().getMessage());
    }

    @Test
    void invalidEmail_HasViolation() {
        // Arrange
        validMember.setEmail("invalid-email");

        // Act
        Set<ConstraintViolation<Member>> violations = validator.validate(validMember);

        // Assert
        assertEquals(1, violations.size());
        assertEquals("Email should be valid", violations.iterator().next().getMessage());
    }

    @Test
    void invalidPhoneNumber_HasViolation() {
        // Arrange
        validMember.setPhoneNumber("invalid-phone");

        // Act
        Set<ConstraintViolation<Member>> violations = validator.validate(validMember);

        // Assert
        assertEquals(1, violations.size());
        assertEquals("Phone number should be valid", violations.iterator().next().getMessage());
    }

    @Test
    void multipleInvalidFields_HasMultipleViolations() {
        // Arrange
        Member invalidMember = Member.builder()
                .name("")
                .email("invalid-email")
                .phoneNumber("invalid-phone")
                .build();

        // Act
        Set<ConstraintViolation<Member>> violations = validator.validate(invalidMember);

        // Assert
        assertEquals(3, violations.size());
    }
} 