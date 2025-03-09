package com.mongo.kitchensink.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongo.kitchensink.KitchensinkApplication;
import com.mongo.kitchensink.config.TestDataManager;
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
import org.junit.jupiter.api.TestInstance;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.test.context.transaction.TestTransaction;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.Set;

/**
 * Base class for integration tests.
 * Sets up the test environment and provides utility methods.
 */
@SpringBootTest(classes = KitchensinkApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class BaseIntegrationTest {
    private static final Logger logger = LoggerFactory.getLogger(BaseIntegrationTest.class);

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    @Autowired
    protected JpaMemberRepository jpaMemberRepository;

    @Autowired
    protected MongoMemberRepository mongoMemberRepository;

    @Autowired
    protected TestDataManager testDataManager;

    @BeforeEach
    public void setUp() {
        try {
            // Clear any existing tracking from previous test
            testDataManager.clearTrackedIds();
            logger.debug("Test setup completed successfully");
        } catch (Exception e) {
            logger.error("Error during test setup", e);
            throw e;
        }
    }

    @AfterEach
    public void tearDown() {
        try {
            // Ensure we're not in a transaction when cleaning up
            if (TestTransaction.isActive()) {
                TestTransaction.end();
            }
            
            // Get counts before cleanup for logging
            long jpaCountBefore = jpaMemberRepository.count();
            long mongoCountBefore = mongoMemberRepository.count();
            
            // Clean up only the data created in this test
            cleanupTestData();
            
            // Get counts after cleanup for verification
            long jpaCountAfter = jpaMemberRepository.count();
            long mongoCountAfter = mongoMemberRepository.count();
            
            // Log the cleanup results
            logger.debug("Cleanup results - JPA: {} -> {}, MongoDB: {} -> {}", 
                jpaCountBefore, jpaCountAfter,
                mongoCountBefore, mongoCountAfter);
            
            // Remove thread-specific tracking data
            testDataManager.removeTracking();
            
            logger.debug("Test cleanup completed successfully");
        } catch (Exception e) {
            logger.error("Error during test cleanup", e);
            throw e;
        }
    }

    /**
     * Cleans up data created during the test.
     * Only removes entities that were tracked during test execution.
     */
    private void cleanupTestData() {
        Set<String> mongoIds = testDataManager.getTrackedMongoIds();
        Set<Long> jpaIds = testDataManager.getTrackedJpaIds();
        
        if (!mongoIds.isEmpty() || !jpaIds.isEmpty()) {
            logger.debug("Cleaning up test data - Mongo IDs: {}, JPA IDs: {}", mongoIds, jpaIds);
            
            // Clean up tracked data
            testDataManager.cleanupAllTestData();
            
            // Verify cleanup
            verifyTestDataCleanup(mongoIds, jpaIds);
        } else {
            logger.debug("No test data to clean up");
        }
    }

    /**
     * Verifies that specific test data has been cleaned up.
     */
    private void verifyTestDataCleanup(Set<String> mongoIds, Set<Long> jpaIds) {
        // Verify MongoDB cleanup
        for (String id : mongoIds) {
            assert !mongoMemberRepository.existsById(id) : 
                String.format("MongoDB entity with ID %s still exists after cleanup", id);
        }
        
        // Verify JPA cleanup
        for (Long id : jpaIds) {
            assert !jpaMemberRepository.existsById(id) : 
                String.format("JPA entity with ID %d still exists after cleanup", id);
        }
        
        logger.debug("Test data cleanup verification successful");
    }

    @Transactional
    protected MemberDTO createTestMember() throws Exception {
        try {
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

            MemberDTO createdMember = objectMapper.readValue(result.getResponse().getContentAsString(), MemberDTO.class);
            
            // Track the created IDs
            testDataManager.trackMongoId(createdMember.getId());
            testDataManager.trackJpaId(Long.parseLong(createdMember.getId()));
            
            logger.debug("Created test member with ID: {}", createdMember.getId());
            return createdMember;
        } catch (Exception e) {
            logger.error("Error creating test member", e);
            throw e;
        }
    }

    @Transactional
    protected MemberDTO createTestMember(String name, String email, String phoneNumber) throws Exception {
        try {
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

            MemberDTO createdMember = objectMapper.readValue(result.getResponse().getContentAsString(), MemberDTO.class);
            
            // Track the created IDs
            testDataManager.trackMongoId(createdMember.getId());
            testDataManager.trackJpaId(Long.parseLong(createdMember.getId()));
            
            logger.debug("Created test member with ID: {}", createdMember.getId());
            return createdMember;
        } catch (Exception e) {
            logger.error("Error creating test member", e);
            throw e;
        }
    }

    /**
     * Verifies that a member exists in both databases.
     *
     * @param id the member ID
     * @param name the member name
     * @param email the member email
     */
    protected void verifyMemberExistsInBothDatabases(String id, String name, String email) {
        try {
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
            
            logger.debug("Verified member exists in both databases with ID: {}", id);
        } catch (Exception e) {
            logger.error("Error verifying member in databases", e);
            throw e;
        }
    }
} 