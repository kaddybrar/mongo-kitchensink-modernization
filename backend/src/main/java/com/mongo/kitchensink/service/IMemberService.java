package com.mongo.kitchensink.service;

import com.mongo.kitchensink.exception.DuplicateEmailException;
import com.mongo.kitchensink.exception.MemberNotFoundException;
import com.mongo.kitchensink.model.IMember;

import java.util.List;

/**
 * Interface for Member service operations.
 */
public interface IMemberService {
    
    /**
     * Retrieves all members.
     *
     * @return list of all members
     */
    List<IMember> getAllMembers();
    
    /**
     * Creates a new member.
     *
     * @param member the member to create
     * @return the created member
     * @throws DuplicateEmailException if the email already exists
     */
    IMember createMember(IMember member);
    
    /**
     * Gets a member by ID.
     *
     * @param id the ID of the member to retrieve
     * @return the member with the specified ID
     */
    IMember getMemberById(String id);
    
    /**
     * Updates an existing member.
     *
     * @param id the ID of the member to update
     * @param updatedMember the updated member details
     * @return the updated member
     * @throws MemberNotFoundException if the member is not found
     * @throws DuplicateEmailException if the email already exists
     */
    IMember updateMember(String id, IMember updatedMember);
    
    /**
     * Deletes a member by ID.
     *
     * @param id the ID of the member to delete
     * @return true if deleted successfully, false otherwise
     */
    boolean deleteMember(String id);
    
    /**
     * Searches for members by name.
     *
     * @param name the name to search for
     * @return list of members whose names contain the search string
     */
    List<IMember> searchMembers(String name);
    
    /**
     * Checks if the service is operating in JPA mode.
     *
     * @return true if in JPA mode, false otherwise (MongoDB mode)
     */
    boolean isJpaMode();
    
  
} 