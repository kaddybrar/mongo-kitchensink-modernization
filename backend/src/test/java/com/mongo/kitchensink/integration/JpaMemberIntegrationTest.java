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
 * Integration tests for JPA Member operations.
 */
@TestPropertySource(properties = {
    "app.database.type=jpa",
    "app.dual-write.enabled=false"
})
public class JpaMemberIntegrationTest extends BaseIntegrationTest {
    @Test
    void createMember_ValidInput_ReturnsCreatedMember() throws Exception {
        // Arrange
        MemberDTO memberDTO = MemberDTO.builder()
                .name("John Doe")
                .email("john@example.com")
                .phoneNumber("+12345678901")
                .build();
        long jpaCount=jpaMemberRepository.count();
        // Act & Assert
        MvcResult result = mockMvc.perform(post("/api/v1/members")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(memberDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("John Doe"))
                .andExpect(jsonPath("$.email").value("john@example.com"))
                .andExpect(jsonPath("$.phoneNumber").value("+12345678901"))
                .andReturn();

        // Extract and track ID
        MemberDTO createdMember = objectMapper.readValue(
                result.getResponse().getContentAsString(), MemberDTO.class);
        testDataManager.trackJpaId(Long.parseLong(createdMember.getId()));

        // Verify data in database
        assert jpaMemberRepository.count() == jpaCount+1;
        assert jpaMemberRepository.findByEmail("john@example.com").isPresent();
    }

    @Test
    void createMember_DuplicateEmail_ReturnsConflict() throws Exception {
        // Arrange
        createTestMember("John Doe", "john@example.com", "+12345678901");
        long jpaCount=jpaMemberRepository.count();
        MemberDTO duplicateMemberDTO = MemberDTO.builder()
                .name("Jane Doe")
                .email("john@example.com") // Same email
                .phoneNumber("+19876543210")
                .build();

        // Act & Assert
        mockMvc.perform(post("/api/v1/members")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(duplicateMemberDTO)))
                .andExpect(status().isConflict());

        // Verify only one member exists
        assert jpaMemberRepository.count() == jpaCount;
    }

    @Test
    void getAllMembers_ReturnsAllMembers() throws Exception {
        // Arrange
        createTestMember("John Doe", "john@example.com", "+12345678901");
        createTestMember("Jane Doe", "jane@example.com", "+19876543210");

        // Act & Assert
        mockMvc.perform(get("/api/v1/members"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].name", hasItems("John Doe", "Jane Doe")))
                .andExpect(jsonPath("$[*].email", hasItems("john@example.com", "jane@example.com")));
    }

    @Test
    void getMemberById_ExistingId_ReturnsMember() throws Exception {
        // Arrange
        MemberDTO createdMember = createTestMember();

        // Act & Assert
        mockMvc.perform(get("/api/v1/members/" + createdMember.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(createdMember.getId()))
                .andExpect(jsonPath("$.name").value("Test User"))
                .andExpect(jsonPath("$.email").value("test@example.com"));
    }

    @Test
    void getMemberById_NonExistingId_ReturnsNotFound() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/v1/members/999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateMember_ValidInput_ReturnsUpdatedMember() throws Exception {
        // Arrange
        MemberDTO createdMember = createTestMember();

        MemberDTO updatedMemberDTO = MemberDTO.builder()
                .id(createdMember.getId())
                .name("Updated Name")
                .email("updated@example.com")
                .phoneNumber("+19876543210")
                .build();

        // Act & Assert
        mockMvc.perform(put("/api/v1/members/" + createdMember.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updatedMemberDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(createdMember.getId()))
                .andExpect(jsonPath("$.name").value("Updated Name"))
                .andExpect(jsonPath("$.email").value("updated@example.com"))
                .andExpect(jsonPath("$.phoneNumber").value("+19876543210"));

        // Verify data in database
        assert jpaMemberRepository.findById(Long.parseLong(createdMember.getId())).get().getName().equals("Updated Name");
    }

    @Test
    void deleteMember_ExistingId_DeletesMember() throws Exception {
        // Arrange
        MemberDTO createdMember = createTestMember();
        long jpaCount=jpaMemberRepository.count();
        // Act & Assert
        mockMvc.perform(delete("/api/v1/members/" + createdMember.getId()))
                .andExpect(status().isNoContent());

        // Verify member is deleted
        assert jpaMemberRepository.count() == jpaCount-1;
    }

    @Test
    void searchMembers_MatchingName_ReturnsMembers() throws Exception {
        // Arrange
        createTestMember("Johny Dove", "john@example.com", "+12345678901");
        createTestMember("Jane Dove", "jane@example.com", "+19876543210");
        createTestMember("Alice Smith", "alice@example.com", "+13456789012");

        // Act & Assert - Search for "Dove"
        mockMvc.perform(get("/api/v1/members/search?name=Dove"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[*].name", containsInAnyOrder("Johny Dove", "Jane Dove")));

        // Act & Assert - Search for "Johny"
        mockMvc.perform(get("/api/v1/members/search?name=Johny"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].name").value("Johny Dove"));
    }
} 