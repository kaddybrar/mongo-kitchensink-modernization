package com.mongo.kitchensink.repository;

import com.mongo.kitchensink.model.Member;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for the MemberRepository interface.
 * These tests verify the repository's interaction with the H2 database.
 */
@DataJpaTest
@ActiveProfiles("test")
public class MemberRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private MemberRepository memberRepository;

    @Test
    void findById_ExistingId_ReturnsMember() {
        // Arrange
        Member member = Member.builder()
                .name("Test User")
                .email("test@example.com")
                .phoneNumber("+12345678901")
                .build();
        
        Long id = entityManager.persistAndGetId(member, Long.class);
        entityManager.flush();
        
        // Act
        Optional<Member> foundMember = memberRepository.findById(id);
        
        // Assert
        assertTrue(foundMember.isPresent());
        assertEquals(member.getName(), foundMember.get().getName());
        assertEquals(member.getEmail(), foundMember.get().getEmail());
    }

    @Test
    void findById_NonExistingId_ReturnsEmptyOptional() {
        // Act
        Optional<Member> foundMember = memberRepository.findById(999L);
        
        // Assert
        assertFalse(foundMember.isPresent());
    }

    @Test
    void existsByEmail_ExistingEmail_ReturnsTrue() {
        // Arrange
        Member member = Member.builder()
                .name("Test User")
                .email("test@example.com")
                .phoneNumber("+12345678901")
                .build();
        
        entityManager.persist(member);
        entityManager.flush();
        
        // Act
        boolean exists = memberRepository.existsByEmail("test@example.com");
        
        // Assert
        assertTrue(exists);
    }

    @Test
    void existsByEmail_NonExistingEmail_ReturnsFalse() {
        // Act
        boolean exists = memberRepository.existsByEmail("nonexistent@example.com");
        
        // Assert
        assertFalse(exists);
    }

    @Test
    void findByNameContainingIgnoreCase_MatchingName_ReturnsMembers() {
        // Arrange
        Member member = Member.builder()
                .name("Test User")
                .email("test@example.com")
                .phoneNumber("+12345678901")
                .build();
        
        entityManager.persist(member);
        entityManager.flush();
        
        // Act
        List<Member> foundMembers = memberRepository.findByNameContainingIgnoreCase("test");
        
        // Assert
        assertFalse(foundMembers.isEmpty());
        assertEquals(1, foundMembers.size());
        assertEquals(member.getName(), foundMembers.get(0).getName());
    }

    @Test
    void findByNameContainingIgnoreCase_PartialMatch_ReturnsMembers() {
        // Arrange
        Member member = Member.builder()
                .name("Test User")
                .email("test@example.com")
                .phoneNumber("+12345678901")
                .build();
        
        entityManager.persist(member);
        entityManager.flush();
        
        // Act
        List<Member> foundMembers = memberRepository.findByNameContainingIgnoreCase("est");
        
        // Assert
        assertFalse(foundMembers.isEmpty());
        assertEquals(1, foundMembers.size());
        assertEquals(member.getName(), foundMembers.get(0).getName());
    }

    @Test
    void findByNameContainingIgnoreCase_CaseInsensitive_ReturnsMembers() {
        // Arrange
        Member member = Member.builder()
                .name("Test User")
                .email("test@example.com")
                .phoneNumber("+12345678901")
                .build();
        
        entityManager.persist(member);
        entityManager.flush();
        
        // Act
        List<Member> foundMembers = memberRepository.findByNameContainingIgnoreCase("TEST");
        
        // Assert
        assertFalse(foundMembers.isEmpty());
        assertEquals(1, foundMembers.size());
        assertEquals(member.getName(), foundMembers.get(0).getName());
    }

    @Test
    void findByNameContainingIgnoreCase_NonMatchingName_ReturnsEmptyList() {
        // Act
        List<Member> foundMembers = memberRepository.findByNameContainingIgnoreCase("NonExistent");
        
        // Assert
        assertTrue(foundMembers.isEmpty());
    }

    @Test
    void save_NewMember_ReturnsSavedMember() {
        // Arrange
        Member newMember = Member.builder()
                .name("New User")
                .email("new@example.com")
                .phoneNumber("+19876543210")
                .build();
        
        // Act
        Member savedMember = memberRepository.save(newMember);
        
        // Assert
        assertNotNull(savedMember.getId());
        assertEquals(newMember.getName(), savedMember.getName());
        assertEquals(newMember.getEmail(), savedMember.getEmail());
        
        // Verify it's in the database
        Member foundMember = entityManager.find(Member.class, savedMember.getId());
        assertNotNull(foundMember);
        assertEquals(savedMember.getName(), foundMember.getName());
    }

    @Test
    void save_ExistingMember_ReturnsUpdatedMember() {
        // Arrange
        Member member = Member.builder()
                .name("Test User")
                .email("test@example.com")
                .phoneNumber("+12345678901")
                .build();
        
        Long id = entityManager.persistAndGetId(member, Long.class);
        entityManager.flush();
        
        Member existingMember = memberRepository.findById(id).get();
        existingMember.setName("Updated Name");
        
        // Act
        Member updatedMember = memberRepository.save(existingMember);
        
        // Assert
        assertEquals(id, updatedMember.getId());
        assertEquals("Updated Name", updatedMember.getName());
        
        // Verify it's updated in the database
        Member foundMember = entityManager.find(Member.class, id);
        assertEquals("Updated Name", foundMember.getName());
    }

    @Test
    void deleteById_ExistingId_RemovesMember() {
        // Arrange
        Member member = Member.builder()
                .name("Test User")
                .email("test@example.com")
                .phoneNumber("+12345678901")
                .build();
        
        Long id = entityManager.persistAndGetId(member, Long.class);
        entityManager.flush();
        
        // Act
        memberRepository.deleteById(id);
        
        // Assert
        Member foundMember = entityManager.find(Member.class, id);
        assertNull(foundMember);
    }

    @Test
    void findAll_ReturnsAllMembers() {
        // Arrange
        Member member1 = Member.builder()
                .name("Test User")
                .email("test@example.com")
                .phoneNumber("+12345678901")
                .build();
        
        Member member2 = Member.builder()
                .name("Second User")
                .email("second@example.com")
                .phoneNumber("+19876543210")
                .build();
        
        entityManager.persist(member1);
        entityManager.persist(member2);
        entityManager.flush();
        
        // Act
        List<Member> allMembers = memberRepository.findAll();
        
        // Assert
        assertEquals(2, allMembers.size());
    }
} 