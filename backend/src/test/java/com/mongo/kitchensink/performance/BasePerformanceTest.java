package com.mongo.kitchensink.performance;

import com.mongo.kitchensink.integration.BaseIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.test.context.ActiveProfiles;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@ActiveProfiles("test")
public abstract class BasePerformanceTest extends BaseIntegrationTest {

    protected List<Long> responseTimes = new ArrayList<>();
    protected long startTime;

    @BeforeEach
    protected void setUp() {
        super.setUp();
        responseTimes.clear();
        // Clean up databases before each strategy test
        cleanDatabases();
    }

    protected void startTimer() {
        startTime = System.nanoTime();
    }

    protected void stopTimer() {
        long duration = System.nanoTime() - startTime;
        responseTimes.add(duration);
    }

    protected double getAverageResponseTime() {
        return responseTimes.stream()
                .mapToLong(Long::longValue)
                .average()
                .orElse(0.0) / 1_000_000.0; // Convert to milliseconds
    }

    protected double getP95ResponseTime() {
        if (responseTimes.isEmpty()) return 0.0;
        int index = (int) Math.ceil(0.95 * responseTimes.size()) - 1;
        return responseTimes.stream()
                .sorted()
                .skip(index)
                .findFirst()
                .orElse(0L) / 1_000_000.0; // Convert to milliseconds
    }

    protected double getP99ResponseTime() {
        if (responseTimes.isEmpty()) return 0.0;
        int index = (int) Math.ceil(0.99 * responseTimes.size()) - 1;
        return responseTimes.stream()
                .sorted()
                .skip(index)
                .findFirst()
                .orElse(0L) / 1_000_000.0; // Convert to milliseconds
    }

    protected void printPerformanceMetrics(String operation) {
        System.out.println("\nPerformance Metrics for " + operation + ":");
        System.out.println("Average Response Time: " + String.format("%.2f", getAverageResponseTime()) + " ms");
        System.out.println("P95 Response Time: " + String.format("%.2f", getP95ResponseTime()) + " ms");
        System.out.println("P99 Response Time: " + String.format("%.2f", getP99ResponseTime()) + " ms");
        System.out.println("Total Requests: " + responseTimes.size());
    }
} 