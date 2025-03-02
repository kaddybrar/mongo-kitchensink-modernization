package com.mongo.kitchensink.config;

import com.mongo.kitchensink.model.MongoMember;
import com.mongo.kitchensink.repository.MongoMemberRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.IndexDefinition;
import org.springframework.data.mongodb.core.index.IndexOperations;
import org.springframework.data.mongodb.core.index.IndexResolver;
import org.springframework.data.mongodb.core.index.MongoPersistentEntityIndexResolver;
import org.springframework.data.mongodb.core.mapping.MongoMappingContext;

import java.util.List;

@Configuration
public class MongoInitializer {

    @Autowired
    private MongoMemberRepository mongoMemberRepository;

    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private MongoMappingContext mongoMappingContext;

    /**
     * Initializes MongoDB with sample data and creates indexes.
     *
     * @return a CommandLineRunner that executes the initialization
     */
    @Bean
    public CommandLineRunner initMongo() {
        return args -> {
            try {
                // Create indexes
                IndexOperations indexOps = mongoTemplate.indexOps(MongoMember.class);
                IndexResolver resolver = new MongoPersistentEntityIndexResolver(mongoMappingContext);
                Iterable<? extends IndexDefinition> indexDefinitions = resolver.resolveIndexFor(MongoMember.class);
                indexDefinitions.forEach(indexOps::ensureIndex);

                // Insert sample data if collection is empty
                if (mongoMemberRepository.count() == 0) {
                    // Create sample members
                    MongoMember member1 = new MongoMember();
                    member1.setId("1");  // Use string representation of JPA ID
                    member1.setName("John Doe");
                    member1.setEmail("john.doe@example.com");
                    member1.setPhoneNumber("+12345678901");

                    MongoMember member2 = new MongoMember();
                    member2.setId("2");  // Use string representation of JPA ID
                    member2.setName("Jane Smith");
                    member2.setEmail("jane.smith@example.com");
                    member2.setPhoneNumber("+19876543210");

                    MongoMember member3 = new MongoMember();
                    member3.setId("3");  // Use string representation of JPA ID
                    member3.setName("Bob Johnson");
                    member3.setEmail("bob.johnson@example.com");
                    member3.setPhoneNumber("+15551234567");

                    // Save to repository
                    mongoMemberRepository.saveAll(List.of(member1, member2, member3));
                }
            } catch (Exception e) {
                // Log the error but don't prevent application startup
                System.err.println("Error initializing MongoDB: " + e.getMessage());
            }
        };
    }
} 