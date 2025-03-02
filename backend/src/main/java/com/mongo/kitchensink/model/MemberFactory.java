package com.mongo.kitchensink.model;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Factory for creating member entities based on the configured database type.
 */
@Component
public class MemberFactory {
    
    @Value("${app.database.type:jpa}")
    private String databaseType;
    
    /**
     * Creates a new empty member of the appropriate type.
     * 
     * @return a new member instance
     */
    public IMember createMember() {
        if ("mongo".equalsIgnoreCase(databaseType)) {
            return new MongoMember();
        } else {
            return new JpaMember();
        }
    }
    
    /**
     * Creates a new member with the provided data.
     * 
     * @param name the member's name
     * @param email the member's email
     * @param phoneNumber the member's phone number
     * @return a new member instance with the provided data
     */
    public IMember createMember(String name, String email, String phoneNumber) {
        if ("mongo".equalsIgnoreCase(databaseType)) {
            return MongoMember.builder()
                    .name(name)
                    .email(email)
                    .phoneNumber(phoneNumber)
                    .build();
        } else {
            return JpaMember.builder()
                    .name(name)
                    .email(email)
                    .phoneNumber(phoneNumber)
                    .build();
        }
    }
    
    /**
     * Converts a member to the appropriate type based on the current database configuration.
     * 
     * @param member the member to convert
     * @return the converted member
     */
    public IMember convertMember(IMember member) {
        if ("mongo".equalsIgnoreCase(databaseType)) {
            return member.toMongoMember();
        } else {
            return member.toJpaMember();
        }
    }
} 