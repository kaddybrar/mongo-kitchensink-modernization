package com.mongo.kitchensink.controller;

import com.mongo.kitchensink.dto.MemberDTO;
import com.mongo.kitchensink.model.IMember;
import com.mongo.kitchensink.model.MemberFactory;
import com.mongo.kitchensink.service.IMemberService;
import com.mongo.kitchensink.exception.ErrorResponse;
import com.mongo.kitchensink.exception.MemberNotFoundException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * REST controller for managing Member operations.
 * Provides endpoints for CRUD operations and member search functionality.
 */
@RestController
@RequestMapping("/api/v1/members")
@Slf4j
@Tag(name = "Member Management", description = "APIs for managing members")
public class MemberController {
    private final IMemberService memberService;
    private final MemberFactory memberFactory;

    /**
     * Constructs a new MemberController with the specified MemberService.
     *
     * @param memberService the service handling member business logic
     * @param memberFactory the factory for creating member entities
     */
    public MemberController(IMemberService memberService, MemberFactory memberFactory) {
        this.memberService = memberService;
        this.memberFactory = memberFactory;
    }

    @Operation(summary = "Get all members")
    @ApiResponse(responseCode = "200", description = "List of all members",
            content = @Content(schema = @Schema(implementation = MemberDTO.class)))
    @GetMapping
    public ResponseEntity<List<MemberDTO>> getAllMembers() {
        List<MemberDTO> members = memberService.getAllMembers().stream()
                .map(MemberDTO::fromMember)
                .collect(Collectors.toList());
        return ResponseEntity.ok(members);
    }

    @Operation(summary = "Create a new member")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Member created successfully",
                content = @Content(schema = @Schema(implementation = MemberDTO.class))),
        @ApiResponse(responseCode = "400", description = "Invalid input",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping
    public ResponseEntity<MemberDTO> createMember(@Valid @RequestBody MemberDTO memberDTO) {
        // Convert DTO to domain model using the appropriate implementation
        IMember member = memberFactory.createMember();
        member.setName(memberDTO.getName());
        member.setEmail(memberDTO.getEmail());
        member.setPhoneNumber(memberDTO.getPhoneNumber());
        
        // Create the member
        IMember createdMember = memberService.createMember(member);
        
        // Convert back to DTO
        return ResponseEntity.ok(MemberDTO.fromMember(createdMember));
    }

    @Operation(summary = "Get a member by ID")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Member found",
                content = @Content(schema = @Schema(implementation = MemberDTO.class))),
        @ApiResponse(responseCode = "404", description = "Member not found",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/{id}")
    public ResponseEntity<MemberDTO> getMemberById(@PathVariable String id) {
        try {
            // For JPA, validate if ID can be converted to Long
            if (memberService.isJpaMode() && !isValidLongId(id)) {
                return ResponseEntity.badRequest().build();
            }
            
            IMember member = memberService.getMemberById(id);
            return ResponseEntity.ok(MemberDTO.fromMember(member));
        } catch (MemberNotFoundException e) {
            // Let the GlobalExceptionHandler handle this
            throw e;
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Operation(summary = "Update a member")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Member updated successfully",
                content = @Content(schema = @Schema(implementation = MemberDTO.class))),
        @ApiResponse(responseCode = "400", description = "Invalid input",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "404", description = "Member not found",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PutMapping("/{id}")
    public ResponseEntity<MemberDTO> updateMember(@PathVariable String id, @RequestBody MemberDTO memberDTO) {
        try {
            // For JPA, validate if ID can be converted to Long
            if (memberService.isJpaMode() && !isValidLongId(id)) {
                return ResponseEntity.badRequest().build();
            }
            
            // Convert DTO to domain model using the factory
            IMember member = memberFactory.createMember();
            member.setName(memberDTO.getName());
            member.setEmail(memberDTO.getEmail());
            member.setPhoneNumber(memberDTO.getPhoneNumber());
            
            // Update the member
            IMember updatedMember = memberService.updateMember(id, member);
            if (updatedMember != null) {
                return ResponseEntity.ok(MemberDTO.fromMember(updatedMember));
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Operation(summary = "Delete a member")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Member deleted successfully"),
        @ApiResponse(responseCode = "404", description = "Member not found",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteMember(@PathVariable String id) {
        try {
            // For JPA, validate if ID can be converted to Long
            if (memberService.isJpaMode() && !isValidLongId(id)) {
                return ResponseEntity.badRequest().build();
            }
            
            boolean deleted = memberService.deleteMember(id);
            if (deleted) {
                return ResponseEntity.noContent().build();
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Operation(summary = "Search members by name")
    @ApiResponse(responseCode = "200", description = "List of members matching the search criteria",
            content = @Content(schema = @Schema(implementation = MemberDTO.class)))
    @GetMapping("/search")
    public ResponseEntity<List<MemberDTO>> searchMembers(
            @Parameter(description = "Name to search for") 
            @RequestParam String name) {
        List<MemberDTO> members = memberService.searchMembers(name).stream()
                .map(MemberDTO::fromMember)
                .collect(Collectors.toList());
        return ResponseEntity.ok(members);
    }

    /**
     * Validates if a string can be converted to a valid Long value
     * @param id the string to validate
     * @return true if valid, false otherwise
     */
    private boolean isValidLongId(String id) {
        try {
            Long.parseLong(id);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}