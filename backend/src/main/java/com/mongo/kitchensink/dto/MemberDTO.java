package com.mongo.kitchensink.dto;

import com.mongo.kitchensink.model.IMember;
import com.mongo.kitchensink.model.JpaMember;
import com.mongo.kitchensink.model.MongoMember;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Data Transfer Object for Member information.
 * Used for API requests and responses.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Member Data Transfer Object")
public class MemberDTO {

    @Schema(description = "Member ID", example = "1")
    private String id;

    @NotBlank(message = "Name is required")
    @Schema(description = "Member's name", example = "John Doe", required = true)
    private String name;

    @NotBlank(message = "Email is required")
    @Email(message = "Email should be valid")
    @Schema(description = "Member's email address", example = "john.doe@example.com", required = true)
    private String email;

    @Pattern(regexp = "^\\+?[0-9]{10,15}$", message = "Phone number should be valid")
    @Schema(description = "Member's phone number in international format", example = "+12345678901")
    private String phoneNumber;

    /**
     * Converts a domain model to DTO.
     *
     * @param member the domain model
     * @return the DTO
     */
    public static MemberDTO fromMember(IMember member) {
        if (member == null) {
            return null;
        }
        
        return MemberDTO.builder()
                .id(member.getId().toString())
                .name(member.getName())
                .email(member.getEmail())
                .phoneNumber(member.getPhoneNumber())
                .build();
    }

    /**
     * Converts this DTO to a JPA Member.
     *
     * @return the JPA Member
     */
    public JpaMember toJpaMember() {
        JpaMember member = new JpaMember();
        if (id != null && !id.isEmpty()) {
            try {
                member.setId(Long.parseLong(id));
            } catch (NumberFormatException e) {
                // Log the error but continue with null ID (for new members)
                System.err.println("Could not parse ID as Long: " + id);
            }
        }
        member.setName(name);
        member.setEmail(email);
        member.setPhoneNumber(phoneNumber);
        return member;
    }

    /**
     * Converts this DTO to a MongoDB Member.
     *
     * @return the MongoDB Member
     */
    public MongoMember toMongoMember() {
        MongoMember member = new MongoMember();
        member.setId(id);
        member.setName(name);
        member.setEmail(email);
        member.setPhoneNumber(phoneNumber);
        return member;
    }
} 