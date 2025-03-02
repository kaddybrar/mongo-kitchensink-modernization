package com.mongo.kitchensink.model;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Interface representing a member entity.
 * Provides common methods for member data access regardless of persistence mechanism.
 */
@Schema(description = "Member Details")
public interface IMember {
    
    /**
     * Gets the member's ID.
     * 
     * @return the member ID
     */
    Object getId();
    
    /**
     * Sets the member's ID.
     * 
     * @param id the member ID to set
     */
    void setId(Object id);
    
    /**
     * Gets the member's name.
     * 
     * @return the member name
     */
    String getName();
    
    /**
     * Sets the member's name.
     * 
     * @param name the name to set
     */
    void setName(String name);
    
    /**
     * Gets the member's email.
     * 
     * @return the member email
     */
    String getEmail();
    
    /**
     * Sets the member's email.
     * 
     * @param email the email to set
     */
    void setEmail(String email);
    
    /**
     * Gets the member's phone number.
     * 
     * @return the phone number
     */
    String getPhoneNumber();
    
    /**
     * Sets the member's phone number.
     * 
     * @param phoneNumber the phone number to set
     */
    void setPhoneNumber(String phoneNumber);
    
    /**
     * Converts this member to a JPA Member entity.
     * 
     * @return a JPA Member entity
     */
    JpaMember toJpaMember();
    
    /**
     * Converts this member to a MongoDB Member document.
     * 
     * @return a MongoDB Member document
     */
    MongoMember toMongoMember();
} 