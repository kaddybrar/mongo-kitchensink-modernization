package com.mongo.kitchensink.service;

import com.mongo.kitchensink.model.IMember;
import com.mongo.kitchensink.model.JpaMember;
import com.mongo.kitchensink.model.MongoMember;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class DualWriteMemberServiceTest {

    @Mock
    private JpaMemberService jpaMemberService;

    @Mock
    private MongoMemberService mongoMemberService;

    @InjectMocks
    private DualWriteMemberService dualWriteMemberService;

    private JpaMember testJpaMember;
    private MongoMember testMongoMember;

    @BeforeEach
    void setUp() {
        // Set up test data
        testJpaMember = JpaMember.builder()
                .id(1L)
                .name("Test User")
                .email("test@example.com")
                .phoneNumber("+12345678901")
                .build();

        testMongoMember = MongoMember.builder()
                .id("1")
                .name("Test User")
                .email("test@example.com")
                .phoneNumber("+12345678901")
                .build();
        
        // Set the read source to JPA by default
        ReflectionTestUtils.setField(dualWriteMemberService, "readSource", "jpa");
    }

    @Test
    void createMember_WritesToBothDatabases() {
        // Arrange
        JpaMember inputMember = new JpaMember();
        inputMember.setName("Test User");
        inputMember.setEmail("test@example.com");
        
        // Use doReturn instead of when to avoid strict stubbing issues
        doReturn(testJpaMember).when(jpaMemberService).createMember(any(IMember.class));
        doReturn(testMongoMember).when(mongoMemberService).createMember(any(IMember.class));

        // Act
        IMember result = dualWriteMemberService.createMember(inputMember);

        // Assert
        assertNotNull(result);
        // Convert both to string for comparison
        assertEquals(testJpaMember.getId().toString(), result.getId().toString());
        assertEquals(testJpaMember.getName(), result.getName());
        
        verify(jpaMemberService).createMember(any(IMember.class));
        verify(mongoMemberService).createMember(any(IMember.class));
    }

    @Test
    void createMember_JpaFailure_RollsBack() {
        // Arrange
        JpaMember inputMember = new JpaMember();
        inputMember.setName("Test User");
        inputMember.setEmail("test@example.com");
        
        doThrow(new RuntimeException("JPA failure")).when(jpaMemberService).createMember(any(IMember.class));

        // Act & Assert
        assertThrows(RuntimeException.class, () -> {
            dualWriteMemberService.createMember(inputMember);
        });
        
        verify(jpaMemberService).createMember(any(IMember.class));
        verify(mongoMemberService, never()).createMember(any());
    }

    @Test
    void createMember_MongoFailure_RollsBack() {
        // Arrange
        JpaMember inputMember = new JpaMember();
        inputMember.setName("Test User");
        inputMember.setEmail("test@example.com");
        
        doReturn(testJpaMember).when(jpaMemberService).createMember(any(IMember.class));
        doThrow(new RuntimeException("Mongo failure")).when(mongoMemberService).createMember(any(IMember.class));
        doReturn(true).when(jpaMemberService).deleteMember(anyString());
        
        // Act & Assert
        assertThrows(RuntimeException.class, () -> {
            dualWriteMemberService.createMember(inputMember);
        });
        
        verify(jpaMemberService).createMember(any(IMember.class));
        verify(mongoMemberService).createMember(any(IMember.class));
        // Verify rollback
        verify(jpaMemberService).deleteMember("1");
    }

    @Test
    void getAllMembers_JpaReadSource_ReturnsJpaMembers() {
        // Arrange
        List<IMember> jpaMembers = Arrays.asList(testJpaMember);
        when(jpaMemberService.getAllMembers()).thenReturn(jpaMembers);
        ReflectionTestUtils.setField(dualWriteMemberService, "readSource", "jpa");

        // Act
        List<IMember> result = dualWriteMemberService.getAllMembers();

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        // Use toString() to compare IDs
        assertEquals(testJpaMember.getId().toString(), result.get(0).getId().toString());
        
        verify(jpaMemberService).getAllMembers();
        verify(mongoMemberService, never()).getAllMembers();
    }

    @Test
    void getAllMembers_MongoReadSource_ReturnsMongoMembers() {
        // Arrange
        List<IMember> mongoMembers = Arrays.asList(testMongoMember);
        when(mongoMemberService.getAllMembers()).thenReturn(mongoMembers);
        ReflectionTestUtils.setField(dualWriteMemberService, "readSource", "mongo");

        // Act
        List<IMember> result = dualWriteMemberService.getAllMembers();

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(testMongoMember.getId(), result.get(0).getId());
        
        verify(mongoMemberService).getAllMembers();
        verify(jpaMemberService, never()).getAllMembers();
    }

    @Test
    void getMemberById_JpaReadSource_ReturnsJpaMember() {
        // Arrange
        String id = "1";
        when(jpaMemberService.getMemberById(id)).thenReturn(testJpaMember);
        ReflectionTestUtils.setField(dualWriteMemberService, "readSource", "jpa");

        // Act
        IMember result = dualWriteMemberService.getMemberById(id);

        // Assert
        assertNotNull(result);
        // Use toString() to compare IDs
        assertEquals(testJpaMember.getId().toString(), result.getId().toString());
        
        verify(jpaMemberService).getMemberById(id);
        verify(mongoMemberService, never()).getMemberById(any());
    }

    @Test
    void getMemberById_MongoReadSource_ReturnsMongoMember() {
        // Arrange
        String id = "1";
        when(mongoMemberService.getMemberById(id)).thenReturn(testMongoMember);
        ReflectionTestUtils.setField(dualWriteMemberService, "readSource", "mongo");

        // Act
        IMember result = dualWriteMemberService.getMemberById(id);

        // Assert
        assertNotNull(result);
        assertEquals(testMongoMember.getId(), result.getId());
        
        verify(mongoMemberService).getMemberById(id);
        verify(jpaMemberService, never()).getMemberById(any());
    }

    @Test
    void updateMember_WritesToBothDatabases() {
        // Arrange
        String id = "1";
        JpaMember inputMember = new JpaMember();
        inputMember.setName("Updated User");
        inputMember.setEmail("updated@example.com");
        
        // Use doReturn instead of when to avoid strict stubbing issues
        doReturn(testJpaMember).when(jpaMemberService).updateMember(eq(id), any(IMember.class));
        doReturn(testMongoMember).when(mongoMemberService).updateMember(eq(id), any(IMember.class));
        ReflectionTestUtils.setField(dualWriteMemberService, "readSource", "jpa");

        // Act
        IMember result = dualWriteMemberService.updateMember(id, inputMember);

        // Assert
        assertNotNull(result);
        // Use toString() to compare IDs
        assertEquals(testJpaMember.getId().toString(), result.getId().toString());
        
        verify(jpaMemberService).updateMember(eq(id), any(IMember.class));
        verify(mongoMemberService).updateMember(eq(id), any(IMember.class));
    }

    @Test
    void updateMember_JpaFailure_RollsBack() {
        // Arrange
        String id = "1";
        JpaMember inputMember = new JpaMember();
        inputMember.setName("Updated User");
        inputMember.setEmail("updated@example.com");
        
        doThrow(new RuntimeException("JPA failure")).when(jpaMemberService).updateMember(eq(id), any(IMember.class));

        // Act & Assert
        assertThrows(RuntimeException.class, () -> {
            dualWriteMemberService.updateMember(id, inputMember);
        });
        
        verify(jpaMemberService).updateMember(eq(id), any(IMember.class));
        verify(mongoMemberService, never()).updateMember(any(), any());
    }

    @Test
    void deleteMember_DeletesFromBothDatabases() {
        // Arrange
        String id = "1";
        when(jpaMemberService.deleteMember(id)).thenReturn(true);
        when(mongoMemberService.deleteMember(id)).thenReturn(true);

        // Act
        boolean result = dualWriteMemberService.deleteMember(id);

        // Assert
        assertTrue(result);
        
        verify(jpaMemberService).deleteMember(id);
        verify(mongoMemberService).deleteMember(id);
    }

    @Test
    void deleteMember_JpaFailure_ReturnsFalse() {
        // Arrange
        String id = "1";
        when(jpaMemberService.deleteMember(id)).thenReturn(false);
        // Make sure mongoMemberService is not called
        lenient().when(mongoMemberService.deleteMember(id)).thenReturn(true);

        // Act
        boolean result = dualWriteMemberService.deleteMember(id);

        // Assert
        assertFalse(result);
        
        verify(jpaMemberService).deleteMember(id);
        verify(mongoMemberService, never()).deleteMember(any());
    }

    @Test
    void searchMembers_JpaReadSource_ReturnsJpaMembers() {
        // Arrange
        String searchTerm = "Test";
        List<IMember> jpaMembers = Arrays.asList(testJpaMember);
        when(jpaMemberService.searchMembers(searchTerm)).thenReturn(jpaMembers);
        ReflectionTestUtils.setField(dualWriteMemberService, "readSource", "jpa");

        // Act
        List<IMember> result = dualWriteMemberService.searchMembers(searchTerm);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        // Use toString() to compare IDs
        assertEquals(testJpaMember.getId().toString(), result.get(0).getId().toString());
        
        verify(jpaMemberService).searchMembers(searchTerm);
        verify(mongoMemberService, never()).searchMembers(any());
    }

    @Test
    void searchMembers_MongoReadSource_ReturnsMongoMembers() {
        // Arrange
        String searchTerm = "Test";
        List<IMember> mongoMembers = Arrays.asList(testMongoMember);
        when(mongoMemberService.searchMembers(searchTerm)).thenReturn(mongoMembers);
        ReflectionTestUtils.setField(dualWriteMemberService, "readSource", "mongo");

        // Act
        List<IMember> result = dualWriteMemberService.searchMembers(searchTerm);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(testMongoMember.getId(), result.get(0).getId());
        
        verify(mongoMemberService).searchMembers(searchTerm);
        verify(jpaMemberService, never()).searchMembers(any());
    }

    @Test
    void isJpaMode_JpaReadSource_ReturnsTrue() {
        // Arrange
        ReflectionTestUtils.setField(dualWriteMemberService, "readSource", "jpa");

        // Act
        boolean result = dualWriteMemberService.isJpaMode();

        // Assert
        assertTrue(result);
    }

    @Test
    void isJpaMode_MongoReadSource_ReturnsFalse() {
        // Arrange
        ReflectionTestUtils.setField(dualWriteMemberService, "readSource", "mongo");

        // Act
        boolean result = dualWriteMemberService.isJpaMode();

        // Assert
        assertFalse(result);
    }
}