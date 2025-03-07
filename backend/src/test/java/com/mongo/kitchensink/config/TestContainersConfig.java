package com.mongo.kitchensink.config;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import java.util.HashMap;
import java.util.Map;

public class TestContainersConfig {
    private static MongoDBContainer mongoDBContainer;
    private static PostgreSQLContainer<?> postgreSQLContainer;

    public static void startContainers() {
        if (mongoDBContainer == null || !mongoDBContainer.isRunning()) {
            mongoDBContainer = new MongoDBContainer(DockerImageName.parse("mongo:6.0.2"))
                    .withExposedPorts(27017);
            mongoDBContainer.start();
        }

        if (postgreSQLContainer == null || !postgreSQLContainer.isRunning()) {
            postgreSQLContainer = new PostgreSQLContainer<>(DockerImageName.parse("postgres:15-alpine"))
                    .withExposedPorts(5432)
                    .withDatabaseName("testdb");
            postgreSQLContainer.start();
        }
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        startContainers();

        // Configure MongoDB connection
        registry.add("spring.data.mongodb.uri", () -> 
            String.format("mongodb://%s:%d/testdb", 
                mongoDBContainer.getHost(), 
                mongoDBContainer.getMappedPort(27017)));
        registry.add("spring.data.mongodb.database", () -> "testdb");

        // Configure PostgreSQL connection
        registry.add("spring.datasource.url", postgreSQLContainer::getJdbcUrl);
        registry.add("spring.datasource.username", postgreSQLContainer::getUsername);
        registry.add("spring.datasource.password", postgreSQLContainer::getPassword);
        registry.add("spring.datasource.driver-class-name", postgreSQLContainer::getDriverClassName);
        
        // Additional configurations
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        registry.add("spring.jpa.properties.hibernate.dialect", () -> "org.hibernate.dialect.PostgreSQLDialect");
        registry.add("spring.data.mongodb.auto-index-creation", () -> "true");
        
        // Disable H2
        registry.add("spring.datasource.h2.enabled", () -> "false");
    }

    public static Map<String, String> getTestProperties() {
        if (mongoDBContainer == null || !mongoDBContainer.isRunning() ||
            postgreSQLContainer == null || !postgreSQLContainer.isRunning()) {
            startContainers();
        }

        Map<String, String> properties = new HashMap<>();
        
        // MongoDB properties
        properties.put("spring.data.mongodb.uri", 
            String.format("mongodb://%s:%d/testdb", 
                mongoDBContainer.getHost(), 
                mongoDBContainer.getMappedPort(27017)));
        properties.put("spring.data.mongodb.database", "testdb");

        // PostgreSQL properties
        properties.put("spring.datasource.url", postgreSQLContainer.getJdbcUrl());
        properties.put("spring.datasource.username", postgreSQLContainer.getUsername());
        properties.put("spring.datasource.password", postgreSQLContainer.getPassword());
        properties.put("spring.datasource.driver-class-name", postgreSQLContainer.getDriverClassName());

        return properties;
    }

    public static void stopContainers() {
        if (mongoDBContainer != null && mongoDBContainer.isRunning()) {
            mongoDBContainer.stop();
            mongoDBContainer = null;
        }
        if (postgreSQLContainer != null && postgreSQLContainer.isRunning()) {
            postgreSQLContainer.stop();
            postgreSQLContainer = null;
        }
    }
} 