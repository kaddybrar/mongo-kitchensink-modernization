package com.mongo.kitchensink.config;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import com.mongo.kitchensink.repository.JpaMemberRepository;
import com.mongo.kitchensink.repository.MongoMemberRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Propagation;
import jakarta.annotation.PreDestroy;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages test data cleanup and tracking for integration tests.
 * Provides methods to ensure clean test data state between test executions.
 */
@Component
public class TestDataManager {
    private static final Logger logger = LoggerFactory.getLogger(TestDataManager.class);

    // Thread-safe storage for tracking IDs during test execution
    private final ConcurrentHashMap<Long, ThreadLocal<Set<String>>> threadMongoIds = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, ThreadLocal<Set<Long>>> threadJpaIds = new ConcurrentHashMap<>();

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    private JpaMemberRepository jpaMemberRepository;

    @Autowired
    private MongoMemberRepository mongoMemberRepository;

    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private final TransactionTemplate transactionTemplate;

    @Autowired
    public TestDataManager(PlatformTransactionManager transactionManager) {
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    /**
     * Cleans up test data from both databases.
     * Only removes data that was created during test execution.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void cleanupAllTestData() {
        try {
            Set<String> mongoIds = getTrackedMongoIds();
            Set<Long> jpaIds = getTrackedJpaIds();

            if (!mongoIds.isEmpty() || !jpaIds.isEmpty()) {
                // Clean tracked data from both databases
                cleanupTrackedMongoData(mongoIds);
                cleanupTrackedJpaData(jpaIds);
                
                // Clear tracking data
                clearTrackedIds();
                
                logger.debug("Test data cleanup successful - Removed {} MongoDB and {} JPA entities", 
                    mongoIds.size(), jpaIds.size());
            } else {
                logger.debug("No test data to clean up");
            }
        } catch (Exception e) {
            logger.error("Error during test data cleanup", e);
            throw e;
        }
    }

    /**
     * Removes specific MongoDB entities.
     */
    private void cleanupTrackedMongoData(Set<String> mongoIds) {
        if (!mongoIds.isEmpty()) {
            try {
                for (String id : mongoIds) {
                    if (mongoMemberRepository.existsById(id)) {
                        mongoMemberRepository.deleteById(id);
                        logger.debug("Deleted MongoDB entity with ID: {}", id);
                    } else {
                        logger.debug("MongoDB entity with ID {} already removed", id);
                    }
                }
            } catch (Exception e) {
                logger.error("Error cleaning MongoDB test data", e);
                throw e;
            }
        }
    }

    /**
     * Removes specific JPA entities.
     */
    private void cleanupTrackedJpaData(Set<Long> jpaIds) {
        if (!jpaIds.isEmpty()) {
            try {
                transactionTemplate.execute(status -> {
                    try {
                        jdbcTemplate.execute("SET CONSTRAINTS ALL DEFERRED");
                        
                        for (Long id : jpaIds) {
                            if (jpaMemberRepository.existsById(id)) {
                                jpaMemberRepository.deleteById(id);
                                logger.debug("Deleted JPA entity with ID: {}", id);
                            } else {
                                logger.debug("JPA entity with ID {} already removed", id);
                            }
                        }
                        
                        jdbcTemplate.execute("SET CONSTRAINTS ALL IMMEDIATE");
                        entityManager.flush();
                        
                        return null;
                    } catch (Exception e) {
                        status.setRollbackOnly();
                        throw e;
                    }
                });
            } catch (Exception e) {
                logger.error("Error cleaning JPA test data", e);
                throw e;
            }
        }
    }

    /**
     * Tracks a MongoDB ID for cleanup.
     */
    public void trackMongoId(String id) {
        if (id != null) {
            getMongoIdsForCurrentThread().add(id);
            logger.debug("Tracked MongoDB ID: {}", id);
        }
    }

    /**
     * Tracks a JPA ID for cleanup.
     */
    public void trackJpaId(Long id) {
        if (id != null) {
            getJpaIdsForCurrentThread().add(id);
            logger.debug("Tracked JPA ID: {}", id);
        }
    }

    /**
     * Gets the tracked MongoDB IDs for the current thread.
     */
    public Set<String> getTrackedMongoIds() {
        return Collections.unmodifiableSet(getMongoIdsForCurrentThread());
    }

    /**
     * Gets the tracked JPA IDs for the current thread.
     */
    public Set<Long> getTrackedJpaIds() {
        return Collections.unmodifiableSet(getJpaIdsForCurrentThread());
    }

    /**
     * Clears tracked IDs for the current thread.
     */
    public void clearTrackedIds() {
        long threadId = Thread.currentThread().getId();
        ThreadLocal<Set<String>> mongoIds = threadMongoIds.get(threadId);
        ThreadLocal<Set<Long>> jpaIds = threadJpaIds.get(threadId);

        if (mongoIds != null) mongoIds.get().clear();
        if (jpaIds != null) jpaIds.get().clear();

        logger.debug("Cleared tracked IDs for thread: {}", threadId);
    }

    /**
     * Removes tracking data for the current thread.
     */
    public void removeTracking() {
        long threadId = Thread.currentThread().getId();
        threadMongoIds.remove(threadId);
        threadJpaIds.remove(threadId);
        logger.debug("Removed tracking for thread: {}", threadId);
    }

    @PreDestroy
    public void cleanup() {
        threadMongoIds.clear();
        threadJpaIds.clear();
        logger.debug("Cleaned up all thread-local resources");
    }

    private Set<String> getMongoIdsForCurrentThread() {
        long threadId = Thread.currentThread().getId();
        return threadMongoIds.computeIfAbsent(threadId, k -> ThreadLocal.withInitial(HashSet::new)).get();
    }

    private Set<Long> getJpaIdsForCurrentThread() {
        long threadId = Thread.currentThread().getId();
        return threadJpaIds.computeIfAbsent(threadId, k -> ThreadLocal.withInitial(HashSet::new)).get();
    }
} 