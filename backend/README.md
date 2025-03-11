# MongoDB Kitchen Sink Backend

This is the backend component of the MongoDB Kitchen Sink Modernization project, featuring RESTful CRUD APIs, comprehensive testing, and code coverage analysis.

## Technology Stack

- Java 23
- Spring Boot 3.1.0
- JUnit 5
- Mockito 5.10.0
- JaCoCo 0.8.11
- Swagger/OpenAPI 3
- MongoDB 5.0
- PostgreSQL 16

## Project Structure

```
src/
├── main/
│   ├── java/com/mongo/kitchensink/
│   │   ├── config/
│   │   │   └── OpenApiConfig.java         # Swagger/OpenAPI configuration
│   │   ├── controller/
│   │   │   ├── HealthController.java      # Health check endpoint
│   │   │   └── MemberController.java      # Member CRUD endpoints
│   │   ├── exception/
│   │   │   ├── DuplicateEmailException.java
│   │   │   ├── ErrorResponse.java
│   │   │   ├── GlobalExceptionHandler.java
│   │   │   └── MemberNotFoundException.java
│   │   ├── model/
│   │   │   └── Member.java                # Member entity with validation
│   │   ├── repository/
│   │   │   └── MemberRepository.java      # Data access layer
│   │   ├── service/
│   │   │   └── MemberService.java         # Business logic layer
│   │   └── KitchensinkApplication.java    # Main application class
│   └── resources/
│       ├── application.properties         # Application configuration
│       └── application-test.properties    # Test configuration
└── test/
    └── java/com/mongo/kitchensink/
        ├── base/
        │   ├── BaseControllerTest.java    # Base class for controller tests
        │   ├── BaseIntegrationTest.java   # Base class for integration tests
        │   └── BaseUnitTest.java          # Base class for unit tests
        ├── controller/
        │   ├── HealthControllerTest.java
        │   └── MemberControllerTest.java
        ├── integration/
        │   ├── HealthControllerIntegrationTest.java
        │   └── MemberServiceIntegrationTest.java
        ├── service/
        │   └── MemberServiceTest.java
        ├── util/
        │   ├── ExceptionTestUtil.java     # Utilities for testing exceptions
        │   └── TestDataFactory.java       # Factory for test data
        └── JacocoVerificationTest.java    # Verifies JaCoCo integration
```

## Getting Started

### Prerequisites

- Java 23 or higher
- Maven 3.8 or higher

### Running the Application

```bash
mvn spring-boot:run
```

The application will be available at http://localhost:8080.

### API Documentation

Swagger UI is available at http://localhost:8080/swagger-ui.html

## API Endpoints

| Method | Endpoint                  | Description                      |
|--------|---------------------------|----------------------------------|
| GET    | /api/v1/health            | Health check endpoint            |
| GET    | /api/v1/members           | Get all members                  |
| GET    | /api/v1/members/{id}      | Get member by ID                 |
| POST   | /api/v1/members           | Create a new member              |
| PUT    | /api/v1/members/{id}      | Update an existing member        |
| DELETE | /api/v1/members/{id}      | Delete a member                  |
| GET    | /api/v1/members/search    | Search members by name           |

## MongoDB Migration Strategy

This project demonstrates a gradual migration approach from PostgreSQL to MongoDB using a dual-write pattern. The key aspects of this strategy include:

### Architecture

- **Common Interface**: `IMember` interface provides a unified contract for both database implementations
- **Dual Database Support**: Parallel implementations for PostgreSQL (`JpaMember`) and MongoDB (`MongoMember`)
- **Service Layer Abstraction**: `IMemberService` with specific implementations for each database type
- **Orchestration**: `DualWriteMemberService` manages writing to both databases while reading from a configurable source

### Key Features

- **Configurable Database Mode**: Application can run in JPA-only, MongoDB-only, or dual-write mode
- **Selective Read Source**: Configurable read source (JPA or MongoDB) during migration
- **Data Consistency Checks**: Optional verification between databases to ensure data integrity
- **Rollback Support**: Transaction-like behavior with rollback for failed operations
- **ID Type Handling**: Transparent conversion between Long IDs (JPA) and String IDs (MongoDB)

### Configuration

```properties
# Database type: jpa, mongo, or dual
app.database.type=dual

# Read source when in dual mode: jpa or mongo
app.database.read.source=jpa

# Enable/disable dual-write functionality
app.dual.write.enabled=true

# Enable/disable data comparison between databases
app.dual.write.compare=true
```

This approach enables a zero-downtime migration with minimal risk, allowing for gradual testing and validation of the MongoDB implementation before fully switching over.

## Database Migrations with Flyway

### Migration Structure

The project uses Flyway for database schema migrations with separate locations for PostgreSQL and MongoDB:

```
src/main/resources/db/migration/
├── postgresql/                # PostgreSQL migrations
│   ├── V1__Create_member_table.sql
│   ├── V2__Add_updated_at_trigger.sql
│   └── V3__Insert_sample_data.sql
└── mongodb/                  # MongoDB migrations (if needed)
    └── V1__Initial_setup.js
```

### Migration Guidelines

1. **Naming Convention**:
   - Version-based: `V{number}__{description}.sql`
   - Example: `V1__Create_member_table.sql`
   - Use double underscores between version and description
   - Keep descriptions concise and use underscores for spaces

2. **Schema Design**:
   - Use `IF NOT EXISTS` for idempotent operations
   - Create sequences before tables that use them
   - Define explicit column types (e.g., `BIGINT` instead of `SERIAL`)
   - Add appropriate indexes for performance
   - Use `TIMESTAMP` type for temporal fields

3. **Best Practices**:
   - Never modify existing migrations
   - Create new migrations for schema changes
   - Include rollback scripts when possible
   - Test migrations on a clean database
   - Keep migrations atomic and focused

### Running Migrations

#### Using Maven

```bash
# Apply migrations
mvn flyway:migrate

# Clean database and reapply migrations
mvn flyway:clean flyway:migrate

# Check migration status
mvn flyway:info

# Validate migrations
mvn flyway:validate

# Repair migration history table
mvn flyway:repair
```

#### Using Docker Compose

```bash
# Apply migrations through application startup
docker compose up -d

# Force migration refresh (will clean database)
docker compose down
docker volume rm mongo-kitchensink-modernization_postgres_data
docker compose up -d
```

### Configuration

The project uses Spring Boot's Flyway auto-configuration with these settings:

```properties
# Enable Flyway
spring.flyway.enabled=true

# PostgreSQL migrations
spring.flyway.locations=classpath:db/migration/postgresql
spring.flyway.baseline-on-migrate=true
spring.flyway.validate-on-migrate=true

# Database connection (from environment)
spring.flyway.url=${POSTGRES_URL:jdbc:postgresql://localhost:5432/kitchensink}
spring.flyway.user=${POSTGRES_USER:postgres}
spring.flyway.password=${POSTGRES_PASSWORD:mysecretpassword}
```

### Troubleshooting Migrations

1. **Checksum Mismatch**:
   ```bash
   # Repair Flyway schema history
   mvn flyway:repair
   ```

2. **Clean Start**:
   ```bash
   # Remove existing data and reapply migrations
   mvn flyway:clean flyway:migrate
   ```

3. **Version Conflicts**:
   ```bash
   # Check current status
   mvn flyway:info
   
   # Repair if needed
   mvn flyway:repair
   ```

## Testing Framework

The project implements a robust testing strategy with multiple layers of tests:

### Test Structure

- **Unit Tests**: Test individual components in isolation
  - `controller`: Tests for REST controllers using MockMvc
  - `service`: Tests for service layer with mocked repositories
  - `repository`: Tests for data access layer

- **Integration Tests**: Test components working together
  - `integration`: End-to-end tests with actual database operations

- **Base Test Classes**:
  - `BaseUnitTest`: Common setup for unit tests with Mockito initialization
  - `BaseControllerTest`: Common setup for controller tests with MockMvc
  - `BaseIntegrationTest`: Common setup for integration tests with test containers

### Running Tests

```bash
# Run all tests
mvn test

# Run a specific test class
mvn test -Dtest=MemberControllerTest

# Run a specific test method
mvn test -Dtest=MemberControllerTest#getMember_ExistingId_ReturnsMember
```

## Performance Testing

The backend includes a comprehensive performance testing framework that measures:

### Test Types
- API response times for all endpoints
- Database operation latencies (both MongoDB and PostgreSQL)
- Concurrent request handling
- Resource utilization under load

### Running Performance Tests

```bash
# Run performance tests in Docker
./docker-performance-test.sh

# View results
open ../performance-reports/latest/backend/report.html
```

### Test Configuration

Performance tests can be configured through environment variables:
```properties
# Number of concurrent users
PERFORMANCE_TEST_USERS=100

# Test duration in seconds
PERFORMANCE_TEST_DURATION=300

# Target endpoints
PERFORMANCE_TEST_ENDPOINTS=/api/v1/members,/api/v1/health

# Database type for testing
PERFORMANCE_TEST_DB_TYPE=mongo,jpa
```

### Test Reports
- Detailed latency metrics
- Response time percentiles
- Error rates and types
- Database operation timings
- Resource utilization graphs

## Code Coverage with JaCoCo

JaCoCo is configured to provide code coverage metrics for the test suite.

### Generating Coverage Reports

```bash
# Run tests and generate coverage report
mvn clean test jacoco:report
```

The coverage report will be available at `target/site/jacoco/index.html`.

### Coverage Goals

The project aims for:
- 70% instruction coverage
- 60% branch coverage

### Coverage Configuration

JaCoCo is configured in the `pom.xml` with:
- Exclusions for generated code (Hibernate proxies, Mockito mocks)
- Integration with Maven Surefire for test execution
- Report generation after test execution

## Code Quality

### Coding Standards

The project follows standard Java coding conventions and Spring Boot best practices:

- Clear separation of concerns (controller, service, repository)
- Proper exception handling with custom exceptions
- Input validation using Bean Validation (JSR-380)
- Comprehensive error responses

### Error Handling and Exception Types

The application implements a robust error handling strategy to provide consistent, informative responses for various error scenarios.

#### Exception Hierarchy

```
java.lang.Exception
└── RuntimeException
    ├── MemberNotFoundException       # When a member with given ID doesn't exist
    ├── DuplicateEmailException       # When trying to create a member with an existing email
    ├── ValidationException           # For bean validation failures
    └── DataAccessException           # For database-related errors
```

#### Global Exception Handler

The `GlobalExceptionHandler` class centralizes error handling using Spring's `@ControllerAdvice` and provides appropriate HTTP status codes and error messages for different exception types:

#### HTTP Status Codes

| Exception Type             | HTTP Status Code | Description                                      |
|----------------------------|------------------|--------------------------------------------------|
| MemberNotFoundException    | 404 Not Found    | The requested member doesn't exist               |
| DuplicateEmailException    | 409 Conflict     | A member with the same email already exists      |
| ValidationException        | 400 Bad Request  | Request data failed validation                   |
| MethodArgumentNotValidException | 400 Bad Request | Bean validation failures                     |
| HttpMessageNotReadableException | 400 Bad Request | Invalid JSON or request format              |
| DataAccessException        | 500 Server Error | Database-related errors                          |
| Exception                  | 500 Server Error | Generic fallback for unhandled exceptions        |

#### Error Response Format

All error responses follow a consistent JSON format:

```json
{
  "status": 404,
  "message": "Member not found with ID: 123",
  "timestamp": "2023-06-15T10:30:45.123Z"
}
```

## Java 23 Compatibility

The project is configured to work with Java 23, which required special configuration for testing tools:

- ByteBuddy experimental mode enabled for Mockito
- JVM flags for module access
- JaCoCo configuration for newer class file versions

## Troubleshooting

### Common Issues

- **JaCoCo Instrumentation Errors**: If you encounter JaCoCo instrumentation errors, try running with `-Djacoco.skip.instrument.mockito=true`
- **Mockito Errors with Java 23**: Use the `-Dnet.bytebuddy.experimental=true` flag to enable ByteBuddy experimental features
- **Hibernate Proxy Issues**: JaCoCo may have issues with Hibernate proxies. The configuration excludes these classes from instrumentation.

### Debugging Tests

To run a specific test with debug output:

```bash
mvn test -Dtest=TestClassName -Dmaven.surefire.debug
```

## Development Guidelines

### Testing Best Practices

1. Use the appropriate base test class for your test
2. Mock external dependencies in unit tests
3. Use real dependencies in integration tests
4. Follow the Arrange-Act-Assert pattern
5. Test both happy paths and error scenarios
6. Use parameterized tests for testing multiple input variations

### Adding New Tests

When adding new features, ensure you add corresponding tests:

1. Unit tests for individual components
2. Integration tests for end-to-end functionality
3. Edge case tests for validation and error handling

## Future Improvements

- Add pagination for member listing
- Implement filtering and sorting options
- Add more advanced search capabilities
- Implement caching for frequently accessed data
- Add metrics and monitoring
- Containerize the application with Docker

## Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add some amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## License

This project is licensed under the MIT License.

## Performance Testing Framework

The project includes a comprehensive performance testing framework that measures and compares different aspects of the application:

### Test Structure

```
src/test/java/com/mongo/kitchensink/performance/
├── BasePerformanceTest.java           # Base class for performance tests
├── DatabaseStrategyComparisonTest.java # Database strategy comparison tests
├── StartupPerformanceTest.java        # Application startup tests
└── PerformanceReportGenerator.java    # Visual report generation
```

### Database Strategy Performance Testing

Located in `DatabaseStrategyComparisonTest.java`, this test suite compares the performance of different database strategies:

#### Features
- Compares JPA, MongoDB, and dual-write strategies
- Measures CRUD operation latencies
- Includes warmup phase to ensure accurate measurements
- Tracks API errors and data verification issues
- Generates visual performance reports

#### Test Configuration
```properties
# Test iterations
ITERATIONS = 100
WARMUP_ITERATIONS = 10

# Metrics tracked
- Average response time
- P95 latency
- P99 latency
- API errors
- Verification errors
```

### Application Startup Performance Testing

Located in `StartupPerformanceTest.java`, this test suite measures application startup performance:

#### Features
- Measures container startup time
- Measures Spring context initialization time
- Runs multiple iterations with clean state
- Uses random ports to prevent conflicts
- Generates startup time reports

#### Test Configuration
```properties
# Test iterations
ITERATIONS = 3

# Components measured
- Container startup time
- Spring context initialization
```

### Performance Report Generation

The `PerformanceReportGenerator` class creates comprehensive performance reports:

#### Report Features
- Visual charts using JFreeChart
- Average latency comparisons
- Percentile analysis (P95, P99)
- Error rate tracking
- HTML reports with detailed metrics

#### Report Types
1. **Database Strategy Report**
   - Operation-wise performance comparison
   - Error analysis
   - Verification results

2. **Startup Performance Report**
   - Component-wise startup times
   - Iteration details
   - Statistical analysis

Reports are generated in the `target/performance-reports` directory with timestamps.

### Running Performance Tests

```bash
# Run database strategy comparison test
mvn test -Dtest=DatabaseStrategyComparisonTest

# Run startup performance test
mvn test -Dtest=StartupPerformanceTest

# View reports
open target/performance-reports/
```

### Base Performance Test Framework

The `BasePerformanceTest` class provides core functionality:

```java
protected void startTimer()      // Start timing an operation
protected void stopTimer()       // Stop timing and record duration
protected double getAverageResponseTime()  // Calculate average response time
protected double getP95ResponseTime()      // Calculate P95 latency
protected double getP99ResponseTime()      // Calculate P99 latency
protected void printPerformanceMetrics()   // Print formatted metrics
```

### Test Infrastructure

#### TestContainers Configuration
- MongoDB 6.0.2 container
- PostgreSQL 15-alpine container
- Automatic container lifecycle management
- Dynamic port allocation
- Configurable database properties

#### Performance Optimization Properties

```properties
# MongoDB Optimizations
spring.data.mongodb.connections-per-host=100
spring.data.mongodb.threads-allowed-to-block-for-connection-multiplier=5
spring.data.mongodb.connect-timeout=2000
spring.data.mongodb.socket-timeout=5000
spring.data.mongodb.max-wait-time=1500
spring.data.mongodb.write-concern=MAJORITY
spring.data.mongodb.read-preference=PRIMARY

# JPA Optimizations
spring.jpa.properties.hibernate.jdbc.batch_size=50
```

### Best Practices for Performance Testing

1. **Test Environment**
   - Use dedicated test containers
   - Ensure clean state between tests
   - Configure appropriate timeouts

2. **Test Execution**
   - Include warmup phase
   - Run multiple iterations
   - Clean databases between runs
   - Use random ports for parallel tests

3. **Data Collection**
   - Track response times
   - Monitor error rates
   - Verify data consistency
   - Generate visual reports

4. **Analysis**
   - Compare strategy performance
   - Analyze percentile latencies
   - Review error patterns
   - Document findings

### Future Enhancements

1. **Additional Metrics**
   - Memory usage tracking
   - CPU utilization
   - Network performance
   - Database connection pool stats

2. **Extended Testing**
   - Load testing scenarios
   - Concurrent user simulation
   - Long-running stability tests
   - Resource consumption analysis

3. **Reporting**
   - Real-time metrics dashboard
   - Trend analysis
   - Performance regression detection
   - Automated performance alerts