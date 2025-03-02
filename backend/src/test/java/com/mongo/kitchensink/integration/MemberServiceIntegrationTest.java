package com.mongo.kitchensink.integration;

import com.mongo.kitchensink.base.BaseIntegrationTest;
import com.mongo.kitchensink.exception.DuplicateEmailException;
import com.mongo.kitchensink.exception.MemberNotFoundException;
import com.mongo.kitchensink.model.Member;
import com.mongo.kitchensink.repository.MemberRepository;
import com.mongo.kitchensink.service.MemberService;
import com.mongo.kitchensink.util.TestDataFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for the MemberService class.
 * These tests verify the service's interaction with the repository layer
 * in a real application context.
 */
public class MemberServiceIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private MemberService memberService;

    @Autowired
    private MemberRepository memberRepository;

    private Member testMember;

    @BeforeEach
    void setUp() {
        // Clean up the database before each test
        memberRepository.deleteAll();

        // Create a test member
        testMember = TestDataFactory.createValidMember();
    }

    @AfterEach
    void tearDown() {
        // Clean up the database after each test
        memberRepository.deleteAll();
    }

    @Test
    void createMember_ValidInput_PersistsMember() {
        // Act
        Member createdMember = memberService.createMember(testMember);

        // Assert
        assertNotNull(createdMember.getId());
        assertEquals(testMember.getName(), createdMember.getName());
        assertEquals(testMember.getEmail(), createdMember.getEmail());
        assertEquals(testMember.getPhoneNumber(), createdMember.getPhoneNumber());

        // Verify it's in the database
        assertTrue(memberRepository.existsById(createdMember.getId()));
    }

    @Test
    void createMember_DuplicateEmail_ThrowsException() {
        // Arrange
        memberService.createMember(testMember);

        Member duplicateEmailMember = Member.builder()
                .name("Another User")
                .email("test@example.com") // Same email as testMember
                .phoneNumber("+19876543210")
                .build();

        // Act & Assert
        assertThrows(DuplicateEmailException.class, () -> {
            memberService.createMember(duplicateEmailMember);
        });
    }

    @Test
    void getAllMembers_ReturnsAllMembers() {
        // Arrange
        Member member1 = memberService.createMember(testMember);
        
        Member secondMember = Member.builder()
                .name("Second User")
                .email("second@example.com")
                .phoneNumber("+19876543210")
                .build();
        Member member2 = memberService.createMember(secondMember);

        // Act
        List<Member> allMembers = memberService.getAllMembers();

        // Assert
        assertEquals(2, allMembers.size());
        assertTrue(allMembers.stream().anyMatch(m -> m.getId().equals(member1.getId())));
        assertTrue(allMembers.stream().anyMatch(m -> m.getId().equals(member2.getId())));
    }

    @Test
    void getMemberById_ExistingId_ReturnsMember() {
        // Arrange
        Member createdMember = memberService.createMember(testMember);

        // Act
        Member foundMember = memberService.getMemberById(createdMember.getId());

        // Assert
        assertEquals(createdMember.getId(), foundMember.getId());
        assertEquals(createdMember.getName(), foundMember.getName());
        assertEquals(createdMember.getEmail(), foundMember.getEmail());
    }

    @Test
    void getMemberById_NonExistingId_ThrowsException() {
        // Act & Assert
        assertThrows(MemberNotFoundException.class, () -> {
            memberService.getMemberById(999L);
        });
    }

    @Test
    void updateMember_ExistingId_UpdatesMember() {
        // Arrange
        Member createdMember = memberService.createMember(testMember);
        
        Member updatedMemberData = Member.builder()
                .name("Updated Name")
                .email("updated@example.com")
                .phoneNumber("+19876543210")
                .build();

        // Act
        Member updatedMember = memberService.updateMember(createdMember.getId(), updatedMemberData);

        // Assert
        assertEquals(createdMember.getId(), updatedMember.getId());
        assertEquals("Updated Name", updatedMember.getName());
        assertEquals("updated@example.com", updatedMember.getEmail());
        assertEquals("+19876543210", updatedMember.getPhoneNumber());

        // Verify it's updated in the database
        Member foundMember = memberRepository.findById(createdMember.getId()).orElseThrow();
        assertEquals("Updated Name", foundMember.getName());
    }

    @Test
    void updateMember_NonExistingId_ThrowsException() {
        // Arrange
        Member updatedMemberData = Member.builder()
                .name("Updated Name")
                .email("updated@example.com")
                .phoneNumber("+19876543210")
                .build();

        // Act & Assert
        assertThrows(MemberNotFoundException.class, () -> {
            memberService.updateMember(999L, updatedMemberData);
        });
    }

    @Test
    void updateMember_DuplicateEmail_ThrowsException() {
        // Arrange
        Member member1 = memberService.createMember(testMember);
        
        Member member2 = Member.builder()
                .name("Second User")
                .email("second@example.com")
                .phoneNumber("+19876543210")
                .build();
        member2 = memberService.createMember(member2);

        Member updatedMemberData = Member.builder()
                .name("Updated Name")
                .email("second@example.com") // Same as member2's email
                .phoneNumber("+19876543210")
                .build();

        // Act & Assert
        assertThrows(DuplicateEmailException.class, () -> {
            memberService.updateMember(member1.getId(), updatedMemberData);
        });
    }

    @Test
    void deleteMember_ExistingId_RemovesMember() {
        // Arrange
        Member createdMember = memberService.createMember(testMember);

        // Act
        memberService.deleteMember(createdMember.getId());

        // Assert
        assertFalse(memberRepository.existsById(createdMember.getId()));
    }

    @Test
    void deleteMember_NonExistingId_ThrowsException() {
        // Act & Assert
        assertThrows(MemberNotFoundException.class, () -> {
            memberService.deleteMember(999L);
        });
    }

    @Test
    void searchMembers_MatchingName_ReturnsMembers() {
        // Arrange
        memberService.createMember(testMember);
        
        Member member2 = Member.builder()
                .name("Another Test User")
                .email("another@example.com")
                .phoneNumber("+19876543210")
                .build();
        memberService.createMember(member2);

        Member member3 = Member.builder()
                .name("Different User")
                .email("different@example.com")
                .phoneNumber("+19876543210")
                .build();
        memberService.createMember(member3);

        // Act
        List<Member> searchResults = memberService.searchMembers("Test");

        // Assert
        assertEquals(2, searchResults.size());
        assertTrue(searchResults.stream().anyMatch(m -> m.getName().equals("Test User")));
        assertTrue(searchResults.stream().anyMatch(m -> m.getName().equals("Another Test User")));
    }

    @Test
    void searchMembers_NoMatches_ReturnsEmptyList() {
        // Arrange
        memberService.createMember(testMember);

        // Act
        List<Member> searchResults = memberService.searchMembers("NonExistent");

        // Assert
        assertTrue(searchResults.isEmpty());
    }
} 