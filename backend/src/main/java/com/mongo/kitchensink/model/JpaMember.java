package com.mongo.kitchensink.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import com.fasterxml.jackson.annotation.JsonIgnore;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

/**
 * JPA Entity class representing a member in a relational database.
 * Contains member information and validation constraints.
 */
@Entity
@Table(name = "member")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Member Details")
public class JpaMember implements IMember {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Name is required")
    @Schema(
        description = "Member's name",
        example = "John Doe",
        required = true
    )
    private String name;

    @NotBlank(message = "Email is required")
    @Email(message = "Email should be valid")
    @Column(unique = true)
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

    
    @CreationTimestamp
    @Column(updatable = false)
    @JsonIgnore
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @JsonIgnore
    private LocalDateTime updatedAt;
    
    /**
     * Sets the ID of this member.
     * 
     * @param id the ID to set
     */
    @Override
    public void setId(Object id) {
        if (id instanceof Long) {
            this.id = (Long) id;
        } else if (id instanceof String) {
            try {
                this.id = Long.parseLong((String) id);
            } catch (NumberFormatException e) {
                // Handle or log the error
            }
        }
    }
    
    /**
     * Converts this JPA Member to itself.
     * 
     * @return this member
     */
    @Override
    public JpaMember toJpaMember() {
        return this;
    }
    
    /**
     * Converts this JPA Member to a MongoDB Member.
     * 
     * @return a MongoDB Member with the same data
     */
    @Override
    public MongoMember toMongoMember() {
        MongoMember mongoMember = new MongoMember();
        mongoMember.setId(id != null ? id.toString() : null);
        mongoMember.setName(name);
        mongoMember.setEmail(email);
        mongoMember.setPhoneNumber(phoneNumber);
        mongoMember.setCreatedAt(createdAt);
        mongoMember.setUpdatedAt(updatedAt);
        return mongoMember;
    }
} 