package com.mongo.kitchensink.service;

import com.mongo.kitchensink.config.DatabaseSelector;
import com.mongo.kitchensink.model.IMember;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

/**
 * Service that implements dual-write strategy for gradual migration from JPA to MongoDB.
 * Writes operations are performed on both databases, while read operations use the configured primary source.
 */
@Service
@Slf4j
public class DualWriteMemberService implements IMemberService {

    private final JpaMemberService jpaService;
    private final MongoMemberService mongoService;
    
    @Value("${app.database.read.source:jpa}")
    private String readSource;
    
    @Value("${app.dual.write.enabled:true}")
    private boolean dualWriteEnabled;
    
    @Value("${app.dual.write.compare:true}")
    private boolean compareResults;

    public DualWriteMemberService(
            JpaMemberService jpaService,
            MongoMemberService mongoService) {
        this.jpaService = jpaService;
        this.mongoService = mongoService;
    }

    /**
     * Determines which service to use for read operations based on configuration.
     * 
     * @return the service to use for reads
     */
    private IMemberService getReadService() {
        return "mongo".equalsIgnoreCase(readSource) ? mongoService : jpaService;
    }

    @Override
    public List<IMember> getAllMembers() {
        log.debug("Getting all members from {}", readSource);
        
        List<IMember> members = getReadService().getAllMembers();
        
        // Optionally compare results for validation
        if (compareResults && !readSource.equalsIgnoreCase("mongo")) {
            try {
                List<IMember> mongoMembers = mongoService.getAllMembers();
                compareAllMembers(members, mongoMembers);
            } catch (Exception e) {
                log.error("Error comparing MongoDB results for getAllMembers", e);
            }
        }
        
        return members;
    }

    @Override
    public IMember getMemberById(String id) {
        log.debug("Getting member by ID from {}", readSource);
        
        IMember member = getReadService().getMemberById(id);
        
        // Optionally compare results for validation
        if (compareResults && !readSource.equalsIgnoreCase("mongo")) {
            try {
                IMember mongoMember = mongoService.getMemberById(id);
                compareMembers(member, mongoMember, id);
            } catch (Exception e) {
                log.error("Error comparing MongoDB result for getMemberById: {}", id, e);
            }
        }
        
        return member;
    }

    @Override
    public IMember createMember(IMember member) {
        // First, create in JPA
        IMember jpaMember = jpaService.createMember(member);
        
        try {
            // Then, create in MongoDB
            IMember mongoMember = mongoService.createMember(member);
            // Return the member from the configured read source
            return "mongo".equalsIgnoreCase(readSource) ? mongoMember : jpaMember;    
        } catch (Exception e) {
            // If MongoDB creation fails, rollback JPA creation
            jpaService.deleteMember(jpaMember.getId().toString());
            throw e;
        }   
    }

    @Override
    public IMember updateMember(String id, IMember updatedMember) {
        log.debug("Updating member in both databases");
        
        // First update in JPA
        IMember updatedJpaMember = jpaService.updateMember(id, updatedMember);
        
        // Then update in MongoDB with the same ID
        IMember mongoMember = updatedJpaMember.toMongoMember();
        mongoService.updateMember(id, mongoMember);
        
        // Return the member from the configured read source
        return "mongo".equalsIgnoreCase(readSource) ? mongoMember : updatedJpaMember;
    }

    @Override
    public boolean deleteMember(String id) {
        log.debug("Deleting member from both databases");
        try {
            // Delete from both databases
            boolean jpaResult = jpaService.deleteMember(id);
            if(jpaResult) {
                return mongoService.deleteMember(id);
            }
            return false;
        } catch (Exception e) {
            log.error("Error deleting member with ID {}", id, e);
            return false;
        }
    }

    @Override
    public List<IMember> searchMembers(String name) {
        log.debug("Searching members by name from {}", readSource);
        
        List<IMember> members = getReadService().searchMembers(name);
        
        // Optionally compare results for validation
        if (compareResults && !readSource.equalsIgnoreCase("mongo")) {
            try {
                List<IMember> mongoMembers = mongoService.searchMembers(name);
                compareAllMembers(members, mongoMembers);
            } catch (Exception e) {
                log.error("Error comparing MongoDB results for searchMembers: {}", name, e);
            }
        }
        
        return members;
    }
    
    /**
     * Compares members from different data sources and logs any discrepancies.
     * 
     * @param member1 First member to compare
     * @param member2 Second member to compare
     * @param id Member ID for logging
     */
    private void compareMembers(IMember member1, IMember member2, String id) {
        if (member1 == null && member2 == null) {
            return;
        }
        
        if (member1 == null || member2 == null) {
            log.warn("Member with ID {} exists in one database but not the other", id);
            return;
        }
        
        if (!Objects.equals(member1.getName(), member2.getName())) {
            log.warn("Name mismatch for member {}: {} vs {}", id, member1.getName(), member2.getName());
        }
        
        if (!Objects.equals(member1.getEmail(), member2.getEmail())) {
            log.warn("Email mismatch for member {}: {} vs {}", id, member1.getEmail(), member2.getEmail());
        }
        
        if (!Objects.equals(member1.getPhoneNumber(), member2.getPhoneNumber())) {
            log.warn("Phone number mismatch for member {}: {} vs {}", id, member1.getPhoneNumber(), member2.getPhoneNumber());
        }
    }

    /**
     * Compares two lists of members and logs any differences.
     *
     * @param list1 the first list
     * @param list2 the second list
     */
    private void compareAllMembers(List<IMember> list1, List<IMember> list2) {
        if (list1.size() != list2.size()) {
            log.warn("Member count mismatch: {} vs {}", list1.size(), list2.size());
        }
        
        // Create a map of members by ID for easier comparison
        java.util.Map<String, IMember> map1 = new java.util.HashMap<>();
        for (IMember member : list1) {
            map1.put(member.getId().toString(), member);
        }
        
        // Compare each member in list2 with its counterpart in list1
        for (IMember member2 : list2) {
            String id = member2.getId().toString();
            IMember member1 = map1.get(id);
            
            if (member1 == null) {
                log.warn("Member with ID {} exists in one database but not the other", id);
                continue;
            }
            
            try {
                compareMembers(member1, member2, id);
            } catch (NumberFormatException e) {
                log.warn("Could not convert ID {} to Long for comparison", id);
                // Compare without the ID
                compareMembers(member1, member2, null);
            }
            
            // Remove the member from map1 to track which members are only in list1
            map1.remove(id);
        }
        
        // Log any members that are only in list1
        for (String id : map1.keySet()) {
            log.warn("Member with ID {} exists in one database but not the other", id);
        }
    }

    @Override
    public boolean isJpaMode() {
        // In dual-write mode, we consider it JPA mode if the read source is JPA
        return !"mongo".equalsIgnoreCase(readSource);
    }

} 
    