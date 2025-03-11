# MongoDB Kitchensink Modernization

A demonstration project showing how to modernize a traditional Java EE application by migrating from PostgreSQL to MongoDB using the dual-write pattern, featuring both a legacy frontend and a modern React frontend.

## Project Overview

This project showcases a practical approach to migrating a traditional Java EE application from a relational database (PostgreSQL) to MongoDB. It implements the dual-write pattern to ensure data consistency during the migration process and includes two frontend implementations:
- A legacy frontend built with vanilla JavaScript
- A modern frontend built with React, TypeScript, and modern tooling

### Key Features

- **Dual-Write Pattern**: Write to both PostgreSQL and MongoDB simultaneously
- **Configurable Read Source**: Switch between data sources without code changes
- **Modern React Frontend**: Built with React 18, TypeScript, and Tailwind CSS
- **Legacy Frontend Support**: Traditional frontend for comparison
- **REST API**: Modern REST API built with Spring Boot
- **Containerized Deployment**: Docker and Docker Compose configuration
- **Comprehensive Testing**: Unit, integration, and performance tests

## Architecture

The application follows a modern architecture with these components:

- **Backend**: Spring Boot application with REST API endpoints
- **Modern Frontend**: React/TypeScript application with modern tooling
- **Legacy Frontend**: HTML5/JavaScript application
- **Databases**: PostgreSQL (legacy) and MongoDB (target)

### Migration Strategy

The application implements a three-phase migration strategy:

1. **Phase 1**: Dual-write to both databases, read from PostgreSQL
2. **Phase 2**: Dual-write to both databases, read from MongoDB
3. **Phase 3**: Write only to MongoDB, read from MongoDB

## Project Structure

```
mongo-kitchensink-modernization/
├── backend/                 # Spring Boot backend application
│   ├── src/                # Source code
│   ├── Dockerfile          # Docker configuration
│   └── README.md           # Backend documentation
├── frontend/               # Legacy web frontend
│   ├── webapp/             # Static web content
│   ├── Dockerfile         # Docker configuration
│   ├── nginx.conf        # Nginx configuration
│   └── README.md         # Frontend documentation
├── frontend_react/        # Modern React frontend
│   ├── src/              # React application source
│   ├── Dockerfile        # Docker configuration
│   ├── nginx.conf       # Nginx configuration
│   └── README.md        # React frontend documentation
├── docker-compose.yml    # Docker Compose configuration
└── README.md            # This file
```

## Getting Started

### Prerequisites

- Docker and Docker Compose
- Java 21 or later (for local development)
- Node.js 18 or later (for frontend development)
- Maven (for local development)
- PostgreSQL (for local development)
- MongoDB (for local development)

### Quick Start

```bash
# Clone the repository
git clone https://github.com/yourusername/mongo-kitchensink-modernization.git
cd mongo-kitchensink-modernization

# Start all services
docker compose up -d
```

### Access Points

- Modern React Frontend: http://localhost:3001
- Legacy Frontend: http://localhost:3000
- Backend API: http://localhost:8081
- Swagger UI: http://localhost:8081/swagger-ui.html
- MongoDB: localhost:27018
- PostgreSQL: localhost:5433

## Frontend Comparison

### Modern React Frontend (Port 3001)

- Built with React 18 and TypeScript
- Modern development experience with Vite
- Component-based architecture
- Efficient state management with React Query
- Beautiful UI with Tailwind CSS
- Comprehensive testing setup
- Type safety throughout the application

### Legacy Frontend (Port 3000)

- Built with vanilla JavaScript
- Traditional development approach
- Direct DOM manipulation
- Bootstrap for styling
- jQuery for DOM operations
- Basic form validation

## Development Workflow

### Backend Development

```bash
cd backend
mvn spring-boot:run
```

### Modern Frontend Development

```bash
cd frontend_react
npm install
npm run dev
```

### Legacy Frontend Development

```bash
cd frontend
# Using any simple HTTP server
npx http-server
```

## Configuration

### Environment Variables

Create a `.env` file in the root directory:

```env
# Database Configuration
MONGODB_URI=mongodb://mongodb:27017/kitchensink
POSTGRES_URL=jdbc:postgresql://postgres:5432/kitchensink
POSTGRES_USER=postgres
POSTGRES_PASSWORD=mysecretpassword

# Backend Configuration
SPRING_PROFILES_ACTIVE=docker
APP_DATABASE_TYPE=dual
APP_DUAL_WRITE_ENABLED=true

# Frontend Configuration
VITE_API_URL=http://localhost:8081
```

## Testing

### Running Backend Tests

```bash
cd backend
mvn test
```

### Running Modern Frontend Tests

```bash
cd frontend_react
npm test
```

### Running Performance Tests

```bash
docker compose --profile performance up
```

### Performance Testing Framework

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

## Deployment

### Production Considerations

1. Enable HTTPS for all services
2. Configure secure database credentials
3. Set up monitoring and logging
4. Configure backup strategy
5. Implement CI/CD pipeline

### Kubernetes Deployment

Basic Kubernetes manifests are provided in the `k8s/` directory:

```bash
kubectl apply -f k8s/
```

## Monitoring

- Backend metrics: http://localhost:8081/actuator/metrics
- Health status: http://localhost:8081/actuator/health
- Database health: Included in health endpoint

## Contributing

1. Fork the repository
2. Create a feature branch
3. Commit your changes
4. Push to the branch
5. Create a Pull Request

## License

This project is licensed under the MIT License.

## Acknowledgments

- Spring Boot team for the excellent framework
- MongoDB team for great documentation
- React team for the amazing frontend library
- The open-source community for various tools and libraries