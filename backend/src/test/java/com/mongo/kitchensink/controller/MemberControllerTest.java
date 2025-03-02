package com.mongo.kitchensink.controller;

import com.mongo.kitchensink.base.BaseControllerTest;
import com.mongo.kitchensink.exception.DuplicateEmailException;
import com.mongo.kitchensink.exception.MemberNotFoundException;
import com.mongo.kitchensink.model.Member;
import com.mongo.kitchensink.service.MemberService;
import com.mongo.kitchensink.util.TestDataFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.http.MediaType;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for the MemberController class.
 * These tests verify the behavior of the REST API endpoints.
 */
public class MemberControllerTest extends BaseControllerTest {

    @Mock
    private MemberService memberService;

    private MemberController memberController;
    private Member testMember;

    @BeforeEach
    void setUp() {
        // Create controller with mocked service
        memberController = new MemberController(memberService);
        
        // Set up MockMvc using the base class method
        setupMockMvc(memberController);
        
        // Create test data
        testMember = TestDataFactory.createValidMember();
        testMember.setId(1L);
    }

    @Test
    void createMember_ValidInput_ReturnsCreatedMember() throws Exception {
        // Arrange
        when(memberService.createMember(any(Member.class))).thenReturn(testMember);

        // Act & Assert
        mockMvc.perform(post("/api/v1/members")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testMember)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.name", is(testMember.getName())))
                .andExpect(jsonPath("$.email", is(testMember.getEmail())))
                .andExpect(jsonPath("$.phoneNumber", is(testMember.getPhoneNumber())));
        
        verify(memberService, times(1)).createMember(any(Member.class));
    }

    @Test
    void createMember_DuplicateEmail_ReturnsConflict() throws Exception {
        // Arrange
        when(memberService.createMember(any(Member.class)))
            .thenThrow(new DuplicateEmailException("Email already exists"));

        // Act & Assert
        mockMvc.perform(post("/api/v1/members")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testMember)))
                .andExpect(status().isConflict());
        
        verify(memberService, times(1)).createMember(any(Member.class));
    }

    @Test
    void createMember_InvalidInput_ReturnsBadRequest() throws Exception {
        // Arrange
        Member invalidMember = Member.builder()
                .email("invalid-email")  // Invalid email format
                .build();  // Missing required name

        // Act & Assert
        mockMvc.perform(post("/api/v1/members")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidMember)))
                .andExpect(status().isBadRequest());
        
        verify(memberService, never()).createMember(any(Member.class));
    }

    @Test
    void getAllMembers_ReturnsAllMembers() throws Exception {
        // Arrange
        List<Member> members = Arrays.asList(
                testMember,
                Member.builder().id(2L).name("Second User").email("second@example.com").build()
        );
        when(memberService.getAllMembers()).thenReturn(members);

        // Act & Assert
        mockMvc.perform(get("/api/v1/members"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].id", is(1)))
                .andExpect(jsonPath("$[0].name", is(testMember.getName())))
                .andExpect(jsonPath("$[1].id", is(2)))
                .andExpect(jsonPath("$[1].name", is("Second User")));
        
        verify(memberService, times(1)).getAllMembers();
    }

    @Test
    void getAllMembers_NoMembers_ReturnsEmptyArray() throws Exception {
        // Arrange
        when(memberService.getAllMembers()).thenReturn(Collections.emptyList());

        // Act & Assert
        mockMvc.perform(get("/api/v1/members"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
        
        verify(memberService, times(1)).getAllMembers();
    }

    @Test
    void getMemberById_ExistingId_ReturnsMember() throws Exception {
        // Arrange
        when(memberService.getMemberById(anyLong())).thenReturn(testMember);

        // Act & Assert
        mockMvc.perform(get("/api/v1/members/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.name", is(testMember.getName())))
                .andExpect(jsonPath("$.email", is(testMember.getEmail())));
        
        verify(memberService, times(1)).getMemberById(1L);
    }

    @Test
    void getMemberById_NonExistingId_ReturnsNotFound() throws Exception {
        // Arrange
        when(memberService.getMemberById(anyLong()))
            .thenThrow(new MemberNotFoundException("Member not found"));

        // Act & Assert
        mockMvc.perform(get("/api/v1/members/999"))
                .andExpect(status().isNotFound());
        
        verify(memberService, times(1)).getMemberById(999L);
    }

    @Test
    void updateMember_ValidInput_ReturnsUpdatedMember() throws Exception {
        // Arrange
        Member updatedMember = Member.builder()
                .id(1L)
                .name("Updated Name")
                .email("updated@example.com")
                .phoneNumber("+19876543210")
                .build();
        
        when(memberService.updateMember(anyLong(), any(Member.class))).thenReturn(updatedMember);

        // Act & Assert
        mockMvc.perform(put("/api/v1/members/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updatedMember)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.name", is(updatedMember.getName())))
                .andExpect(jsonPath("$.email", is(updatedMember.getEmail())))
                .andExpect(jsonPath("$.phoneNumber", is(updatedMember.getPhoneNumber())));
        
        verify(memberService, times(1)).updateMember(eq(1L), any(Member.class));
    }

    @Test
    void updateMember_NonExistingId_ReturnsNotFound() throws Exception {
        // Arrange
        Member updatedMember = Member.builder()
                .name("Updated Name")
                .email("updated@example.com")
                .phoneNumber("+19876543210")
                .build();
        
        when(memberService.updateMember(anyLong(), any(Member.class)))
            .thenThrow(new MemberNotFoundException("Member not found"));

        // Act & Assert
        mockMvc.perform(put("/api/v1/members/999")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updatedMember)))
                .andExpect(status().isNotFound());
        
        verify(memberService, times(1)).updateMember(eq(999L), any(Member.class));
    }

    @Test
    void updateMember_DuplicateEmail_ReturnsConflict() throws Exception {
        // Arrange
        Member updatedMember = Member.builder()
                .name("Updated Name")
                .email("duplicate@example.com")
                .phoneNumber("+19876543210")
                .build();
        
        when(memberService.updateMember(anyLong(), any(Member.class)))
            .thenThrow(new DuplicateEmailException("Email already exists"));

        // Act & Assert
        mockMvc.perform(put("/api/v1/members/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updatedMember)))
                .andExpect(status().isConflict());
        
        verify(memberService, times(1)).updateMember(eq(1L), any(Member.class));
    }

    @Test
    void updateMember_InvalidInput_ReturnsBadRequest() throws Exception {
        // Arrange
        Member invalidMember = Member.builder()
                .email("invalid-email")  // Invalid email format
                .build();  // Missing required name

        // Act & Assert
        mockMvc.perform(put("/api/v1/members/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidMember)))
                .andExpect(status().isBadRequest());
        
        verify(memberService, never()).updateMember(anyLong(), any(Member.class));
    }

    @Test
    void deleteMember_ExistingId_ReturnsNoContent() throws Exception {
        // Arrange
        doNothing().when(memberService).deleteMember(anyLong());

        // Act & Assert
        mockMvc.perform(delete("/api/v1/members/1"))
                .andExpect(status().isNoContent());
        
        verify(memberService, times(1)).deleteMember(1L);
    }

    @Test
    void deleteMember_NonExistingId_ReturnsNotFound() throws Exception {
        // Arrange
        doThrow(new MemberNotFoundException("Member not found"))
            .when(memberService).deleteMember(anyLong());

        // Act & Assert
        mockMvc.perform(delete("/api/v1/members/999"))
                .andExpect(status().isNotFound());
        
        verify(memberService, times(1)).deleteMember(999L);
    }

    @Test
    void searchMembers_ReturnsMatchingMembers() throws Exception {
        // Arrange
        List<Member> matchingMembers = Arrays.asList(testMember);
        when(memberService.searchMembers(anyString())).thenReturn(matchingMembers);

        // Act & Assert
        mockMvc.perform(get("/api/v1/members/search?name=Test"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id", is(1)))
                .andExpect(jsonPath("$[0].name", is(testMember.getName())));
        
        verify(memberService, times(1)).searchMembers("Test");
    }

    @Test
    void searchMembers_NoMatches_ReturnsEmptyArray() throws Exception {
        // Arrange
        when(memberService.searchMembers(anyString())).thenReturn(Collections.emptyList());

        // Act & Assert
        mockMvc.perform(get("/api/v1/members/search?name=NonExistent"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
        
        verify(memberService, times(1)).searchMembers("NonExistent");
    }

    @Test
    void searchMembers_MissingNameParameter_ReturnsBadRequest() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/v1/members/search"))
                .andExpect(status().isBadRequest());
        
        verify(memberService, never()).searchMembers(anyString());
    }
} 