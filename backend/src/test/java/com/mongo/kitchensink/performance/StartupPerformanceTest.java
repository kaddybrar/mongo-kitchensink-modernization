package com.mongo.kitchensink.performance;

import com.mongo.kitchensink.config.TestContainersConfig;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.boot.SpringApplication;
import com.mongo.kitchensink.KitchensinkApplication;
import org.springframework.test.context.support.TestPropertySourceUtils;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.boot.web.servlet.server.ConfigurableServletWebServerFactory;

import java.io.IOException;
import java.net.ServerSocket;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {
    "spring.test.database.replace=NONE",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.data.mongodb.auto-index-creation=true",
    "spring.main.allow-bean-definition-overriding=true",
    "spring.datasource.h2.enabled=false"
})
public class StartupPerformanceTest extends BasePerformanceTest {

    private static final int ITERATIONS = 3;

    private int findRandomPort() {
        try (ServerSocket socket = new ServerSocket(0)) {
            socket.setReuseAddress(true);
            return socket.getLocalPort();
        } catch (IOException e) {
            throw new RuntimeException("Could not find a free port", e);
        }
    }

    @Test
    void measureStartupTime() {
        Map<String, Map<String, List<Long>>> results = new HashMap<>();
        Map<String, List<Long>> startupTimes = new HashMap<>();
        results.put("Application Startup", startupTimes);

        System.out.println("\nMeasuring application startup time...");
        
        for (int i = 0; i < ITERATIONS; i++) {
            System.out.println("\nIteration " + (i + 1) + " of " + ITERATIONS);
            
            // Measure container startup time
            Instant containersStart = Instant.now();
            TestContainersConfig.startContainers();
            long containerStartupTime = Duration.between(containersStart, Instant.now()).toMillis();
            startupTimes.computeIfAbsent("Container Startup", k -> new ArrayList<>()).add(containerStartupTime);
            
            // Measure Spring context startup time
            Instant contextStart = Instant.now();
            ConfigurableApplicationContext context = null;
            try {
                SpringApplication app = new SpringApplication(KitchensinkApplication.class);
                ConfigurableEnvironment env = new StandardEnvironment();
                
                // Get the properties from TestContainersConfig
                Map<String, String> properties = TestContainersConfig.getTestProperties();
                
                // Add all test container properties
                for (Map.Entry<String, String> entry : properties.entrySet()) {
                    TestPropertySourceUtils.addInlinedPropertiesToEnvironment(env, 
                        entry.getKey() + "=" + entry.getValue());
                }
                
                // Find a random port for this iteration
                int randomPort = findRandomPort();
                
                // Add other required properties
                TestPropertySourceUtils.addInlinedPropertiesToEnvironment(env,
                    "spring.test.database.replace=NONE",
                    "spring.jpa.hibernate.ddl-auto=create-drop",
                    "spring.data.mongodb.auto-index-creation=true",
                    "spring.main.allow-bean-definition-overriding=true",
                    "spring.datasource.h2.enabled=false",
                    "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect",
                    "server.port=" + randomPort
                );
                
                app.setEnvironment(env);
                context = app.run();
                long contextStartupTime = Duration.between(contextStart, Instant.now()).toMillis();
                startupTimes.computeIfAbsent("Spring Context", k -> new ArrayList<>()).add(contextStartupTime);
            } finally {
                if (context != null) {
                    context.close();
                }
                TestContainersConfig.stopContainers();
            }
        }

        // Generate report
        System.out.println("\n=== Application Startup Performance Report ===\n");
        
        for (Map.Entry<String, List<Long>> entry : startupTimes.entrySet()) {
            String component = entry.getKey();
            List<Long> times = entry.getValue();
            
            double avgTime = times.stream()
                    .mapToLong(Long::longValue)
                    .average()
                    .orElse(0.0);
            
            System.out.printf("%s:\n", component);
            System.out.printf("  Average: %.2f ms (%.2f seconds)\n", avgTime, avgTime / 1000.0);
            System.out.printf("  Individual times: %s\n", times);
            System.out.println();
        }

        // Generate visual report
        PerformanceReportGenerator.generateStartupReport(results);
    }
} 