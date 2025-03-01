package com.mongo.kitchensink.controller;

import com.mongo.kitchensink.model.Member;
import com.mongo.kitchensink.service.MemberService;
import com.mongo.kitchensink.exception.ErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for managing Member operations.
 * Provides endpoints for CRUD operations and member search functionality.
 */
@RestController
@RequestMapping("/api/v1/members")
@Slf4j
@Tag(name = "Member", description = "Member management APIs")
public class MemberController {
    private final MemberService memberService;

    /**
     * Constructs a new MemberController with the specified MemberService.
     *
     * @param memberService the service handling member business logic
     */
    public MemberController(MemberService memberService) {
        this.memberService = memberService;
    }

    @Operation(summary = "Create a new member")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Member created successfully",
                content = @Content(schema = @Schema(implementation = Member.class))),
        @ApiResponse(responseCode = "400", description = "Invalid input"),
        @ApiResponse(responseCode = "409", description = "Email already exists",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping
    public ResponseEntity<Member> createMember(@Valid @RequestBody Member member) {
        log.info("Creating new member");
        return ResponseEntity.ok(memberService.createMember(member));
    }

    @Operation(summary = "Get all members")
    @ApiResponse(responseCode = "200", description = "List all members",
            content = @Content(schema = @Schema(implementation = Member.class)))
    @GetMapping
    public ResponseEntity<List<Member>> getAllMembers() {
        return ResponseEntity.ok(memberService.getAllMembers());
    }

    @Operation(summary = "Get a member by ID")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Found the member",
                content = @Content(schema = @Schema(implementation = Member.class))),
        @ApiResponse(responseCode = "404", description = "Member not found",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/{id}")
    public ResponseEntity<Member> getMemberById(
            @Parameter(description = "ID of member to be searched") 
            @PathVariable Long id) {
        return ResponseEntity.ok(memberService.getMemberById(id));
    }

    @Operation(summary = "Update a member")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Member updated successfully",
                content = @Content(schema = @Schema(implementation = Member.class))),
        @ApiResponse(responseCode = "404", description = "Member not found",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid input")
    })
    @PutMapping("/{id}")
    public ResponseEntity<Member> updateMember(
            @Parameter(description = "ID of member to be updated") 
            @PathVariable Long id,
            @Valid @RequestBody Member memberDetails) {
        return ResponseEntity.ok(memberService.updateMember(id, memberDetails));
    }

    @Operation(summary = "Delete a member")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Member deleted successfully"),
        @ApiResponse(responseCode = "404", description = "Member not found",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteMember(
            @Parameter(description = "ID of member to be deleted") 
            @PathVariable Long id) {
        memberService.deleteMember(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Search members by name")
    @ApiResponse(responseCode = "200", description = "List of members matching the search criteria",
            content = @Content(schema = @Schema(implementation = Member.class)))
    @GetMapping("/search")
    public ResponseEntity<List<Member>> searchMembers(
            @Parameter(description = "Name to search for") 
            @RequestParam String name) {
        return ResponseEntity.ok(memberService.searchMembers(name));
    }
}