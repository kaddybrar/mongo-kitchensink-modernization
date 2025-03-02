// src/main/java/com/mongo/kitchensink/repository/MemberRepository.java
package com.mongo.kitchensink.repository;

import com.mongo.kitchensink.model.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository interface for Member entities.
 * Provides methods for CRUD operations and custom queries.
 */
@Repository
public interface MemberRepository extends JpaRepository<Member, Long> {
    
    /**
     * Checks if a member with the given email exists.
     *
     * @param email the email to check
     * @return true if a member with the email exists, false otherwise
     */
    boolean existsByEmail(String email);
    
    /**
     * Finds members whose names contain the given string (case-insensitive).
     *
     * @param name the name to search for
     * @return list of members whose names contain the search string
     */
    List<Member> findByNameContainingIgnoreCase(String name);
}