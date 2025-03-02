package com.mongo.kitchensink.util;

import com.mongo.kitchensink.model.Member;

/**
 * Factory class for creating test data.
 * Provides consistent test objects across all test classes.
 */
public class TestDataFactory {

    /**
     * Creates a valid member for testing.
     * 
     * @return a valid member instance
     */
    public static Member createValidMember() {
        return Member.builder()
                .name("Test User")
                .email("test@example.com")
                .phoneNumber("+12345678901")
                .build();
    }
    
    /**
     * Creates a valid member with custom email.
     * 
     * @param email the email to use
     * @return a valid member instance with the specified email
     */
    public static Member createMemberWithEmail(String email) {
        return Member.builder()
                .name("Test User")
                .email(email)
                .phoneNumber("+12345678901")
                .build();
    }
    
    /**
     * Creates a member with the specified properties.
     * 
     * @param name the name to use
     * @param email the email to use
     * @param phoneNumber the phone number to use
     * @return a member instance with the specified properties
     */
    public static Member createMember(String name, String email, String phoneNumber) {
        return Member.builder()
                .name(name)
                .email(email)
                .phoneNumber(phoneNumber)
                .build();
    }
} 