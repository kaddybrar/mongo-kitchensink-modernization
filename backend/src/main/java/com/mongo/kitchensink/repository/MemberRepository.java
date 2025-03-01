// src/main/java/com/mongo/kitchensink/repository/MemberRepository.java
package com.mongo.kitchensink.repository;

import com.mongo.kitchensink.model.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MemberRepository extends JpaRepository<Member, Long> {
    boolean existsByEmail(String email);
    List<Member> findByNameContainingIgnoreCase(String name);
}