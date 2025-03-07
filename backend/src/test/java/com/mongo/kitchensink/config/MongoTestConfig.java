package com.mongo.kitchensink.config;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * MongoDB test configuration.
 * This class configures MongoDB for testing with TestContainers.
 */
@TestConfiguration
@Profile("test")
@Import(MongoAutoConfiguration.class)
public class MongoTestConfig {

    /**
     * Creates a MongoDB container for testing.
     *
     * @return the MongoDB container
     */
    @Bean(initMethod = "start", destroyMethod = "stop")
    public MongoDBContainer mongoDBContainer() {
        MongoDBContainer container = new MongoDBContainer(DockerImageName.parse("mongo:4.4.6"));
        return container;
    }

    /**
     * Creates MongoDB client settings.
     *
     * @param mongoDBContainer the MongoDB container
     * @return the MongoDB client settings
     */
    @Bean
    @Primary
    public MongoClientSettings mongoClientSettings(MongoDBContainer mongoDBContainer) {
        ConnectionString connectionString = new ConnectionString(mongoDBContainer.getConnectionString());
        return MongoClientSettings.builder()
                .applyConnectionString(connectionString)
                .build();
    }

    /**
     * Creates a MongoDB client for testing.
     *
     * @param mongoClientSettings the MongoDB client settings
     * @return the MongoDB client
     */
    @Bean
    @Primary
    public MongoClient mongoClient(MongoClientSettings mongoClientSettings) {
        return MongoClients.create(mongoClientSettings);
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