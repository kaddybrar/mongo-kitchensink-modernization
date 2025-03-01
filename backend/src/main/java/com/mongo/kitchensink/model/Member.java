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
    @JsonIgnore
    private Long id;

    @NotBlank(message = "Name is required")
    @Schema(
        description = "Member's name",
        example = "John Doe",
        required = true
    )
    private String name;

    @Email(message = "Invalid email format")
    @NotBlank(message = "Email is required")
    @Column(unique = true)
    @Schema(
        description = "Member's email address",
        example = "john.doe@example.com",
        required = true
    )
    private String email;

    @Pattern(regexp = "^\\+?[1-9][0-9]{7,14}$", message = "Invalid phone number")
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