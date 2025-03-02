// src/main/java/com/mongo/kitchensink/model/Member.java
package com.mongo.kitchensink.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

/**
 * Entity class representing a member.
 * Contains member information and validation constraints.
 */
@Entity
@Table(name = "members")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
@Schema(description = "Member Details")
public class Member {
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

    @CreatedDate
    @JsonIgnore
    private LocalDateTime createdAt;

    @LastModifiedDate
    @JsonIgnore
    private LocalDateTime updatedAt;
}