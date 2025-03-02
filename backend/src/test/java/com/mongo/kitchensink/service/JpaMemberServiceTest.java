package com.mongo.kitchensink.service;

import com.mongo.kitchensink.exception.DuplicateEmailException;
import com.mongo.kitchensink.exception.MemberNotFoundException;
import com.mongo.kitchensink.model.JpaMember;
import com.mongo.kitchensink.model.IMember;
import com.mongo.kitchensink.repository.JpaMemberRepository;
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
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for the JpaMemberService class.
 * These tests verify the business logic in the service layer.
 */
@ExtendWith(MockitoExtension.class)
public class JpaMemberServiceTest {

    @Mock
    private JpaMemberRepository jpaMemberRepository;

    @InjectMocks
    private JpaMemberService jpaMemberService;

    private JpaMember testMember;

    @BeforeEach
    void setUp() {
        testMember = JpaMember.builder()
                .id(1L)
                .name("Test User")
                .email("test@example.com")
                .phoneNumber("+12345678901")
                .build();
    }

    @Test
    void createMember_ValidInput_ReturnsSavedMember() {
        // Arrange
        when(jpaMemberRepository.existsByEmail(anyString())).thenReturn(false);
        when(jpaMemberRepository.save(any(JpaMember.class))).thenReturn(testMember);

        // Act
        IMember savedMember = jpaMemberService.createMember(testMember);

        // Assert
        assertNotNull(savedMember);
        assertEquals(testMember.getId(), savedMember.getId());
        assertEquals(testMember.getName(), savedMember.getName());
        assertEquals(testMember.getEmail(), savedMember.getEmail());
        
        verify(jpaMemberRepository, times(1)).existsByEmail(testMember.getEmail());
        verify(jpaMemberRepository, times(1)).save(testMember);
    }

    @Test
    void createMember_DuplicateEmail_ThrowsDuplicateEmailException() {
        // Arrange
        when(jpaMemberRepository.existsByEmail(anyString())).thenReturn(true);

        // Act & Assert
        assertThrows(DuplicateEmailException.class, () -> {
            jpaMemberService.createMember(testMember);
        });
        
        verify(jpaMemberRepository, times(1)).existsByEmail(testMember.getEmail());
        verify(jpaMemberRepository, never()).save(any(JpaMember.class));
    }

    @Test
    void getAllMembers_ReturnsAllMembers() {
        // Arrange
        List<JpaMember> members = Arrays.asList(
                testMember,
                JpaMember.builder().id(2L).name("Second User").email("second@example.com").build()
        );
        when(jpaMemberRepository.findAll()).thenReturn(members);

        // Act
        List<IMember> result = jpaMemberService.getAllMembers();

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals(testMember.getId(), result.get(0).getId());
        assertEquals("Second User", result.get(1).getName());
        
        verify(jpaMemberRepository, times(1)).findAll();
    }

    @Test
    void getAllMembers_NoMembers_ReturnsEmptyList() {
        // Arrange
        when(jpaMemberRepository.findAll()).thenReturn(Collections.emptyList());

        // Act
        List<IMember> result = jpaMemberService.getAllMembers();

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
        
        verify(jpaMemberRepository, times(1)).findAll();
    }

    @Test
    void getMemberById_ExistingId_ReturnsMember() {
        // Arrange
        Long longId = 1L;
        String id = longId.toString();
        when(jpaMemberRepository.findById(longId)).thenReturn(Optional.of(testMember));

        // Act
        IMember result = jpaMemberService.getMemberById(id);

        // Assert
        assertNotNull(result);
        assertEquals(testMember.getId(), result.getId());
        assertEquals(testMember.getName(), result.getName());
        
        verify(jpaMemberRepository, times(1)).findById(longId);
    }

    @Test
    void getMemberById_NonExistingId_ThrowsMemberNotFoundException() {
        // Arrange
        Long longId = 999L;
        String id = longId.toString();
        when(jpaMemberRepository.findById(longId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(MemberNotFoundException.class, () -> {
            jpaMemberService.getMemberById(id);
        });
        
        verify(jpaMemberRepository, times(1)).findById(longId);
    }

    @Test
    void updateMember_ValidInput_ReturnsUpdatedMember() {
        // Arrange
        Long longId = 1L;
        String id = longId.toString();
        JpaMember updatedMember = JpaMember.builder()
                .name("Updated Name")
                .email("updated@example.com")
                .phoneNumber("+19876543210")
                .build();
        
        when(jpaMemberRepository.findById(longId)).thenReturn(Optional.of(testMember));
        when(jpaMemberRepository.existsByEmail("updated@example.com")).thenReturn(false);
        when(jpaMemberRepository.save(any(JpaMember.class))).thenReturn(testMember);

        // Act
        IMember result = jpaMemberService.updateMember(id, updatedMember);

        // Assert
        assertNotNull(result);
        assertEquals(testMember.getId(), result.getId());
        
        verify(jpaMemberRepository, times(1)).findById(longId);
        verify(jpaMemberRepository, times(1)).existsByEmail("updated@example.com");
        verify(jpaMemberRepository, times(1)).save(any(JpaMember.class));
    }

    @Test
    void updateMember_ExistingIdAndSameEmail_ReturnsUpdatedMember() {
        // Arrange
        Long longId = 1L;
        String id = longId.toString();
        JpaMember updatedMember = JpaMember.builder()
                .name("Updated Name")
                .email(testMember.getEmail()) // Same email
                .phoneNumber("+19876543210")
                .build();
        
        when(jpaMemberRepository.findById(longId)).thenReturn(Optional.of(testMember));
        when(jpaMemberRepository.save(any(JpaMember.class))).thenReturn(testMember);

        // Act
        IMember result = jpaMemberService.updateMember(id, updatedMember);

        // Assert
        assertNotNull(result);
        assertEquals(testMember.getId(), result.getId());
        
        verify(jpaMemberRepository, times(1)).findById(longId);
        verify(jpaMemberRepository, never()).existsByEmail(anyString());
        verify(jpaMemberRepository, times(1)).save(any(JpaMember.class));
    }

    @Test
    void updateMember_NonExistingId_ThrowsMemberNotFoundException() {
        // Arrange
        Long longId = 999L;
        String id = longId.toString();
        JpaMember updatedMember = JpaMember.builder()
                .name("Updated Name")
                .email("updated@example.com")
                .phoneNumber("+19876543210")
                .build();
        
        when(jpaMemberRepository.findById(longId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(MemberNotFoundException.class, () -> {
            jpaMemberService.updateMember(id, updatedMember);
        });
        
        verify(jpaMemberRepository, times(1)).findById(longId);
        verify(jpaMemberRepository, never()).existsByEmail(anyString());
        verify(jpaMemberRepository, never()).save(any(JpaMember.class));
    }

    @Test
    void updateMember_DuplicateEmail_ThrowsDuplicateEmailException() {
        // Arrange
        Long longId = 1L;
        String id = longId.toString();
        JpaMember updatedMember = JpaMember.builder()
                .name("Updated Name")
                .email("duplicate@example.com")
                .phoneNumber("+19876543210")
                .build();
        
        when(jpaMemberRepository.findById(longId)).thenReturn(Optional.of(testMember));
        when(jpaMemberRepository.existsByEmail(anyString())).thenReturn(true);

        // Act & Assert
        assertThrows(DuplicateEmailException.class, () -> {
            jpaMemberService.updateMember(id, updatedMember);
        });
        
        verify(jpaMemberRepository, times(1)).findById(longId);
        verify(jpaMemberRepository, times(1)).existsByEmail(updatedMember.getEmail());
        verify(jpaMemberRepository, never()).save(any(JpaMember.class));
    }

    @Test
    void deleteMember_ExistingId_DeletesMember() {
        // Arrange
        Long longId = 1L;
        String id = longId.toString();
        when(jpaMemberRepository.existsById(longId)).thenReturn(true);
        doNothing().when(jpaMemberRepository).deleteById(longId);

        // Act
        boolean result = jpaMemberService.deleteMember(id);

        // Assert
        assertTrue(result);
        verify(jpaMemberRepository, times(1)).existsById(longId);
        verify(jpaMemberRepository, times(1)).deleteById(longId);
    }

    @Test
    void deleteMember_NonExistingId_ReturnsFalse() {
        // Arrange
        Long longId = 999L;
        String id = longId.toString();
        when(jpaMemberRepository.existsById(longId)).thenReturn(false);

        // Act
        boolean result = jpaMemberService.deleteMember(id);

        // Assert
        assertFalse(result);
        verify(jpaMemberRepository, times(1)).existsById(longId);
        verify(jpaMemberRepository, never()).deleteById(anyLong());
    }

    @Test
    void searchMembers_ReturnsMatchingMembers() {
        // Arrange
        List<JpaMember> matchingMembers = Arrays.asList(testMember);
        when(jpaMemberRepository.findByNameContainingIgnoreCase(anyString())).thenReturn(matchingMembers);

        // Act
        List<IMember> result = jpaMemberService.searchMembers("Test");

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(testMember.getId(), result.get(0).getId());
        
        verify(jpaMemberRepository, times(1)).findByNameContainingIgnoreCase("Test");
    }

    @Test
    void searchMembers_NoMatches_ReturnsEmptyList() {
        // Arrange
        when(jpaMemberRepository.findByNameContainingIgnoreCase(anyString())).thenReturn(Collections.emptyList());

        // Act
        List<IMember> result = jpaMemberService.searchMembers("NonExistent");

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
        
        verify(jpaMemberRepository, times(1)).findByNameContainingIgnoreCase("NonExistent");
    }
} 