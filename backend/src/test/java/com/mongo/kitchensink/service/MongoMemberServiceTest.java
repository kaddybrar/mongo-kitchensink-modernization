package com.mongo.kitchensink.service;

import com.mongo.kitchensink.exception.DuplicateEmailException;
import com.mongo.kitchensink.exception.MemberNotFoundException;
import com.mongo.kitchensink.model.MongoMember;
import com.mongo.kitchensink.model.IMember;
import com.mongo.kitchensink.repository.MongoMemberRepository;
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
 * Unit tests for the MongoMemberService class.
 * These tests verify the business logic in the service layer.
 */
@ExtendWith(MockitoExtension.class)
public class MongoMemberServiceTest {

    @Mock
    private MongoMemberRepository mongoMemberRepository;

    @InjectMocks
    private MongoMemberService mongoMemberService;

    private MongoMember testMember;

    @BeforeEach
    void setUp() {
        testMember = MongoMember.builder()
                .id("1")
                .name("Test User")
                .email("test@example.com")
                .phoneNumber("+12345678901")
                .build();
    }

    @Test
    void createMember_ValidInput_ReturnsSavedMember() {
        // Arrange
        when(mongoMemberRepository.existsByEmail(anyString())).thenReturn(false);
        when(mongoMemberRepository.save(any(MongoMember.class))).thenReturn(testMember);

        // Act
        IMember savedMember = mongoMemberService.createMember(testMember);

        // Assert
        assertNotNull(savedMember);
        assertEquals(testMember.getId(), savedMember.getId());
        assertEquals(testMember.getName(), savedMember.getName());
        assertEquals(testMember.getEmail(), savedMember.getEmail());
        
        verify(mongoMemberRepository, times(1)).existsByEmail(testMember.getEmail());
        verify(mongoMemberRepository, times(1)).save(testMember);
    }

    @Test
    void createMember_DuplicateEmail_ThrowsDuplicateEmailException() {
        // Arrange
        when(mongoMemberRepository.existsByEmail(anyString())).thenReturn(true);

        // Act & Assert
        assertThrows(DuplicateEmailException.class, () -> {
            mongoMemberService.createMember(testMember);
        });
        
        verify(mongoMemberRepository, times(1)).existsByEmail(testMember.getEmail());
        verify(mongoMemberRepository, never()).save(any(MongoMember.class));
    }

    @Test
    void getAllMembers_ReturnsAllMembers() {
        // Arrange
        List<MongoMember> members = Arrays.asList(
                testMember,
                MongoMember.builder().id("2").name("Second User").email("second@example.com").build()
        );
        when(mongoMemberRepository.findAll()).thenReturn(members);

        // Act
        List<IMember> result = mongoMemberService.getAllMembers();

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals(testMember.getId(), result.get(0).getId());
        assertEquals("Second User", result.get(1).getName());
        
        verify(mongoMemberRepository, times(1)).findAll();
    }

    // Add more tests similar to JpaMemberServiceTest but adapted for MongoDB
    // ...
} 