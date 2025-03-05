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