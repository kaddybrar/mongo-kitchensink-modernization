package com.mongo.kitchensink.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.utility.DockerImageName;
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration;
import org.springframework.context.annotation.Import;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;

/**
 * Test configuration for integration tests.
 */
@TestConfiguration
@Profile("test")
@Import(MongoAutoConfiguration.class)
public class TestConfig {

    /**
     * Creates a MongoDB container for testing.
     *
     * @return the MongoDB container
     */
    @Bean(initMethod = "start", destroyMethod = "stop")
    public MongoDBContainer mongoDBContainer() {
        MongoDBContainer container = new MongoDBContainer(DockerImageName.parse("mongo:4.4.6"));
        container.start();
        return container;
    }

    /**
     * Creates a MongoDB client for testing.
     *
     * @param mongoDBContainer the MongoDB container
     * @return the MongoDB client
     */
    @Bean
    @Primary
    public MongoClient mongoClient(MongoDBContainer mongoDBContainer) {
        String connectionString = mongoDBContainer.getConnectionString();
        return MongoClients.create(connectionString);
    }

    /**
     * Creates a MongoDB template for testing.
     *
     * @param mongoClient the MongoDB client
     * @return the MongoDB template
     */
    @Bean
    @Primary
    public MongoTemplate mongoTemplate(MongoClient mongoClient) {
        return new MongoTemplate(mongoClient, "test");
    }
} 