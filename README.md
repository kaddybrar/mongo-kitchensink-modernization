# MongoDB Kitchensink Modernization

A demonstration project showing how to modernize a traditional Java EE application by migrating from PostgreSQL to MongoDB using the dual-write pattern.

## Project Overview

This project showcases a practical approach to migrating a traditional Java EE application from a relational database (PostgreSQL) to MongoDB. It implements the dual-write pattern to ensure data consistency during the migration process.

### Key Features

- **Dual-Write Pattern**: Write to both PostgreSQL and MongoDB simultaneously
- **Configurable Read Source**: Switch between data sources without code changes
- **REST API**: Modern REST API built with Spring Boot
- **Responsive Frontend**: Modern web interface built with HTML5, CSS3, and JavaScript
- **Containerized Deployment**: Docker and Docker Compose configuration for easy deployment
- **Comprehensive Testing**: Unit and integration tests for all components

## Architecture

The application follows a modern architecture with these components:

- **Backend**: Spring Boot application with REST API endpoints
- **Frontend**: HTML5/JavaScript application served by Nginx
- **Databases**: PostgreSQL (legacy) and MongoDB (target)

### Migration Strategy

The application implements a three-phase migration strategy:

1. **Phase 1**: Dual-write to both databases, read from PostgreSQL
2. **Phase 2**: Dual-write to both databases, read from MongoDB
3. **Phase 3**: Write only to MongoDB, read from MongoDB

This approach allows for a gradual, low-risk migration with the ability to roll back at any point.

## Project Structure

```
mongo-kitchensink-modernization/
├── backend/                 # Spring Boot backend application
│   ├── src/                 # Source code
│   ├── Dockerfile           # Docker configuration for backend
│   └── README.md            # Backend documentation
├── frontend/                # Web frontend
│   ├── webapp/              # Static web content
│   ├── Dockerfile           # Docker configuration for frontend
│   ├── nginx.conf           # Nginx configuration
│   └── README.md            # Frontend documentation
├── docker-compose.yml       # Docker Compose configuration
└── README.md                # This file
```

## Getting Started

### Prerequisites

- Docker and Docker Compose
- Java 17 or later (for local development)
- Maven (for local development)
- PostgreSQL (for local development)
- MongoDB (for local development)

### Running with Docker Compose

The easiest way to run the application is using Docker Compose:

```bash
# Clone the repository
git clone https://github.com/yourusername/mongo-kitchensink-modernization.git
cd mongo-kitchensink-modernization

# Start the application
docker-compose up -d
```

This will start:
- PostgreSQL database
- MongoDB database
- Backend Spring Boot application
- Frontend Nginx server

Access the application at http://localhost:80

### Docker Commands

Here are some useful Docker commands for managing the application:

#### View running containers
```bash
docker-compose ps
```

#### View logs
```bash
# View logs from all services
docker compose logs

# View logs from a specific service
docker compose logs backend
docker compose logs frontend

# Follow logs in real-time
docker-compose logs -f
```

#### Stop the application
```bash
docker compose down
```

#### Rebuild and restart services
```bash
# Rebuild and restart all services
docker compose up -d --build

# Rebuild and restart a specific service
docker compose up -d --build backend
```

#### Reset the environment
```bash
# Stop containers and remove volumes (will delete all data)
docker compose down -v
```

#### Scale services
```bash
# Run multiple instances of the backend
docker compose up -d --scale backend=3
```

### Database Migration Commands

To control the database migration process:

```bash
# Phase 1: Dual-write, read from PostgreSQL
docker compose exec backend java -jar app.jar --app.database.type=jpa --app.dual-write.enabled=true

# Phase 2: Dual-write, read from MongoDB
docker compose exec backend java -jar app.jar --app.database.type=mongo --app.dual-write.enabled=true

# Phase 3: MongoDB only
docker compose exec backend java -jar app.jar --app.database.type=mongo --app.dual-write.enabled=false
```

### Local Development Setup

#### Backend

```bash
cd backend
mvn spring-boot:run
```

The backend will be available at http://localhost:8080

#### Frontend

```bash
cd frontend/webapp
# Using any simple HTTP server, e.g.:
npx http-server
```

The frontend will be available at http://localhost:8080

## Configuration

### Database Selection

The application can be configured to use different database modes:

- **JPA Mode**: Uses only PostgreSQL
- **MongoDB Mode**: Uses only MongoDB
- **Dual-Write Mode**: Writes to both databases, configurable read source

These modes can be configured in `application.properties`:

```properties
# Options: jpa, mongo
app.database.type=jpa

# Read Source Configuration (for dual-write strategy)
# Options: jpa, mongo
app.database.read.source=jpa

# Dual-Write Options
app.dual-write.enabled=true
app.dual-write.compare=true
```

### Environment Variables

You can also configure the application using environment variables:

```bash
# Database type
export APP_DATABASE_TYPE=mongo

# Dual-write configuration
export APP_DUAL_WRITE_ENABLED=true
export APP_DATABASE_READ_SOURCE=mongo

# Database connection settings
export SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/kitchensink
export SPRING_DATASOURCE_USERNAME=postgres
export SPRING_DATASOURCE_PASSWORD=postgres

export SPRING_DATA_MONGODB_URI=mongodb://localhost:27017/kitchensink
```

## API Documentation

The REST API is documented using Swagger UI and is available at:

```
http://localhost:8080/swagger-ui.html
```

## Testing

### Running Backend Tests

```bash
cd backend
mvn test
```

### Running Integration Tests

```bash
cd backend
mvn verify -P integration-test
```

### Running Performance Tests via Docker

The project includes comprehensive performance testing capabilities for both frontend and backend components. All tests can be run using Docker Compose for consistent and isolated testing environments.

#### Quick Start
```bash
# Run all performance tests
./run-performance-tests.sh

# View results
open performance-reports/latest/index.html
```

#### Manual Test Execution
You can also run specific test components individually:

```bash
# Run only backend performance tests
docker compose --profile performance run --rm backend-performance-tests

# Run only frontend performance tests
docker compose --profile performance run --rm frontend-performance-tests

# Run with specific timestamp
TIMESTAMP=$(date +%Y%m%d_%H%M%S) docker compose --profile performance run --rm backend-performance-tests
```

#### Test Configuration
Performance tests can be configured through environment variables:

Backend Tests:
```properties
# Number of concurrent users
PERFORMANCE_TEST_USERS=100

# Test duration in seconds
PERFORMANCE_TEST_DURATION=300

# Target endpoints to test
PERFORMANCE_TEST_ENDPOINTS=/api/v1/members,/api/v1/health

# Database types to test
PERFORMANCE_TEST_DB_TYPE=mongo,jpa

# Warmup iterations
PERFORMANCE_TEST_WARMUP=10

# Main test iterations
PERFORMANCE_TEST_ITERATIONS=100
```

Frontend Tests:
```properties
# Browser type (chromium/firefox)
PERFORMANCE_TEST_BROWSER=chromium

# Number of test iterations
PERFORMANCE_TEST_ITERATIONS=3

# Network throttling (fast 3G, slow 3G)
PERFORMANCE_TEST_NETWORK=fast3G

# CPU throttling factor
PERFORMANCE_TEST_CPU_THROTTLE=4
```

#### Test Reports
Performance test results are organized by timestamp and stored in the `performance-reports` directory:

```
performance-reports/
├── latest/                     # Symlink to most recent test run
└── runs/
    └── YYYYMMDD_HHMMSS/       # Timestamped test results
        ├── index.html         # Combined test report
        ├── backend/           # Backend test results
        │   ├── database-strategy-comparison/
        │   └── startup-performance/
        └── frontend/          # Frontend test results
            └── performance-report.html
```

Each test run generates:
- HTML reports with interactive charts
- Raw performance data in JSON format
- Comparative analysis between runs
- Screenshots and traces (frontend tests)
- Resource utilization metrics

#### Metrics Collected

Backend Performance Tests measure:
- API response times (average, P95, P99)
- Database operation latencies
- Concurrent request handling
- Resource utilization under load
- Database comparison metrics (JPA vs MongoDB)
- Application startup times

Frontend Performance Tests measure:
- Page load times
- First Contentful Paint (FCP)
- Largest Contentful Paint (LCP)
- Time to Interactive (TTI)
- Component render times
- Network request latencies

#### Running Tests in CI/CD
For automated testing in CI/CD environments:

```bash
# Run with specific configuration
PERFORMANCE_TEST_USERS=50 \
PERFORMANCE_TEST_DURATION=60 \
PERFORMANCE_TEST_DB_TYPE=mongo \
./run-performance-tests.sh

# Run with fail conditions
PERFORMANCE_TEST_MAX_P95_LATENCY=500 \
PERFORMANCE_TEST_ERROR_RATE_THRESHOLD=1 \
./run-performance-tests.sh
```

#### Analyzing Results
The test results can be analyzed using:

```bash
# Generate comparison report with previous run
./analyze-performance.sh --compare-with previous

# Export metrics to CSV
./analyze-performance.sh --export csv

# Generate summary report
./analyze-performance.sh --summary
```

## Frontend Modernization Plan

The current frontend is built with vanilla JavaScript and Bootstrap. We have a plan to modernize it using React and TypeScript for better maintainability and developer experience.

### Planned Tech Stack

- **React**: For component-based UI development
- **TypeScript**: For type safety and better developer experience
- **React Router**: For client-side routing
- **Axios**: For API communication
- **Jest/React Testing Library**: For testing
- **Bootstrap or Material-UI**: For styling

### Implementation Phases

1. **Phase 1 (Current)**: Vanilla JavaScript with Bootstrap
   - Simple HTML/CSS/JS implementation
   - Direct DOM manipulation
   - Basic form validation

2. **Phase 2 (Planned)**: React with TypeScript
   - Component-based architecture
   - Strong typing with TypeScript
   - Improved state management
   - Enhanced form validation
   - Comprehensive testing

### Benefits of Migration

- **Improved Developer Experience**: Better tooling, type safety, and component reusability
- **Enhanced Maintainability**: Clearer code organization and separation of concerns
- **Better Performance**: Virtual DOM for efficient rendering
- **Improved Testing**: Easier to test components in isolation
- **Modern UX**: More responsive and interactive user interface

### Migration Strategy

We plan to implement the React frontend alongside the existing one, allowing for:

1. **Parallel Development**: Continue enhancing the current frontend while building the new one
2. **Incremental Adoption**: Gradually replace parts of the application
3. **A/B Testing**: Compare user experience between the two versions
4. **Seamless Transition**: Switch to the new frontend once feature parity is achieved

## Deployment

### Production Deployment Considerations

For production deployment:

1. Configure secure database credentials in environment variables
2. Enable HTTPS for both frontend and backend
3. Configure proper logging
4. Set up monitoring and alerting

### Kubernetes Deployment

For Kubernetes deployment, use the provided manifests:

```bash
# Apply Kubernetes manifests
kubectl apply -f k8s/

# Check deployment status
kubectl get pods
```

## Monitoring

The application includes Spring Boot Actuator endpoints for monitoring:

```
http://localhost:8080/actuator/health
http://localhost:8080/actuator/info
http://localhost:8080/actuator/metrics
```

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

## License

This project is licensed under the Apache License 2.0 - see the LICENSE file for details.

## Acknowledgments

- This project is based on the JBoss EAP Kitchensink quickstart
- Thanks to the Spring Boot and MongoDB teams for their excellent documentation