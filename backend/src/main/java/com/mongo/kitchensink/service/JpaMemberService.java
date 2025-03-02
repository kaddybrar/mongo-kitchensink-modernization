package com.mongo.kitchensink.service;

import com.mongo.kitchensink.exception.DuplicateEmailException;
import com.mongo.kitchensink.exception.MemberNotFoundException;
import com.mongo.kitchensink.model.IMember;
import com.mongo.kitchensink.model.JpaMember;
import com.mongo.kitchensink.repository.JpaMemberRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

import java.util.List;
import java.util.stream.Collectors;

/**
 * JPA Service class for Member operations.
 * Provides business logic for member management using a relational database.
 */
@Service
@Transactional
@Slf4j
@Qualifier("jpaMemberService")
@ConditionalOnProperty(name = "app.database.type", havingValue = "jpa")
public class JpaMemberService implements IMemberService {
    private final JpaMemberRepository jpaMemberRepository;

    /**
     * Constructs a new JpaMemberService with the specified repository.
     *
     * @param jpaMemberRepository the repository for member data access
     */
    public JpaMemberService(JpaMemberRepository jpaMemberRepository) {
        this.jpaMemberRepository = jpaMemberRepository;
    }

    /**
     * Creates a new member after validating email uniqueness.
     *
     * @param member the member to create
     * @return the created member
     * @throws DuplicateEmailException if the email already exists
     */
    @Override
    public IMember createMember(IMember member) {
        JpaMember jpaMember = member.toJpaMember();
        
        if (jpaMemberRepository.existsByEmail(jpaMember.getEmail())) {
            throw new DuplicateEmailException("Email already exists");
        }
        log.debug("Creating new member: {}", jpaMember.getEmail());
        return jpaMemberRepository.save(jpaMember);
    }

    /**
     * Retrieves all members from the database.
     *
     * @return list of all members
     */
    @Override
    public List<IMember> getAllMembers() {
        return jpaMemberRepository.findAll().stream()
                .map(member -> (IMember) member)
                .collect(Collectors.toList());
    }

    /**
     * Retrieves a member by their ID.
     *
     * @param id the ID of the member to retrieve as a String
     * @return the member with the given ID
     * @throws MemberNotFoundException if the member is not found
     */
    @Override
    public IMember getMemberById(String id) {
        try {
            Long longId = Long.parseLong(id);
            return jpaMemberRepository.findById(longId)
                    .orElseThrow(() -> new MemberNotFoundException("Member not found with id: " + id));
        } catch (NumberFormatException e) {
            throw new MemberNotFoundException("Invalid ID format for JPA: " + id);
        }
    }

    /**
     * Updates an existing member's information.
     *
     * @param id the ID of the member to update as a String
     * @param updatedMember the updated member details
     * @return the updated member
     * @throws MemberNotFoundException if the member is not found
     * @throws DuplicateEmailException if the email already exists
     */
    @Override
    public IMember updateMember(String id, IMember updatedMember) {
        try {
            Long longId = Long.parseLong(id);
            // Check if member exists
            JpaMember jpaMember = jpaMemberRepository.findById(longId)
                    .orElseThrow(() -> new MemberNotFoundException("Member not found with id: " + id));
            
            // Convert to JPA entity
            JpaMember jpaUpdatedMember = updatedMember.toJpaMember();
            
            // Check if email is being changed and if it's already in use
            if (!jpaMember.getEmail().equals(jpaUpdatedMember.getEmail()) && 
                jpaMemberRepository.existsByEmail(jpaUpdatedMember.getEmail())) {
                throw new DuplicateEmailException("Email already exists");
            }
            
            // Set the ID to ensure we're updating the right record
            jpaUpdatedMember.setId(longId);
            
            // Save and return
            return jpaMemberRepository.save(jpaUpdatedMember);
        } catch (NumberFormatException e) {
            throw new MemberNotFoundException("Invalid ID format for JPA: " + id);
        }
    }

    /**
     * Deletes a member by their ID.
     *
     * @param id the ID of the member to delete as a String
     * @return true if deleted successfully, false otherwise
     */
    @Override
    public boolean deleteMember(String id) {
        try {
            Long longId = Long.parseLong(id);
            // Check if member exists
            if (!jpaMemberRepository.existsById(longId)) {
                return false;
            }
            
            // Delete
            jpaMemberRepository.deleteById(longId);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * Searches for members by name (case-insensitive partial match).
     *
     * @param name the name to search for
     * @return list of members whose names contain the search string
     */
    @Override
    public List<IMember> searchMembers(String name) {
        return jpaMemberRepository.findByNameContainingIgnoreCase(name).stream()
                .map(member -> (IMember) member)
                .collect(Collectors.toList());
    }

    @Override
    public boolean isJpaMode() {
        return true; // This is the JPA implementation, so always return true
    }
} 