package com.mongo.kitchensink.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongo.kitchensink.exception.DuplicateEmailException;
import com.mongo.kitchensink.exception.GlobalExceptionHandler;
import com.mongo.kitchensink.exception.MemberNotFoundException;
import com.mongo.kitchensink.model.Member;
import com.mongo.kitchensink.service.MemberService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

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
public class MemberControllerTest {

    private MockMvc mockMvc;

    @Mock
    private MemberService memberService;

    @InjectMocks
    private MemberController memberController;

    private ObjectMapper objectMapper = new ObjectMapper();
    private Member testMember;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        
        // Set up MockMvc with GlobalExceptionHandler and validator
        mockMvc = MockMvcBuilders.standaloneSetup(memberController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(new LocalValidatorFactoryBean())
                .build();

        testMember = Member.builder()
                .id(1L)
                .name("Test User")
                .email("test@example.com")
                .phoneNumber("+12345678901")
                .build();
    }

    @Test
    void createMember_ValidInput_ReturnsCreatedMember() throws Exception {
        when(memberService.createMember(any(Member.class))).thenReturn(testMember);

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
        when(memberService.createMember(any(Member.class)))
            .thenThrow(new DuplicateEmailException("Email already exists"));

        mockMvc.perform(post("/api/v1/members")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testMember)))
                .andExpect(status().isConflict());
        
        verify(memberService, times(1)).createMember(any(Member.class));
    }

    @Test
    void createMember_InvalidInput_ReturnsBadRequest() throws Exception {
        Member invalidMember = Member.builder()
                .email("invalid-email")  // Invalid email format
                .build();  // Missing required name

        mockMvc.perform(post("/api/v1/members")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidMember)))
                .andExpect(status().isBadRequest());
        
        verify(memberService, never()).createMember(any(Member.class));
    }

    @Test
    void getAllMembers_ReturnsAllMembers() throws Exception {
        List<Member> members = Arrays.asList(
                testMember,
                Member.builder().id(2L).name("Second User").email("second@example.com").build()
        );
        when(memberService.getAllMembers()).thenReturn(members);

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
        when(memberService.getAllMembers()).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/v1/members"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
        
        verify(memberService, times(1)).getAllMembers();
    }

    @Test
    void getMemberById_ExistingId_ReturnsMember() throws Exception {
        when(memberService.getMemberById(anyLong())).thenReturn(testMember);

        mockMvc.perform(get("/api/v1/members/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.name", is(testMember.getName())))
                .andExpect(jsonPath("$.email", is(testMember.getEmail())));
        
        verify(memberService, times(1)).getMemberById(1L);
    }

    @Test
    void getMemberById_NonExistingId_ReturnsNotFound() throws Exception {
        when(memberService.getMemberById(anyLong()))
            .thenThrow(new MemberNotFoundException("Member not found"));

        mockMvc.perform(get("/api/v1/members/999"))
                .andExpect(status().isNotFound());
        
        verify(memberService, times(1)).getMemberById(999L);
    }

    @Test
    void updateMember_ValidInput_ReturnsUpdatedMember() throws Exception {
        Member updatedMember = Member.builder()
                .id(1L)
                .name("Updated Name")
                .email("updated@example.com")
                .phoneNumber("+19876543210")
                .build();
        
        when(memberService.updateMember(anyLong(), any(Member.class))).thenReturn(updatedMember);

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
        Member updatedMember = Member.builder()
                .name("Updated Name")
                .email("updated@example.com")
                .phoneNumber("+19876543210")
                .build();
        
        when(memberService.updateMember(anyLong(), any(Member.class)))
            .thenThrow(new MemberNotFoundException("Member not found"));

        mockMvc.perform(put("/api/v1/members/999")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updatedMember)))
                .andExpect(status().isNotFound());
        
        verify(memberService, times(1)).updateMember(eq(999L), any(Member.class));
    }

    @Test
    void updateMember_DuplicateEmail_ReturnsConflict() throws Exception {
        Member updatedMember = Member.builder()
                .name("Updated Name")
                .email("duplicate@example.com")
                .phoneNumber("+19876543210")
                .build();
        
        when(memberService.updateMember(anyLong(), any(Member.class)))
            .thenThrow(new DuplicateEmailException("Email already exists"));

        mockMvc.perform(put("/api/v1/members/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updatedMember)))
                .andExpect(status().isConflict());
        
        verify(memberService, times(1)).updateMember(eq(1L), any(Member.class));
    }

    @Test
    void updateMember_InvalidInput_ReturnsBadRequest() throws Exception {
        Member invalidMember = Member.builder()
                .email("invalid-email")  // Invalid email format
                .build();  // Missing required name

        mockMvc.perform(put("/api/v1/members/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidMember)))
                .andExpect(status().isBadRequest());
        
        verify(memberService, never()).updateMember(anyLong(), any(Member.class));
    }

    @Test
    void deleteMember_ExistingId_ReturnsNoContent() throws Exception {
        doNothing().when(memberService).deleteMember(anyLong());

        mockMvc.perform(delete("/api/v1/members/1"))
                .andExpect(status().isNoContent());
        
        verify(memberService, times(1)).deleteMember(1L);
    }

    @Test
    void deleteMember_NonExistingId_ReturnsNotFound() throws Exception {
        doThrow(new MemberNotFoundException("Member not found"))
            .when(memberService).deleteMember(anyLong());

        mockMvc.perform(delete("/api/v1/members/999"))
                .andExpect(status().isNotFound());
        
        verify(memberService, times(1)).deleteMember(999L);
    }

    @Test
    void searchMembers_ReturnsMatchingMembers() throws Exception {
        List<Member> matchingMembers = Arrays.asList(testMember);
        when(memberService.searchMembers(anyString())).thenReturn(matchingMembers);

        mockMvc.perform(get("/api/v1/members/search?name=Test"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id", is(1)))
                .andExpect(jsonPath("$[0].name", is(testMember.getName())));
        
        verify(memberService, times(1)).searchMembers("Test");
    }

    @Test
    void searchMembers_NoMatches_ReturnsEmptyArray() throws Exception {
        when(memberService.searchMembers(anyString())).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/v1/members/search?name=NonExistent"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
        
        verify(memberService, times(1)).searchMembers("NonExistent");
    }

    @Test
    void searchMembers_MissingNameParameter_ReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/v1/members/search"))
                .andExpect(status().isBadRequest());
        
        verify(memberService, never()).searchMembers(anyString());
    }
} 