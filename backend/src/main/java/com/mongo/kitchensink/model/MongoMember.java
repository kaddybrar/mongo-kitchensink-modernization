package com.mongo.kitchensink.model;

import jakarta.validation.constraints.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

/**
 * Document class representing a member in MongoDB.
 * Contains member information and validation constraints.
 */
@Document(collection = "members")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Member Details")
public class MongoMember implements IMember {
    @Id
    private String id;

    @NotBlank(message = "Name is required")
    @Schema(
        description = "Member's name",
        example = "John Doe",
        required = true
    )
    @Indexed
    private String name;

    @NotBlank(message = "Email is required")
    @Email(message = "Email should be valid")
    @Indexed(unique = true)
    @Schema(
        description = "Member's email address",
        example = "john.doe@example.com",
        required = true
    )
    private String email;

    @Pattern(regexp = "^\\+?[0-9]{10,15}$", message = "Phone number should be valid")
    @Schema(
        description = "Member's phone number in international format",
        example = "+12345678901"
    )
    private String phoneNumber;

    @CreatedDate
    @JsonIgnore
    private LocalDateTime createdAt;

    @LastModifiedDate
    @JsonIgnore
    private LocalDateTime updatedAt;
    
    /**
     * Sets the ID of this member.
     * Ensures MongoDB IDs are always string representations of JPA IDs.
     * 
     * @param id the ID to set
     */
    @Override
    public void setId(Object id) {
        if (id == null) {
            this.id = null;
            return;
        }
        
        // Always convert to String to ensure consistency
        this.id = id.toString();
    }
    
    /**
     * Converts this MongoDB Member to a JPA Member.
     * 
     * @return a JPA Member with the same data
     */
    @Override
    public JpaMember toJpaMember() {
        JpaMember member = JpaMember.builder()
                .name(this.name)
                .email(this.email)
                .phoneNumber(this.phoneNumber)
                .build();
        
        if (this.id != null) {
            try {
                member.setId(Long.parseLong(this.id));
            } catch (NumberFormatException e) {
                // For new records or non-numeric IDs
            }
        }
        
        return member;
    }
    
    /**
     * Converts this MongoDB Member to itself.
     * 
     * @return this member
     */
    @Override
    public MongoMember toMongoMember() {
        return this;
    }
} 