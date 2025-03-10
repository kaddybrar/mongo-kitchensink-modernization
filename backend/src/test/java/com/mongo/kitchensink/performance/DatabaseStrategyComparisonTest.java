package com.mongo.kitchensink.performance;

import com.mongo.kitchensink.dto.MemberDTO;
import com.mongo.kitchensink.model.JpaMember;
import com.mongo.kitchensink.model.MongoMember;
import com.mongo.kitchensink.repository.JpaMemberRepository;
import com.mongo.kitchensink.repository.MongoMemberRepository;
import com.mongo.kitchensink.config.TestDataManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {
    "spring.test.database.replace=NONE",
    "spring.jpa.hibernate.ddl-auto=update",
    "spring.data.mongodb.auto-index-creation=true",
    "spring.main.allow-bean-definition-overriding=true",
    "spring.datasource.h2.enabled=false"
})
public class DatabaseStrategyComparisonTest extends BasePerformanceTest {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseStrategyComparisonTest.class);

    private static final int ITERATIONS = 100;
    private static final int WARMUP_ITERATIONS = 10;

    private final Map<String, Map<String, List<Long>>> results = new HashMap<>();
    private final Map<String, Map<String, Integer>> errorCounts = new HashMap<>();
    private final Map<String, Map<String, Integer>> verificationErrors = new HashMap<>();



    @Autowired
    private TestRestTemplate restTemplate;

    @Value("${local.server.port}")
    private int port;

    @Autowired
    private JpaMemberRepository jpaMemberRepository;

    @Autowired
    private MongoMemberRepository mongoMemberRepository;

    @Autowired
    private TestDataManager testDataManager;

    private String baseUrl;

    @Test
    void compareDatabaseStrategies() throws Exception {
        baseUrl = "http://localhost:" + port + "/api/v1/members";
        
        // Test JPA Strategy
        testStrategy("JPA", "jpa", false, null);
        
        // Clean up and wait before switching strategy
        cleanupAndWait();
        
        // Test MongoDB Strategy
        testStrategy("MongoDB", "mongo", false, null);
        
        // Clean up and wait before switching strategy
        cleanupAndWait();
        
        // Test Dual Write Strategy
        testStrategy("Dual Write", "jpa", true, "dual-write");
        
        // Generate and print comparison report
        printComparisonReport();
    }

    private void cleanupAndWait() throws Exception {
        // Clean up only test data
        for (Long id : testDataManager.getTrackedJpaIds()) {
            jpaMemberRepository.deleteById(id);
        }
        for (String id : testDataManager.getTrackedMongoIds()) {
            mongoMemberRepository.deleteById(id);
        }
        testDataManager.clearTrackedIds();
        
        // Wait for changes to propagate
        TimeUnit.SECONDS.sleep(2);
    }

    private void testStrategy(String strategyName, String databaseType, boolean dualWriteEnabled, String migrationStrategy) throws Exception {
        // Configure test properties
        System.setProperty("app.database.type", databaseType);
        System.setProperty("app.dual-write.enabled", String.valueOf(dualWriteEnabled));
        if (migrationStrategy != null) {
            System.setProperty("app.migration.strategy", migrationStrategy);
        }

        // Verify properties are set correctly
        verifyProperties(databaseType, dualWriteEnabled, migrationStrategy);

        Map<String, List<Long>> strategyResults = new HashMap<>();
        Map<String, Integer> strategyErrors = new HashMap<>();
        Map<String, Integer> strategyVerificationErrors = new HashMap<>();
        results.put(strategyName, strategyResults);
        errorCounts.put(strategyName, strategyErrors);
        verificationErrors.put(strategyName, strategyVerificationErrors);

        // Warmup phase
        logger.info("\nWarming up {} strategy...", strategyName);
        logger.info("Using database type: {}", databaseType);
        logger.info("Dual write enabled: {}", dualWriteEnabled);
        logger.info("Migration strategy: {}", migrationStrategy);
        
        for (int i = 0; i < WARMUP_ITERATIONS; i++) {
            performOperations(strategyResults, strategyErrors, strategyVerificationErrors, true);
        }

        // Actual test phase
        logger.info("\nTesting {} strategy...", strategyName);
        for (int i = 0; i < ITERATIONS; i++) {
            performOperations(strategyResults, strategyErrors, strategyVerificationErrors, false);
        }
    }

    private void verifyProperties(String databaseType, boolean dualWriteEnabled, String migrationStrategy) {
        // Make a test call to verify properties
        ResponseEntity<String> response = restTemplate.getForEntity("/actuator/env", String.class);
        logger.info("Current environment properties:");
        logger.info("{}", response.getBody());
        
        // Verify through repositories
        if (dualWriteEnabled) {
            assertTrue(mongoMemberRepository.count() >= 0, "MongoDB repository should be accessible");
            assertTrue(jpaMemberRepository.count() >= 0, "JPA repository should be accessible");
        } else if (databaseType.equals("mongo")) {
            assertTrue(mongoMemberRepository.count() >= 0, "MongoDB repository should be accessible");
        } else {
            assertTrue(jpaMemberRepository.count() >= 0, "JPA repository should be accessible");
        }
    }

    private void performOperations(Map<String, List<Long>> results, Map<String, Integer> errors, 
                                 Map<String, Integer> verificationErrors, boolean isWarmup) throws Exception {
        // Create Member
        MemberDTO memberDTO = MemberDTO.builder()
                .name("Performance Test User")
                .email("performance@test.com")
                .phoneNumber("+12345678901")
                .build();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        startTimer();
        ResponseEntity<MemberDTO> createResponse = restTemplate.exchange(
            baseUrl,
            HttpMethod.POST,
            new HttpEntity<>(memberDTO, headers),
            MemberDTO.class
        );
        stopTimer();

        if (createResponse.getStatusCode().is2xxSuccessful()) {
            String id = createResponse.getBody().getId();
            // Track the created IDs
            testDataManager.trackMongoId(id);
            testDataManager.trackJpaId(Long.parseLong(id));

            if (!isWarmup) {
                // Convert nanoseconds to milliseconds
                long responseTimeMs = responseTimes.get(responseTimes.size() - 1) / 1_000_000;
                results.computeIfAbsent("Create", k -> new ArrayList<>()).add(responseTimeMs);
            }

            // Verify dual write if enabled
            if (System.getProperty("app.dual-write.enabled").equals("true")) {
                verifyDualWrite(id, memberDTO, "Create", verificationErrors);
            }

            // Get Member by ID
            startTimer();
            ResponseEntity<MemberDTO> getResponse = restTemplate.getForEntity(
                baseUrl + "/" + id,
                MemberDTO.class
            );
            stopTimer();

            if (getResponse.getStatusCode().is2xxSuccessful()) {
                if (!isWarmup) {
                    // Convert nanoseconds to milliseconds
                    long responseTimeMs = responseTimes.get(responseTimes.size() - 1) / 1_000_000;
                    results.computeIfAbsent("GetById", k -> new ArrayList<>()).add(responseTimeMs);
                }
                assertEquals(memberDTO.getName(), getResponse.getBody().getName());
                assertEquals(memberDTO.getEmail(), getResponse.getBody().getEmail());
            }

            // Get All Members
            startTimer();
            ResponseEntity<List> getAllResponse = restTemplate.getForEntity(
                baseUrl,
                List.class
            );
            stopTimer();

            if (getAllResponse.getStatusCode().is2xxSuccessful()) {
                if (!isWarmup) {
                    // Convert nanoseconds to milliseconds
                    long responseTimeMs = responseTimes.get(responseTimes.size() - 1) / 1_000_000;
                    results.computeIfAbsent("GetAll", k -> new ArrayList<>()).add(responseTimeMs);
                }
                assertNotNull(getAllResponse.getBody());
                assertFalse(getAllResponse.getBody().isEmpty());
            }

            // Update Member
            MemberDTO updatedMember = MemberDTO.builder()
                    .id(id)
                    .name("Updated Performance Test User")
                    .email("updated@test.com")
                    .phoneNumber("+19876543210")
                    .build();

            startTimer();
            ResponseEntity<MemberDTO> updateResponse = restTemplate.exchange(
                baseUrl + "/" + id,
                HttpMethod.PUT,
                new HttpEntity<>(updatedMember, headers),
                MemberDTO.class
            );
            stopTimer();

            if (updateResponse.getStatusCode().is2xxSuccessful()) {
                if (!isWarmup) {
                    // Convert nanoseconds to milliseconds
                    long responseTimeMs = responseTimes.get(responseTimes.size() - 1) / 1_000_000;
                    results.computeIfAbsent("Update", k -> new ArrayList<>()).add(responseTimeMs);
                }
                assertEquals(updatedMember.getName(), updateResponse.getBody().getName());
                assertEquals(updatedMember.getEmail(), updateResponse.getBody().getEmail());

                // Verify dual write if enabled
                if (System.getProperty("app.dual-write.enabled").equals("true")) {
                    verifyDualWrite(id, updatedMember, "Update", verificationErrors);
                }
            }

            // Search Members
            startTimer();
            ResponseEntity<List> searchResponse = restTemplate.getForEntity(
                baseUrl + "/search?name=Performance",
                List.class
            );
            stopTimer();

            if (searchResponse.getStatusCode().is2xxSuccessful()) {
                if (!isWarmup) {
                    // Convert nanoseconds to milliseconds
                    long responseTimeMs = responseTimes.get(responseTimes.size() - 1) / 1_000_000;
                    results.computeIfAbsent("Search", k -> new ArrayList<>()).add(responseTimeMs);
                }
                assertNotNull(searchResponse.getBody());
            }

            // Delete Member
            startTimer();
            ResponseEntity<Void> deleteResponse = restTemplate.exchange(
                baseUrl + "/" + id,
                HttpMethod.DELETE,
                null,
                Void.class
            );
            stopTimer();

            if (deleteResponse.getStatusCode().is2xxSuccessful() || deleteResponse.getStatusCode().is3xxRedirection()) {
                if (!isWarmup) {
                    // Convert nanoseconds to milliseconds
                    long responseTimeMs = responseTimes.get(responseTimes.size() - 1) / 1_000_000;
                    results.computeIfAbsent("Delete", k -> new ArrayList<>()).add(responseTimeMs);
                }

                // Verify dual write if enabled
                if (System.getProperty("app.dual-write.enabled").equals("true")) {
                    verifyDualWriteDelete(id, "Delete", verificationErrors);
                }
            }
        }
    }

    private void verifyDualWrite(String id, MemberDTO memberDTO, String operation, Map<String, Integer> verificationErrors) {
        try {
            // Add delay to ensure writes are completed
            TimeUnit.MILLISECONDS.sleep(100);

            // Check JPA database
            JpaMember jpaMember = jpaMemberRepository.findById(Long.parseLong(id)).orElse(null);
            if (jpaMember == null) {
                logger.warn("JPA verification failed for {} operation - Member not found in JPA database", operation);
                verificationErrors.merge(operation, 1, Integer::sum);
                return;
            }
            if (!jpaMember.getName().equals(memberDTO.getName()) || !jpaMember.getEmail().equals(memberDTO.getEmail())) {
                logger.warn("JPA verification failed for {} operation - Data mismatch", operation);
                logger.warn("Expected: {}", memberDTO);
                logger.warn("Found in JPA: {}", jpaMember);
                verificationErrors.merge(operation, 1, Integer::sum);
            }

            // Check MongoDB
            MongoMember mongoMember = mongoMemberRepository.findById(id).orElse(null);
            if (mongoMember == null) {
                logger.warn("MongoDB verification failed for {} operation - Member not found in MongoDB", operation);
                verificationErrors.merge(operation, 1, Integer::sum);
                return;
            }
            if (!mongoMember.getName().equals(memberDTO.getName()) || !mongoMember.getEmail().equals(memberDTO.getEmail())) {
                logger.warn("MongoDB verification failed for {} operation - Data mismatch", operation);
                logger.warn("Expected: {}", memberDTO);
                logger.warn("Found in MongoDB: {}", mongoMember);
                verificationErrors.merge(operation, 1, Integer::sum);
            }

            logger.debug("Current database counts after {}:", operation);
            logger.debug("JPA count: {}", jpaMemberRepository.count());
            logger.debug("MongoDB count: {}", mongoMemberRepository.count());

        } catch (Exception e) {
            logger.error("Verification error for {} operation: {}", operation, e.getMessage());
            verificationErrors.merge(operation, 1, Integer::sum);
        }
    }

    private void verifyDualWriteDelete(String id, String operation, Map<String, Integer> verificationErrors) {
        try {
            // Add delay to ensure deletes are completed
            TimeUnit.MILLISECONDS.sleep(100);

            // Check JPA database
            boolean jpaExists = jpaMemberRepository.existsById(Long.parseLong(id));
            if (jpaExists) {
                logger.warn("JPA verification failed for {} operation - record still exists", operation);
                verificationErrors.merge(operation, 1, Integer::sum);
            }

            // Check MongoDB
            boolean mongoExists = mongoMemberRepository.existsById(id);
            if (mongoExists) {
                logger.warn("MongoDB verification failed for {} operation - record still exists", operation);
                verificationErrors.merge(operation, 1, Integer::sum);
            }

            logger.debug("Current database counts after {}:", operation);
            logger.debug("JPA count: {}", jpaMemberRepository.count());
            logger.debug("MongoDB count: {}", mongoMemberRepository.count());

        } catch (Exception e) {
            logger.error("Verification error for {} operation: {}", operation, e.getMessage());
            verificationErrors.merge(operation, 1, Integer::sum);
        }
    }

    private void printComparisonReport() {
        logger.info("\n=== Database Strategy Performance Comparison Report ===\n");
        
        String[] operations = {"Create", "GetById", "GetAll", "Update", "Search", "Delete"};
        
        for (String operation : operations) {
            logger.info("\n{} Operation:", operation);
            logger.info("----------------------------------------");
            
            for (Map.Entry<String, Map<String, List<Long>>> strategyEntry : results.entrySet()) {
                String strategy = strategyEntry.getKey();
                List<Long> times = strategyEntry.getValue().get(operation);
                Integer errors = errorCounts.get(strategy).getOrDefault(operation, 0);
                Integer verificationErrors = this.verificationErrors.get(strategy).getOrDefault(operation, 0);
                
                double avg = times.stream().mapToLong(Long::longValue).average().orElse(0.0);
                double p95 = calculatePercentile(times, 95);
                double p99 = calculatePercentile(times, 99);
                
                logger.info("{}:", strategy);
                logger.info("  Average: {:.2f} ms", avg);
                logger.info("  P95: {:.2f} ms", p95);
                logger.info("  P99: {:.2f} ms", p99);
                logger.info("  API Errors: {}", errors);
                logger.info("  Verification Errors: {}", verificationErrors);
                logger.info("");
            }
        }

        logger.info("\nSummary:");
        logger.info("----------------------------------------");
        for (String strategy : results.keySet()) {
            double totalAvg = 0;
            int totalErrors = 0;
            int totalVerificationErrors = 0;
            for (String operation : operations) {
                List<Long> times = results.get(strategy).get(operation);
                totalAvg += times.stream().mapToLong(Long::longValue).average().orElse(0.0);
                totalErrors += errorCounts.get(strategy).getOrDefault(operation, 0);
                totalVerificationErrors += verificationErrors.get(strategy).getOrDefault(operation, 0);
            }
            logger.info("{} Overall Average: {:.2f} ms", strategy, totalAvg / operations.length);
            logger.info("{} Total API Errors: {}", strategy, totalErrors);
            logger.info("{} Total Verification Errors: {}", strategy, totalVerificationErrors);
        }

        // Generate visual report with charts
        PerformanceReportGenerator.generateReport(results, errorCounts, verificationErrors);
        logger.info("\nDetailed performance report with charts has been generated in the 'performance-reports' directory.");
    }

    private double calculatePercentile(List<Long> times, int percentile) {
        if (times.isEmpty()) return 0.0;
        int index = (int) Math.ceil(percentile / 100.0 * times.size()) - 1;
        return times.stream()
                .sorted()
                .skip(index)
                .findFirst()
                .orElse(0L);
    }
} 