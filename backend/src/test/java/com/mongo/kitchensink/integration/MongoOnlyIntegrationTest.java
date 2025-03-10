package com.mongo.kitchensink.integration;

import com.mongo.kitchensink.dto.MemberDTO;
import com.mongo.kitchensink.model.MongoMember;
import com.mongo.kitchensink.service.JpaMemberService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for MongoDB-only operations after full migration.
 * This test simulates the scenario where the application has fully migrated to MongoDB
 * and no longer uses JPA/PostgreSQL.
 */
@SpringBootTest(properties = {
    "app.database.type=mongo",
    "app.dual-write.enabled=false",
    "app.migration.strategy=direct",
    "app.database.read.source=mongo"
})
@TestPropertySource(properties = {
    "spring.data.jpa.repositories.bootstrap-mode=default"
})
@MockBean(JpaMemberService.class)
public class MongoOnlyIntegrationTest extends BaseIntegrationTest {
    private static final Logger logger = LoggerFactory.getLogger(MongoOnlyIntegrationTest.class);

    @Test
    void createAndRetrieveMember_MongoOnly_Success() throws Exception {
        // Arrange - Create a new member
        MemberDTO memberDTO = MemberDTO.builder()
                .name("MongoDB User")
                .email("mongo@example.com")
                .phoneNumber("+15551234567")
                .build();

        // Act - Create the member
        mockMvc.perform(post("/api/v1/members")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(memberDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("MongoDB User"))
                .andExpect(jsonPath("$.email").value("mongo@example.com"));

        // Assert - Verify the member exists in MongoDB
        assert mongoMemberRepository.findByEmail("mongo@example.com").isPresent();
        
        testDataManager.trackMongoId(mongoMemberRepository.findByEmail("mongo@example.com").get().getId());
    }

    @Test
    void updateMember_MongoOnly_Success() throws Exception {
        // Arrange - Create a member via API to ensure proper ID generation
        MemberDTO memberDTO = MemberDTO.builder()
                .name("Original Name")
                .email("update@example.com")
                .phoneNumber("+15551234567")
                .build();

        // Create the member and get the response with the ID
        String responseJson = mockMvc.perform(post("/api/v1/members")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(memberDTO)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        
        MemberDTO createdMember = objectMapper.readValue(responseJson, MemberDTO.class);

        // Prepare update data
        MemberDTO updatedMemberDTO = MemberDTO.builder()
                .name("Updated MongoDB User")
                .email("update@example.com")
                .phoneNumber("+15559876543")
                .build();

        // Act - Update the member
        mockMvc.perform(put("/api/v1/members/" + createdMember.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updatedMemberDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated MongoDB User"))
                .andExpect(jsonPath("$.phoneNumber").value("+15559876543"));

        // Assert - Verify the member was updated in MongoDB
        MongoMember updatedMember = mongoMemberRepository.findByEmail("update@example.com").orElseThrow();
        assert "Updated MongoDB User".equals(updatedMember.getName());
        assert "+15559876543".equals(updatedMember.getPhoneNumber());
        testDataManager.trackMongoId(createdMember.getId());
    }

    @Test
    void deleteMember_MongoOnly_Success() throws Exception {
        // Arrange - Create a member via API to ensure proper ID generation
        MemberDTO memberDTO = MemberDTO.builder()
                .id("2")
                .name("Delete Me")
                .email("delete@example.com")
                .phoneNumber("+15551234567")
                .build();

        // Create the member and get the response with the ID
        String responseJson = mockMvc.perform(post("/api/v1/members")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(memberDTO)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        logger.debug("Response JSON: {}", responseJson);
        MemberDTO createdMember = objectMapper.readValue(responseJson, MemberDTO.class);

        // Act - Delete the member
        mockMvc.perform(delete("/api/v1/members/" + createdMember.getId()))
                .andExpect(status().isNoContent());

        // Assert - Verify the member was deleted from MongoDB
        assert mongoMemberRepository.findByEmail("delete@example.com").isEmpty();
    }

    @Test
    void searchMembers_MongoOnly_Success() throws Exception {
        // Arrange - Create members via API
        createMemberViaApi("John MongoDB", "john@example.com", "+15551234567");
        createMemberViaApi("Jane MongoDB", "jane@example.com", "+15559876543");
        createMemberViaApi("Bob SQL", "bob@example.com", "+15551112222");

        // Act & Assert - Search for members with "MongoDB" in their name
        mockMvc.perform(get("/api/v1/members/search?name=MongoDB"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[*].name", containsInAnyOrder("John MongoDB", "Jane MongoDB")));
    }
    
    /**
     * Helper method to create a member via the API
     */
    private MemberDTO createMemberViaApi(String name, String email, String phoneNumber) throws Exception {
        MemberDTO memberDTO = MemberDTO.builder()
                .name(name)
                .email(email)
                .phoneNumber(phoneNumber)
                .build();

        String responseJson = mockMvc.perform(post("/api/v1/members")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(memberDTO)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        
        MemberDTO createdMember = objectMapper.readValue(responseJson, MemberDTO.class);
        
        // Track IDs for cleanup
        testDataManager.trackMongoId(createdMember.getId());
        
        return createdMember;
    }
} 