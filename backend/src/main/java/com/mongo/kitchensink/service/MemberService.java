package com.mongo.kitchensink.service;

import com.mongo.kitchensink.exception.DuplicateEmailException;
import com.mongo.kitchensink.exception.MemberNotFoundException;
import com.mongo.kitchensink.model.Member;
import com.mongo.kitchensink.repository.MemberRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service class for managing member operations.
 * Handles business logic for member-related operations including CRUD and search functionality.
 */
@Service
@Transactional
@Slf4j
public class MemberService {
    private final MemberRepository memberRepository;

    /**
     * Constructs a new MemberService with the specified repository.
     *
     * @param memberRepository the repository for member data access
     */
    public MemberService(MemberRepository memberRepository) {
        this.memberRepository = memberRepository;
    }

    /**
     * Creates a new member after validating email uniqueness.
     *
     * @param member the member to create
     * @return the created member
     * @throws DuplicateEmailException if the email already exists
     */
    public Member createMember(Member member) {
        if (memberRepository.existsByEmail(member.getEmail())) {
            throw new DuplicateEmailException("Email already exists");
        }
        log.debug("Creating new member: {}", member.getEmail());
        return memberRepository.save(member);
    }

    /**
     * Retrieves all members from the database.
     *
     * @return list of all members
     */
    public List<Member> getAllMembers() {
        return memberRepository.findAll();
    }

    /**
     * Retrieves a specific member by their ID.
     *
     * @param id the ID of the member to retrieve
     * @return the requested member
     * @throws MemberNotFoundException if the member is not found
     */
    public Member getMemberById(Long id) {
        return memberRepository.findById(id)
            .orElseThrow(() -> new MemberNotFoundException("Member not found"));
    }

    /**
     * Updates an existing member's information.
     *
     * @param id the ID of the member to update
     * @param memberDetails the updated member details
     * @return the updated member
     * @throws MemberNotFoundException if the member is not found
     */
    public Member updateMember(Long id, Member memberDetails) {
        Member member = getMemberById(id);
        member.setName(memberDetails.getName());
        member.setPhoneNumber(memberDetails.getPhoneNumber());
        log.debug("Updating member: {}", member.getEmail());
        return memberRepository.save(member);
    }

    /**
     * Deletes a member by their ID.
     *
     * @param id the ID of the member to delete
     * @throws MemberNotFoundException if the member is not found
     */
    public void deleteMember(Long id) {
        if (!memberRepository.existsById(id)) {
            throw new MemberNotFoundException("Member not found");
        }
        memberRepository.deleteById(id);
    }

    /**
     * Searches for members by name (case-insensitive partial match).
     *
     * @param name the name to search for
     * @return list of members whose names contain the search string
     */
    public List<Member> searchMembers(String name) {
        return memberRepository.findByNameContainingIgnoreCase(name);
    }
}