package com.mongo.kitchensink.integration;

import com.mongo.kitchensink.dto.MemberDTO;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for Dual-Write Member operations.
 */
@TestPropertySource(properties = {
    "app.database.type=jpa",
    "app.dual-write.enabled=true",
    "app.migration.strategy=dual-write"
})
public class DualWriteMemberIntegrationTest extends BaseIntegrationTest {
    @Test
    void createMember_ValidInput_WritesToBothDatabases() throws Exception {
        // Arrange
        MemberDTO memberDTO = MemberDTO.builder()
                .name("John Doe")
                .email("john@example.com")
                .phoneNumber("+12345678901")
                .build();

        // Act
        MvcResult result = mockMvc.perform(post("/api/v1/members")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(memberDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("John Doe"))
                .andReturn();

        // Extract the ID from the response
        MemberDTO createdMember = objectMapper.readValue(
                result.getResponse().getContentAsString(), MemberDTO.class);
        String id = createdMember.getId();

        // Track IDs for cleanup
        testDataManager.trackMongoId(id);
        testDataManager.trackJpaId(Long.parseLong(id));

        // Assert - Verify data in both databases
        verifyMemberExistsInBothDatabases(id, "John Doe", "john@example.com");
    }

    @Test
    void updateMember_ValidInput_UpdatesBothDatabases() throws Exception {
        // Arrange - Create a member
        MemberDTO createdMember = createTestMember();

        // Prepare update
        MemberDTO updatedMemberDTO = MemberDTO.builder()
                .id(createdMember.getId())
                .name("Updated Name")
                .email("updated@example.com")
                .phoneNumber("+19876543210")
                .build();

        // Act - Update the member
        mockMvc.perform(put("/api/v1/members/" + createdMember.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updatedMemberDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated Name"));

        // Assert - Verify data in both databases
        verifyMemberExistsInBothDatabases(createdMember.getId(), "Updated Name", "updated@example.com");
    }

    @Test
    void deleteMember_ExistingId_DeletesFromBothDatabases() throws Exception {
        // Arrange - Create a member
        MemberDTO createdMember = createTestMember();
        long jpaCount = jpaMemberRepository.count();
        long mongoCount = mongoMemberRepository.count();
        // Verify it exists in both databases
        verifyMemberExistsInBothDatabases(createdMember.getId(), "Test User", "test@example.com");

        // Act - Delete the member
        mockMvc.perform(delete("/api/v1/members/" + createdMember.getId()))
                .andExpect(status().isNoContent());

        // Assert - Verify it's deleted from both databases
        assert jpaMemberRepository.count() == jpaCount - 1;
        assert mongoMemberRepository.count() == mongoCount - 1;
    }

    @Test
    void getAllMembers_ReturnsAllMembersFromPrimaryDatabase() throws Exception {
        // Arrange - Create members
        createTestMember("John Doe", "john@example.com", "+12345678901");
        createTestMember("Jane Doe", "jane@example.com", "+19876543210");

        // Act & Assert
        mockMvc.perform(get("/api/v1/members"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].name", hasItems("John Doe", "Jane Doe")));

        // Verify both databases have the same data
        assert jpaMemberRepository.count() == mongoMemberRepository.count();
    }

    @Test
    void searchMembers_MatchingName_ReturnsMembersFromPrimaryDatabase() throws Exception {
        // Arrange
        createTestMember("John Dove", "johnDove@example.com", "+12345678901");
        createTestMember("Jane Dove", "janeDove@example.com", "+19876543210");
        createTestMember("Alice Smith", "aliceSmith@example.com", "+13456789012");

        // Act & Assert - Search for "Dove"
        mockMvc.perform(get("/api/v1/members/search?name=Dove"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[*].name", hasItems("John Dove", "Jane Dove")));
    }
} 