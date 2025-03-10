package com.mongo.kitchensink.performance;

import com.mongo.kitchensink.integration.BaseIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.test.context.ActiveProfiles;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@ActiveProfiles("test")
public abstract class BasePerformanceTest extends BaseIntegrationTest {
    private static final Logger logger = LoggerFactory.getLogger(BasePerformanceTest.class);

    protected List<Long> responseTimes = new ArrayList<>();
    protected long startTime;

    @BeforeEach
    @Override
    public void setUp() {
        super.setUp();
        responseTimes = new ArrayList<>();
    }

    protected void startTimer() {
        startTime = System.nanoTime();
    }

    protected void stopTimer() {
        long endTime = System.nanoTime();
        responseTimes.add(endTime - startTime);
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
        logger.info("\nPerformance Metrics for {}:", operation);
        logger.info("Average Response Time: {:.2f} ms", getAverageResponseTime());
        logger.info("P95 Response Time: {:.2f} ms", getP95ResponseTime());
        logger.info("P99 Response Time: {:.2f} ms", getP99ResponseTime());
        logger.info("Total Requests: {}", responseTimes.size());
    }
} 