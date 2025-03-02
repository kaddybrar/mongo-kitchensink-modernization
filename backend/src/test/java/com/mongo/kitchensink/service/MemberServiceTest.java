package com.mongo.kitchensink.service;

import com.mongo.kitchensink.exception.DuplicateEmailException;
import com.mongo.kitchensink.exception.MemberNotFoundException;
import com.mongo.kitchensink.model.Member;
import com.mongo.kitchensink.repository.MemberRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for the MemberService class.
 * These tests verify the business logic in the service layer.
 */
@ExtendWith(MockitoExtension.class)
public class MemberServiceTest {

    @Mock
    private MemberRepository memberRepository;

    @InjectMocks
    private MemberService memberService;

    private Member testMember;

    @BeforeEach
    void setUp() {
        testMember = Member.builder()
                .id(1L)
                .name("Test User")
                .email("test@example.com")
                .phoneNumber("+12345678901")
                .build();
    }

    @Test
    void createMember_ValidInput_ReturnsSavedMember() {
        // Arrange
        when(memberRepository.existsByEmail(anyString())).thenReturn(false);
        when(memberRepository.save(any(Member.class))).thenReturn(testMember);

        // Act
        Member savedMember = memberService.createMember(testMember);

        // Assert
        assertNotNull(savedMember);
        assertEquals(testMember.getId(), savedMember.getId());
        assertEquals(testMember.getName(), savedMember.getName());
        assertEquals(testMember.getEmail(), savedMember.getEmail());
        
        verify(memberRepository, times(1)).existsByEmail(testMember.getEmail());
        verify(memberRepository, times(1)).save(testMember);
    }

    @Test
    void createMember_DuplicateEmail_ThrowsDuplicateEmailException() {
        // Arrange
        when(memberRepository.existsByEmail(anyString())).thenReturn(true);

        // Act & Assert
        assertThrows(DuplicateEmailException.class, () -> {
            memberService.createMember(testMember);
        });
        
        verify(memberRepository, times(1)).existsByEmail(testMember.getEmail());
        verify(memberRepository, never()).save(any(Member.class));
    }

    @Test
    void getAllMembers_ReturnsAllMembers() {
        // Arrange
        List<Member> members = Arrays.asList(
                testMember,
                Member.builder().id(2L).name("Second User").email("second@example.com").build()
        );
        when(memberRepository.findAll()).thenReturn(members);

        // Act
        List<Member> result = memberService.getAllMembers();

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals(testMember.getId(), result.get(0).getId());
        assertEquals("Second User", result.get(1).getName());
        
        verify(memberRepository, times(1)).findAll();
    }

    @Test
    void getAllMembers_NoMembers_ReturnsEmptyList() {
        // Arrange
        when(memberRepository.findAll()).thenReturn(Collections.emptyList());

        // Act
        List<Member> result = memberService.getAllMembers();

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
        
        verify(memberRepository, times(1)).findAll();
    }

    @Test
    void getMemberById_ExistingId_ReturnsMember() {
        // Arrange
        when(memberRepository.findById(anyLong())).thenReturn(Optional.of(testMember));

        // Act
        Member result = memberService.getMemberById(1L);

        // Assert
        assertNotNull(result);
        assertEquals(testMember.getId(), result.getId());
        assertEquals(testMember.getName(), result.getName());
        
        verify(memberRepository, times(1)).findById(1L);
    }

    @Test
    void getMemberById_NonExistingId_ThrowsMemberNotFoundException() {
        // Arrange
        when(memberRepository.findById(anyLong())).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(MemberNotFoundException.class, () -> {
            memberService.getMemberById(999L);
        });
        
        verify(memberRepository, times(1)).findById(999L);
    }

    @Test
    void updateMember_ExistingIdAndUniqueEmail_ReturnsUpdatedMember() {
        // Arrange
        Member updatedMember = Member.builder()
                .name("Updated Name")
                .email("updated@example.com")
                .phoneNumber("+19876543210")
                .build();
        
        when(memberRepository.findById(anyLong())).thenReturn(Optional.of(testMember));
        when(memberRepository.existsByEmail(anyString())).thenReturn(false);
        when(memberRepository.save(any(Member.class))).thenReturn(testMember);

        // Act
        Member result = memberService.updateMember(1L, updatedMember);

        // Assert
        assertNotNull(result);
        assertEquals(testMember.getId(), result.getId());
        
        verify(memberRepository, times(1)).findById(1L);
        verify(memberRepository, times(1)).existsByEmail(updatedMember.getEmail());
        verify(memberRepository, times(1)).save(any(Member.class));
    }

    @Test
    void updateMember_ExistingIdAndSameEmail_ReturnsUpdatedMember() {
        // Arrange
        Member updatedMember = Member.builder()
                .name("Updated Name")
                .email(testMember.getEmail()) // Same email
                .phoneNumber("+19876543210")
                .build();
        
        when(memberRepository.findById(anyLong())).thenReturn(Optional.of(testMember));
        when(memberRepository.save(any(Member.class))).thenReturn(testMember);

        // Act
        Member result = memberService.updateMember(1L, updatedMember);

        // Assert
        assertNotNull(result);
        assertEquals(testMember.getId(), result.getId());
        
        verify(memberRepository, times(1)).findById(1L);
        verify(memberRepository, never()).existsByEmail(anyString());
        verify(memberRepository, times(1)).save(any(Member.class));
    }

    @Test
    void updateMember_NonExistingId_ThrowsMemberNotFoundException() {
        // Arrange
        Member updatedMember = Member.builder()
                .name("Updated Name")
                .email("updated@example.com")
                .phoneNumber("+19876543210")
                .build();
        
        when(memberRepository.findById(anyLong())).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(MemberNotFoundException.class, () -> {
            memberService.updateMember(999L, updatedMember);
        });
        
        verify(memberRepository, times(1)).findById(999L);
        verify(memberRepository, never()).existsByEmail(anyString());
        verify(memberRepository, never()).save(any(Member.class));
    }

    @Test
    void updateMember_DuplicateEmail_ThrowsDuplicateEmailException() {
        // Arrange
        Member updatedMember = Member.builder()
                .name("Updated Name")
                .email("duplicate@example.com")
                .phoneNumber("+19876543210")
                .build();
        
        when(memberRepository.findById(anyLong())).thenReturn(Optional.of(testMember));
        when(memberRepository.existsByEmail(anyString())).thenReturn(true);

        // Act & Assert
        assertThrows(DuplicateEmailException.class, () -> {
            memberService.updateMember(1L, updatedMember);
        });
        
        verify(memberRepository, times(1)).findById(1L);
        verify(memberRepository, times(1)).existsByEmail(updatedMember.getEmail());
        verify(memberRepository, never()).save(any(Member.class));
    }

    @Test
    void deleteMember_ExistingId_DeletesMember() {
        // Arrange
        when(memberRepository.existsById(anyLong())).thenReturn(true);
        doNothing().when(memberRepository).deleteById(anyLong());

        // Act
        memberService.deleteMember(1L);

        // Assert
        verify(memberRepository, times(1)).existsById(1L);
        verify(memberRepository, times(1)).deleteById(1L);
    }

    @Test
    void deleteMember_NonExistingId_ThrowsMemberNotFoundException() {
        // Arrange
        when(memberRepository.existsById(anyLong())).thenReturn(false);

        // Act & Assert
        assertThrows(MemberNotFoundException.class, () -> {
            memberService.deleteMember(999L);
        });
        
        verify(memberRepository, times(1)).existsById(999L);
        verify(memberRepository, never()).deleteById(anyLong());
    }

    @Test
    void searchMembers_ReturnsMatchingMembers() {
        // Arrange
        List<Member> matchingMembers = Arrays.asList(testMember);
        when(memberRepository.findByNameContainingIgnoreCase(anyString())).thenReturn(matchingMembers);

        // Act
        List<Member> result = memberService.searchMembers("Test");

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(testMember.getId(), result.get(0).getId());
        
        verify(memberRepository, times(1)).findByNameContainingIgnoreCase("Test");
    }

    @Test
    void searchMembers_NoMatches_ReturnsEmptyList() {
        // Arrange
        when(memberRepository.findByNameContainingIgnoreCase(anyString())).thenReturn(Collections.emptyList());

        // Act
        List<Member> result = memberService.searchMembers("NonExistent");

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
        
        verify(memberRepository, times(1)).findByNameContainingIgnoreCase("NonExistent");
    }
} 