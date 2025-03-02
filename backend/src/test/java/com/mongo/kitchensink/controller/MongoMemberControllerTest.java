package com.mongo.kitchensink.controller;

import com.mongo.kitchensink.dto.MemberDTO;
import com.mongo.kitchensink.exception.DuplicateEmailException;
import com.mongo.kitchensink.exception.MemberNotFoundException;
import com.mongo.kitchensink.model.IMember;
import com.mongo.kitchensink.model.MemberFactory;
import com.mongo.kitchensink.model.MongoMember;
import com.mongo.kitchensink.service.MongoMemberService;
import com.mongo.kitchensink.util.TestDataFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for the MemberController with MongoDB implementation.
 * This approach tests the controller methods directly without using MockMvc.
 */
@ExtendWith(MockitoExtension.class)
public class MongoMemberControllerTest {

    @Mock
    private MongoMemberService memberService;

    @Mock
    private MemberFactory memberFactory;

    private MemberController memberController;
    private MongoMember testMember;
    private MemberDTO testMemberDTO;

    @BeforeEach
    void setUp() {
        // Create controller with mocked service
        memberController = new MemberController(memberService, memberFactory);
        
        // Create test data
        testMember = TestDataFactory.createValidMongoMember();
        testMember.setId("1");
        
        // Create test DTO
        testMemberDTO = MemberDTO.builder()
                .id("1")
                .name(testMember.getName())
                .email(testMember.getEmail())
                .phoneNumber(testMember.getPhoneNumber())
                .build();
        
        // We'll set up specific stubs in each test method instead of here
    }

    @Test
    void createMember_ValidInput_ReturnsCreatedMember() {
        // Arrange
        when(memberFactory.createMember()).thenReturn(new MongoMember());
        doReturn(testMember).when(memberService).createMember(any());

        // Act
        ResponseEntity<MemberDTO> response = memberController.createMember(testMemberDTO);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(testMemberDTO.getId(), response.getBody().getId());
        assertEquals(testMemberDTO.getName(), response.getBody().getName());
        assertEquals(testMemberDTO.getEmail(), response.getBody().getEmail());
        verify(memberService, times(1)).createMember(any());
    }

    @Test
    void createMember_DuplicateEmail_ThrowsException() {
        // Arrange
        when(memberFactory.createMember()).thenReturn(new MongoMember());
        doThrow(new DuplicateEmailException("Email already exists"))
            .when(memberService).createMember(any());

        // Act & Assert
        assertThrows(DuplicateEmailException.class, () -> {
            memberController.createMember(testMemberDTO);
        });
        
        verify(memberService, times(1)).createMember(any());
    }

    @Test
    void getAllMembers_ReturnsAllMembers() {
        // Arrange
        List<IMember> members = Arrays.asList(
                testMember,
                MongoMember.builder().id("def456").name("Second User").email("second@example.com").build()
        );
        when(memberService.getAllMembers()).thenReturn(members);

        // Act
        ResponseEntity<List<MemberDTO>> response = memberController.getAllMembers();

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(2, response.getBody().size());
        assertEquals(testMember.getId().toString(), response.getBody().get(0).getId());
        assertEquals(testMember.getName(), response.getBody().get(0).getName());
        verify(memberService, times(1)).getAllMembers();
    }

    @Test
    void getMemberById_ExistingId_ReturnsMember() {
        // Arrange
        String id = "1";
        when(memberService.getMemberById(id)).thenReturn(testMember);
        when(memberService.isJpaMode()).thenReturn(false);

        // Act
        ResponseEntity<MemberDTO> response = memberController.getMemberById(id);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(testMember.getId().toString(), response.getBody().getId());
        assertEquals(testMember.getName(), response.getBody().getName());
        verify(memberService, times(1)).getMemberById(id);
    }

    @Test
    void getMemberById_NonExistingId_ThrowsException() {
        // Arrange
        String id = "999";
        when(memberService.getMemberById(id))
            .thenThrow(new MemberNotFoundException("Member not found"));
        when(memberService.isJpaMode()).thenReturn(false);

        // Act & Assert
        assertThrows(MemberNotFoundException.class, () -> {
            memberController.getMemberById(id);
        });
        
        verify(memberService, times(1)).getMemberById(id);
    }

    @Test
    void updateMember_ValidInput_ReturnsUpdatedMember() {
        // Arrange
        String id = "1";
        when(memberFactory.createMember()).thenReturn(new MongoMember());
        
        MongoMember updatedMember = MongoMember.builder()
                .id(id)
                .name("Updated Name")
                .email("updated@example.com")
                .phoneNumber("+19876543210")
                .build();
        
        MemberDTO updatedMemberDTO = MemberDTO.builder()
                .id(id)
                .name("Updated Name")
                .email("updated@example.com")
                .phoneNumber("+19876543210")
                .build();
        
        when(memberService.updateMember(eq(id), any())).thenReturn(updatedMember);
        when(memberService.isJpaMode()).thenReturn(false);

        // Act
        ResponseEntity<MemberDTO> response = memberController.updateMember(id, updatedMemberDTO);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(updatedMember.getId().toString(), response.getBody().getId());
        assertEquals(updatedMember.getName(), response.getBody().getName());
        assertEquals(updatedMember.getEmail(), response.getBody().getEmail());
        verify(memberService, times(1)).updateMember(eq(id), any());
    }

    @Test
    void deleteMember_ExistingId_DeletesSuccessfully() {
        // Arrange
        String id = "1";
        when(memberService.deleteMember(id)).thenReturn(true);
        when(memberService.isJpaMode()).thenReturn(false);

        // Act
        ResponseEntity<Void> response = memberController.deleteMember(id);

        // Assert
        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(memberService, times(1)).deleteMember(id);
    }

    @Test
    void searchMembers_ReturnsMatchingMembers() {
        // Arrange
        List<IMember> matchingMembers = Arrays.asList(testMember);
        when(memberService.searchMembers("Test")).thenReturn(matchingMembers);

        // Act
        ResponseEntity<List<MemberDTO>> response = memberController.searchMembers("Test");

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().size());
        assertEquals(testMember.getId().toString(), response.getBody().get(0).getId());
        assertEquals(testMember.getName(), response.getBody().get(0).getName());
        verify(memberService, times(1)).searchMembers("Test");
    }
} 