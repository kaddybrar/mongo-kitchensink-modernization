package com.mongo.kitchensink.service;

import com.mongo.kitchensink.exception.DuplicateEmailException;
import com.mongo.kitchensink.exception.MemberNotFoundException;
import com.mongo.kitchensink.model.MongoMember;
import com.mongo.kitchensink.model.IMember;
import com.mongo.kitchensink.repository.MongoMemberRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Service class for MongoDB Member operations.
 * Provides business logic for member management.
 */
@Service
@Slf4j
@Qualifier("mongoMemberService")
public class MongoMemberService implements IMemberService {

    private final MongoMemberRepository mongoMemberRepository;

    /**
     * Constructs a new MongoMemberService with the specified repository.
     *
     * @param mongoMemberRepository the repository for MongoDB member data access
     */
    public MongoMemberService(MongoMemberRepository mongoMemberRepository) {
        this.mongoMemberRepository = mongoMemberRepository;
    }

    /**
     * Retrieves all members.
     *
     * @return list of all members
     */
    @Override
    public List<IMember> getAllMembers() {
        return mongoMemberRepository.findAll().stream()
                .map(member -> (IMember) member)
                .collect(Collectors.toList());
    }

    /**
     * Creates a new member.
     *
     * @param member the member to create
     * @return the created member
     * @throws DuplicateEmailException if the email already exists
     */
    @Override
    public IMember createMember(IMember member) {
        if (mongoMemberRepository.existsByEmail(member.getEmail())) {
            throw new DuplicateEmailException("Email already exists");
        }
        log.debug("Creating new member: {}", member.getEmail());
        
        MongoMember mongoMember = member.toMongoMember();
        MongoMember savedMember = mongoMemberRepository.save(mongoMember);
        return savedMember;
    }

    /**
     * Retrieves a member by ID.
     *
     * @param id the ID of the member to retrieve as a String
     * @return the member with the given ID
     * @throws MemberNotFoundException if the member is not found
     */
    @Override
    public IMember getMemberById(String id) {
        return mongoMemberRepository.findById(id)
                .orElseThrow(() -> new MemberNotFoundException("Member not found with id: " + id));
    }

    /**
     * Updates an existing member.
     *
     * @param id the ID of the member to update as a String
     * @param updatedMember the updated member details
     * @return the updated member
     * @throws MemberNotFoundException if the member is not found
     * @throws DuplicateEmailException if the email already exists
     */
    @Override
    public IMember updateMember(String id, IMember updatedMember) {
        // Check if member exists
        MongoMember existingMember = mongoMemberRepository.findById(id)
                .orElseThrow(() -> new MemberNotFoundException("Member not found with id: " + id));
        
        // Convert to MongoDB entity
        MongoMember mongoMember = updatedMember.toMongoMember();
        
        // Check if email is being changed and if it's already in use
        if (!existingMember.getEmail().equals(mongoMember.getEmail()) && 
            mongoMemberRepository.existsByEmail(mongoMember.getEmail())) {
            throw new DuplicateEmailException("Email already exists");
        }
        
        // Set the ID to ensure we're updating the right record
        mongoMember.setId(id);
        
        // Save and return
        return mongoMemberRepository.save(mongoMember);
    }

    /**
     * Deletes a member by ID.
     *
     * @param id the ID of the member to delete as a String
     * @return true if deleted successfully, false otherwise
     */
    @Override
    public boolean deleteMember(String id) {
        // Check if member exists
        if (!mongoMemberRepository.existsById(id)) {
            return false;
        }
        
        // Delete
        mongoMemberRepository.deleteById(id);
        return true;
    }

    /**
     * Searches for members by name.
     *
     * @param name the name to search for
     * @return list of members whose names contain the search string
     */
    @Override
    public List<IMember> searchMembers(String name) {
        return mongoMemberRepository.findByNameContainingIgnoreCase(name).stream()
                .map(member -> (IMember) member)
                .collect(Collectors.toList());
    }

    @Override
    public boolean isJpaMode() {
        return false; // This is the MongoDB implementation, so always return false
    }

} 