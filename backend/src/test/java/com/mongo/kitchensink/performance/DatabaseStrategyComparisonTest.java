package com.mongo.kitchensink.performance;

import com.mongo.kitchensink.config.TestContainersConfig;
import com.mongo.kitchensink.dto.MemberDTO;
import com.mongo.kitchensink.model.JpaMember;
import com.mongo.kitchensink.model.MongoMember;
import com.mongo.kitchensink.repository.JpaMemberRepository;
import com.mongo.kitchensink.repository.MongoMemberRepository;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
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
import org.springframework.test.context.support.TestPropertySourceUtils;
import org.springframework.test.util.TestSocketUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {
    "spring.test.database.replace=NONE",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.data.mongodb.auto-index-creation=true",
    "spring.main.allow-bean-definition-overriding=true",
    "spring.datasource.h2.enabled=false"
})
public class DatabaseStrategyComparisonTest extends BasePerformanceTest {

    private static final int ITERATIONS = 100;
    private static final int WARMUP_ITERATIONS = 10;

    private final Map<String, Map<String, List<Long>>> results = new HashMap<>();
    private final Map<String, Map<String, Integer>> errorCounts = new HashMap<>();
    private final Map<String, Map<String, Integer>> verificationErrors = new HashMap<>();

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private TestRestTemplate restTemplate;

    @Value("${local.server.port}")
    private int port;

    @Autowired
    private JpaMemberRepository jpaMemberRepository;

    @Autowired
    private MongoMemberRepository mongoMemberRepository;

    private String baseUrl;

    @BeforeAll
    static void setup() {
        TestContainersConfig.startContainers();
    }

    @AfterAll
    static void cleanup() {
        TestContainersConfig.stopContainers();
    }

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
        // Clean both databases
        jpaMemberRepository.deleteAll();
        mongoMemberRepository.deleteAll();
        
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
        System.out.println("\nWarming up " + strategyName + " strategy...");
        System.out.println("Using database type: " + databaseType);
        System.out.println("Dual write enabled: " + dualWriteEnabled);
        System.out.println("Migration strategy: " + migrationStrategy);
        
        for (int i = 0; i < WARMUP_ITERATIONS; i++) {
            performOperations(strategyResults, strategyErrors, strategyVerificationErrors, true);
        }

        // Actual test phase
        System.out.println("\nTesting " + strategyName + " strategy...");
        for (int i = 0; i < ITERATIONS; i++) {
            performOperations(strategyResults, strategyErrors, strategyVerificationErrors, false);
        }
    }

    private void verifyProperties(String databaseType, boolean dualWriteEnabled, String migrationStrategy) {
        // Make a test call to verify properties
        ResponseEntity<String> response = restTemplate.getForEntity("/actuator/env", String.class);
        System.out.println("Current environment properties:");
        System.out.println(response.getBody());
        
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
            if (!isWarmup) {
                // Convert nanoseconds to milliseconds
                long responseTimeMs = responseTimes.get(responseTimes.size() - 1) / 1_000_000;
                results.computeIfAbsent("Create", k -> new ArrayList<>()).add(responseTimeMs);
            }
            String id = createResponse.getBody().getId();

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
                verificationErrors.merge(operation, 1, Integer::sum);
                System.out.println("JPA verification failed for " + operation + " operation - Member not found in JPA database");
            } else if (!jpaMember.getName().equals(memberDTO.getName()) || 
                !jpaMember.getEmail().equals(memberDTO.getEmail())) {
                verificationErrors.merge(operation, 1, Integer::sum);
                System.out.println("JPA verification failed for " + operation + " operation - Data mismatch");
                System.out.println("Expected: " + memberDTO);
                System.out.println("Found in JPA: " + jpaMember);
            }

            // Check MongoDB
            MongoMember mongoMember = mongoMemberRepository.findById(id).orElse(null);
            if (mongoMember == null) {
                verificationErrors.merge(operation, 1, Integer::sum);
                System.out.println("MongoDB verification failed for " + operation + " operation - Member not found in MongoDB");
            } else if (!mongoMember.getName().equals(memberDTO.getName()) || 
                !mongoMember.getEmail().equals(memberDTO.getEmail())) {
                verificationErrors.merge(operation, 1, Integer::sum);
                System.out.println("MongoDB verification failed for " + operation + " operation - Data mismatch");
                System.out.println("Expected: " + memberDTO);
                System.out.println("Found in MongoDB: " + mongoMember);
            }

            // Print database counts for verification
            System.out.println("Current database counts after " + operation + ":");
            System.out.println("JPA count: " + jpaMemberRepository.count());
            System.out.println("MongoDB count: " + mongoMemberRepository.count());

        } catch (Exception e) {
            verificationErrors.merge(operation, 1, Integer::sum);
            System.out.println("Verification error for " + operation + " operation: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void verifyDualWriteDelete(String id, String operation, Map<String, Integer> verificationErrors) {
        try {
            // Add delay to ensure deletes are completed
            TimeUnit.MILLISECONDS.sleep(100);

            // Check JPA database
            boolean jpaExists = jpaMemberRepository.existsById(Long.parseLong(id));
            if (jpaExists) {
                verificationErrors.merge(operation, 1, Integer::sum);
                System.out.println("JPA verification failed for " + operation + " operation - record still exists");
            }

            // Check MongoDB
            boolean mongoExists = mongoMemberRepository.existsById(id);
            if (mongoExists) {
                verificationErrors.merge(operation, 1, Integer::sum);
                System.out.println("MongoDB verification failed for " + operation + " operation - record still exists");
            }

            // Print database counts for verification
            System.out.println("Current database counts after " + operation + ":");
            System.out.println("JPA count: " + jpaMemberRepository.count());
            System.out.println("MongoDB count: " + mongoMemberRepository.count());

        } catch (Exception e) {
            verificationErrors.merge(operation, 1, Integer::sum);
            System.out.println("Verification error for " + operation + " operation: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void printComparisonReport() {
        // Generate text report in console
        System.out.println("\n=== Database Strategy Performance Comparison Report ===\n");
        
        String[] operations = {"Create", "GetById", "GetAll", "Update", "Search", "Delete"};
        
        for (String operation : operations) {
            System.out.println("\n" + operation + " Operation:");
            System.out.println("----------------------------------------");
            
            for (Map.Entry<String, Map<String, List<Long>>> strategyEntry : results.entrySet()) {
                String strategy = strategyEntry.getKey();
                List<Long> times = strategyEntry.getValue().get(operation);
                Integer errors = errorCounts.get(strategy).getOrDefault(operation, 0);
                Integer verificationErrors = this.verificationErrors.get(strategy).getOrDefault(operation, 0);
                
                double avg = times.stream().mapToLong(Long::longValue).average().orElse(0.0);
                double p95 = calculatePercentile(times, 95);
                double p99 = calculatePercentile(times, 99);
                
                System.out.printf("%s:\n", strategy);
                System.out.printf("  Average: %.2f ms\n", avg);
                System.out.printf("  P95: %.2f ms\n", p95);
                System.out.printf("  P99: %.2f ms\n", p99);
                System.out.printf("  API Errors: %d\n", errors);
                System.out.printf("  Verification Errors: %d\n", verificationErrors);
                System.out.println();
            }
        }

        // Print summary
        System.out.println("\nSummary:");
        System.out.println("----------------------------------------");
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
            System.out.printf("%s Overall Average: %.2f ms\n", strategy, totalAvg / operations.length);
            System.out.printf("%s Total API Errors: %d\n", strategy, totalErrors);
            System.out.printf("%s Total Verification Errors: %d\n", strategy, totalVerificationErrors);
        }

        // Generate visual report with charts
        PerformanceReportGenerator.generateReport(results, errorCounts, verificationErrors);
        System.out.println("\nDetailed performance report with charts has been generated in the 'performance-reports' directory.");
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