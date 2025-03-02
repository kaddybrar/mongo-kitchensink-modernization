package com.mongo.kitchensink.repository;

import com.mongo.kitchensink.model.MongoMember;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for MongoDB Member entities.
 * Provides methods to interact with the MongoDB database.
 */
@Repository
public interface MongoMemberRepository extends MongoRepository<MongoMember, String> {
    
    /**
     * Finds a member by email.
     *
     * @param email the email to search for
     * @return the member with the given email, or empty if no member exists with that email
     */
    Optional<MongoMember> findByEmail(String email);
    
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
    @Query("{ 'name': { $regex: ?0, $options: 'i' } }")
    List<MongoMember> findByNameContainingIgnoreCase(String name);
} 