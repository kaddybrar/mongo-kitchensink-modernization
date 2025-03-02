package com.mongo.kitchensink.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.mongo.kitchensink.exception.GlobalExceptionHandler;
import com.mongo.kitchensink.model.IMember;
import com.mongo.kitchensink.model.JpaMember;
import com.mongo.kitchensink.model.MemberFactory;
import com.mongo.kitchensink.service.DualWriteMemberService;
import com.mongo.kitchensink.util.TestDataFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Arrays;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for the MemberController with DualWrite implementation.
 */
@ExtendWith(MockitoExtension.class)
public class DualWriteMemberControllerTest {

    @Mock
    private DualWriteMemberService memberService;

    @Mock
    private MemberFactory memberFactory;

    private MemberController memberController;
    private JpaMember testMember;
    private ObjectMapper objectMapper;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        // Create controller with mocked service
        memberController = new MemberController(memberService, memberFactory);
        
        // Create a custom ObjectMapper that can handle IMember interface
        objectMapper = new ObjectMapper();
        SimpleModule module = new SimpleModule();
        module.addAbstractTypeMapping(IMember.class, JpaMember.class);
        objectMapper.registerModule(module);
        
        // Create a custom message converter with our ObjectMapper
        MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter();
        converter.setObjectMapper(objectMapper);
        
        // Set up MockMvc with the controller and our custom converter
        mockMvc = MockMvcBuilders.standaloneSetup(memberController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setMessageConverters(converter)
                .build();
        
        // Create test data
        testMember = TestDataFactory.createValidJpaMember();
        testMember.setId(1L);
    }

    @Test
    void createMember_ValidInput_ReturnsCreatedMember() throws Exception {
        // Arrange
        when(memberFactory.createMember()).thenReturn(new JpaMember());
        doReturn(testMember).when(memberService).createMember(any());

        // Act & Assert
        mockMvc.perform(post("/api/v1/members")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testMember)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("1"))
                .andExpect(jsonPath("$.name").value(testMember.getName()))
                .andExpect(jsonPath("$.email").value(testMember.getEmail()))
                .andExpect(jsonPath("$.phoneNumber").value(testMember.getPhoneNumber()));
        
        verify(memberService, times(1)).createMember(any());
    }

    @Test
    void getAllMembers_ReturnsAllMembers() throws Exception {
        // Arrange
        List<IMember> members = Arrays.asList(
                testMember,
                JpaMember.builder().id(2L).name("Second User").email("second@example.com").build()
        );
        when(memberService.getAllMembers()).thenReturn(members);

        // Act & Assert
        mockMvc.perform(get("/api/v1/members"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].id").value("1"))
                .andExpect(jsonPath("$[1].id").value("2"));
        
        verify(memberService, times(1)).getAllMembers();
    }

    @Test
    void getMemberById_ExistingId_ReturnsMember() throws Exception {
        // Arrange
        String id = "1";
        when(memberService.getMemberById(id)).thenReturn(testMember);
        when(memberService.isJpaMode()).thenReturn(true);

        // Act & Assert
        mockMvc.perform(get("/api/v1/members/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("1"))
                .andExpect(jsonPath("$.name").value(testMember.getName()))
                .andExpect(jsonPath("$.email").value(testMember.getEmail()));
        
        verify(memberService, times(1)).getMemberById(id);
    }

    @Test
    void updateMember_ValidInput_ReturnsUpdatedMember() throws Exception {
        // Arrange
        String id = "1";
        when(memberFactory.createMember()).thenReturn(new JpaMember());
        
        JpaMember updatedMember = JpaMember.builder()
                .id(1L)
                .name("Updated Name")
                .email("updated@example.com")
                .phoneNumber("+19876543210")
                .build();
        
        when(memberService.updateMember(eq(id), any())).thenReturn(updatedMember);
        when(memberService.isJpaMode()).thenReturn(true);

        // Act & Assert
        mockMvc.perform(put("/api/v1/members/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updatedMember)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("1"))
                .andExpect(jsonPath("$.name").value(updatedMember.getName()))
                .andExpect(jsonPath("$.email").value(updatedMember.getEmail()))
                .andExpect(jsonPath("$.phoneNumber").value(updatedMember.getPhoneNumber()));
        
        verify(memberService, times(1)).updateMember(eq(id), any());
    }

    @Test
    void deleteMember_ExistingId_ReturnsNoContent() throws Exception {
        // Arrange
        String id = "1";
        when(memberService.deleteMember(id)).thenReturn(true);
        when(memberService.isJpaMode()).thenReturn(true);

        // Act & Assert
        mockMvc.perform(delete("/api/v1/members/1"))
                .andExpect(status().isNoContent());
        
        verify(memberService, times(1)).deleteMember(id);
    }

    @Test
    void searchMembers_ReturnsMatchingMembers() throws Exception {
        // Arrange
        List<IMember> matchingMembers = Arrays.asList(testMember);
        when(memberService.searchMembers("John")).thenReturn(matchingMembers);

        // Act & Assert
        mockMvc.perform(get("/api/v1/members/search?name=John"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value("1"))
                .andExpect(jsonPath("$[0].name").value(testMember.getName()));
        
        verify(memberService, times(1)).searchMembers("John");
    }

    // Add more tests for dual-write specific behavior
} 