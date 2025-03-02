package com.mongo.kitchensink.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongo.kitchensink.KitchensinkApplication;
import com.mongo.kitchensink.dto.MemberDTO;
import com.mongo.kitchensink.model.JpaMember;
import com.mongo.kitchensink.model.MongoMember;
import com.mongo.kitchensink.repository.JpaMemberRepository;
import com.mongo.kitchensink.repository.MongoMemberRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Base class for integration tests.
 * Sets up the test environment and provides utility methods.
 */
@SpringBootTest(classes = KitchensinkApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
public abstract class BaseIntegrationTest {

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    @Autowired
    protected JpaMemberRepository jpaMemberRepository;

    @Autowired
    protected MongoMemberRepository mongoMemberRepository;

    @BeforeEach
    void setUp() {
        // Clean up databases before each test
        cleanDatabases();
    }

    @AfterEach
    void tearDown() {
        // Clean up databases after each test
        cleanDatabases();
    }

    /**
     * Cleans up both databases.
     */
    protected void cleanDatabases() {
        jpaMemberRepository.deleteAll();
        mongoMemberRepository.deleteAll();
    }

    /**
     * Creates a test member in the database.
     *
     * @return the created member DTO
     * @throws Exception if an error occurs
     */
    protected MemberDTO createTestMember() throws Exception {
        MemberDTO memberDTO = MemberDTO.builder()
                .name("Test User")
                .email("test@example.com")
                .phoneNumber("+12345678901")
                .build();

        MvcResult result = mockMvc.perform(post("/api/v1/members")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(memberDTO)))
                .andExpect(status().isOk())
                .andReturn();

        return objectMapper.readValue(result.getResponse().getContentAsString(), MemberDTO.class);
    }

    /**
     * Creates a test member with the given details.
     *
     * @param name the name
     * @param email the email
     * @param phoneNumber the phone number
     * @return the created member DTO
     * @throws Exception if an error occurs
     */
    protected MemberDTO createTestMember(String name, String email, String phoneNumber) throws Exception {
        MemberDTO memberDTO = MemberDTO.builder()
                .name(name)
                .email(email)
                .phoneNumber(phoneNumber)
                .build();

        MvcResult result = mockMvc.perform(post("/api/v1/members")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(memberDTO)))
                .andExpect(status().isOk())
                .andReturn();

        return objectMapper.readValue(result.getResponse().getContentAsString(), MemberDTO.class);
    }

    /**
     * Verifies that a member exists in both databases.
     *
     * @param id the member ID
     * @param name the member name
     * @param email the member email
     */
    protected void verifyMemberExistsInBothDatabases(String id, String name, String email) {
        // Check JPA database
        JpaMember jpaMember = jpaMemberRepository.findById(Long.parseLong(id)).orElse(null);
        assert jpaMember != null;
        assert jpaMember.getName().equals(name);
        assert jpaMember.getEmail().equals(email);

        // Check MongoDB
        MongoMember mongoMember = mongoMemberRepository.findById(id).orElse(null);
        assert mongoMember != null;
        assert mongoMember.getName().equals(name);
        assert mongoMember.getEmail().equals(email);
    }
} 