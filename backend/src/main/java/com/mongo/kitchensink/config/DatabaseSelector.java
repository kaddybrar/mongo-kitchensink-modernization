package com.mongo.kitchensink.config;

import com.mongo.kitchensink.model.IMember;
import com.mongo.kitchensink.service.DualWriteMemberService;
import com.mongo.kitchensink.service.IMemberService;
import com.mongo.kitchensink.service.JpaMemberService;
import com.mongo.kitchensink.service.MongoMemberService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * Configuration class for selecting the database implementation.
 */
@Configuration
public class DatabaseSelector {

    @Value("${app.migration.strategy:direct}")
    private String migrationStrategy;

    @Value("${app.database.type:jpa}")
    private String databaseType;

    @Value("${app.dual-write.enabled:false}")
    private boolean dualWriteEnabled;

    @Value("${app.database.read.source:jpa}")
    private String readSource;

    @Value("${app.dual-write.compare:false}")
    private boolean compareResults;

    @Autowired
    private JpaMemberService jpaMemberService;

    @Autowired
    private MongoMemberService mongoMemberService;

    /**
     * Selects the appropriate member service based on configuration.
     *
     * @return the selected member service
     */
    @Bean
    @Primary
    public IMemberService memberService() {
        if ("dual-write".equalsIgnoreCase(migrationStrategy) && dualWriteEnabled) {
            DualWriteMemberService service = new DualWriteMemberService(jpaMemberService, mongoMemberService);
            // The service will get readSource and compareResults from @Value annotations
            return service;
        } else if ("mongo".equalsIgnoreCase(databaseType)) {
            return mongoMemberService;
        } else {
            return jpaMemberService;
        }
    }

    /**
     * Ensures MongoDB IDs are derived from JPA IDs during migration.
     * This method is used by the DualWriteMemberService to maintain ID consistency.
     *
     * @param member the member from JPA
     * @return the member with consistent ID for MongoDB
     */
    public IMember ensureConsistentId(IMember member) {
        if (member == null || member.getId() == null) {
            return member;
        }
        
        // If this is a JPA member, ensure its MongoDB counterpart uses the same ID
        if (member.getClass().getSimpleName().contains("Jpa")) {
            return member;
        } 
        // If this is a MongoDB member, ensure it uses the JPA ID
        else if (member.getClass().getSimpleName().contains("Mongo")) {
            String jpaId = member.getId().toString();
            if (!jpaId.equals(member.getId().toString())) {
                member.setId(jpaId);
            }
            return member;
        }
        
        return member;
    }
} 