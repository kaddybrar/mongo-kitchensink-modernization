package com.mongo.kitchensink.repository;

import com.mongo.kitchensink.model.JpaMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * JPA Repository interface for Member entities.
 * Provides methods to interact with the relational database.
 */
@Repository
public interface JpaMemberRepository extends JpaRepository<JpaMember, Long> {
    
    /**
     * Finds a member by email.
     *
     * @param email the email to search for
     * @return the member with the given email, or empty if no member exists with that email
     */
    Optional<JpaMember> findByEmail(String email);
    
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
    List<JpaMember> findByNameContainingIgnoreCase(String name);
} 