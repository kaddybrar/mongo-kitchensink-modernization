package com.mongo.kitchensink.util;

import com.mongo.kitchensink.model.JpaMember;
import com.mongo.kitchensink.model.MongoMember;

/**
 * Factory class for creating test data objects.
 * This class provides methods to create valid and invalid test objects for unit tests.
 */
public class TestDataFactory {

    /**
     * Creates a valid JpaMember instance for testing.
     * 
     * @return a valid JpaMember instance
     */
    public static JpaMember createValidJpaMember() {
        return JpaMember.builder()
                .name("Test User")
                .email("test@example.com")
                .phoneNumber("+12345678901")
                .build();
    }
    
    /**
     * Creates a valid MongoMember instance for testing.
     * 
     * @return a valid MongoMember instance
     */
    public static MongoMember createValidMongoMember() {
        return MongoMember.builder()
                .name("Test User")
                .email("test@example.com")
                .phoneNumber("+12345678901")
                .build();
    }
    
    /**
     * Creates a JpaMember instance with an invalid email for testing validation.
     * 
     * @return a JpaMember instance with an invalid email
     */
    public static JpaMember createJpaMemberWithInvalidEmail() {
        return JpaMember.builder()
                .name("Test User")
                .email("invalid-email")
                .phoneNumber("+12345678901")
                .build();
    }
    
    /**
     * Creates a MongoMember instance with an invalid email for testing validation.
     * 
     * @return a MongoMember instance with an invalid email
     */
    public static MongoMember createMongoMemberWithInvalidEmail() {
        return MongoMember.builder()
                .name("Test User")
                .email("invalid-email")
                .phoneNumber("+12345678901")
                .build();
    }
    
    /**
     * Creates a JpaMember instance with an invalid phone number for testing validation.
     * 
     * @return a JpaMember instance with an invalid phone number
     */
    public static JpaMember createJpaMemberWithInvalidPhone() {
        return JpaMember.builder()
                .name("Test User")
                .email("test@example.com")
                .phoneNumber("123456") // Invalid format
                .build();
    }
    
    /**
     * Creates a MongoMember instance with an invalid phone number for testing validation.
     * 
     * @return a MongoMember instance with an invalid phone number
     */
    public static MongoMember createMongoMemberWithInvalidPhone() {
        return MongoMember.builder()
                .name("Test User")
                .email("test@example.com")
                .phoneNumber("123456") // Invalid format
                .build();
    }
    
    /**
     * Creates a JpaMember instance with missing required fields for testing validation.
     * 
     * @return a JpaMember instance with missing required fields
     */
    public static JpaMember createJpaMemberWithMissingFields() {
        return JpaMember.builder()
                .email("test@example.com")
                // Missing name and phone number
                .build();
    }
    
    /**
     * Creates a MongoMember instance with missing required fields for testing validation.
     * 
     * @return a MongoMember instance with missing required fields
     */
    public static MongoMember createMongoMemberWithMissingFields() {
        return MongoMember.builder()
                .email("test@example.com")
                // Missing name and phone number
                .build();
    }
    
    /**
     * Creates a JpaMember instance with a specific ID.
     * 
     * @param id the ID to set
     * @return a JpaMember instance with the specified ID
     */
    public static JpaMember createJpaMemberWithId(Long id) {
        JpaMember member = createValidJpaMember();
        member.setId(id);
        return member;
    }
    
    /**
     * Creates a MongoMember instance with a specific ID.
     * 
     * @param id the ID to set
     * @return a MongoMember instance with the specified ID
     */
    public static MongoMember createMongoMemberWithId(String id) {
        MongoMember member = createValidMongoMember();
        member.setId(id);
        return member;
    }
} 