# Local Deployment Guide

This guide provides step-by-step instructions for deploying and accessing the application locally using Docker.

## Prerequisites

- Docker Engine (20.10.0 or higher)
- Docker Compose (v2.0.0 or higher)
- Git
- 8GB RAM minimum (16GB recommended)
- 20GB free disk space

## Quick Start

```bash
# Clone the repository
git clone https://github.com/yourusername/mongo-kitchensink-modernization.git
cd mongo-kitchensink-modernization

# Start all services
docker compose up -d

# Check service status
docker compose ps

# Access the application
Frontend: http://localhost:3000
Backend API: http://localhost:8081
```

## Detailed Deployment Steps

### 1. Environment Setup

```bash
# Create a .env file in the root directory
cat > .env << EOL
# MongoDB Configuration
MONGODB_URI=mongodb://mongodb:27017/kitchensink
MONGODB_DATABASE=kitchensink

# PostgreSQL Configuration
POSTGRES_URL=jdbc:postgresql://postgres:5432/kitchensink
POSTGRES_USER=postgres
POSTGRES_PASSWORD=mysecretpassword
POSTGRES_DB=kitchensink

# Backend Configuration
SPRING_PROFILES_ACTIVE=docker
APP_DATABASE_TYPE=jpa
APP_DUAL_WRITE_ENABLED=true
APP_DATABASE_READ_SOURCE=jpa

# Flyway Configuration
SPRING_FLYWAY_ENABLED=true
SPRING_FLYWAY_BASELINE_ON_MIGRATE=true
SPRING_FLYWAY_VALIDATE_ON_MIGRATE=true

# Frontend Configuration
REACT_APP_API_URL=http://localhost:8081
NODE_ENV=production
EOL
```

### 2. Database Migrations

The application uses Flyway for database schema management. Migrations are automatically applied when the backend service starts.

```bash
# Start fresh (removes all data)
docker compose down -v
docker compose up -d postgres

# Wait for PostgreSQL to be ready
docker compose logs -f postgres

# Apply migrations through backend startup
docker compose up -d backend

# Verify migration status
docker compose exec backend java -jar app.jar --flyway:info

# Manual migration commands (if needed)
docker compose exec backend java -jar app.jar --flyway:migrate
docker compose exec backend java -jar app.jar --flyway:validate
```

#### Troubleshooting Migrations

```bash
# Check migration status
docker compose exec backend java -jar app.jar --flyway:info

# Repair migration history (for checksum mismatches)
docker compose exec backend java -jar app.jar --flyway:repair

# Clean and reapply migrations (WARNING: deletes all data)
docker compose exec backend java -jar app.jar --flyway:clean
docker compose exec backend java -jar app.jar --flyway:migrate
```

### 3. Start Individual Services

```bash
# Start databases first
docker compose up -d mongodb postgres

# Wait for databases to be ready (check logs)
docker compose logs -f mongodb postgres

# Start backend service
docker compose up -d backend

# Start frontend service
docker compose up -d frontend
```

### 4. Verify Deployment

```bash
# Check all container statuses
docker compose ps

# Check service logs
docker compose logs -f

# Check specific service logs
docker compose logs -f backend
docker compose logs -f frontend
```

### 5. Access Points

1. **Frontend Application**
   - URL: http://localhost:3000
   - Default credentials: none required
   - Features:
     * Member management interface
     * Search functionality
     * Form validation

2. **Backend API**
   - Base URL: http://localhost:8081
   - Swagger UI: http://localhost:8081/swagger-ui.html
   - Health check: http://localhost:8081/health
   - Key endpoints:
     * GET /api/v1/members
     * POST /api/v1/members
     * GET /api/v1/members/{id}
     * PUT /api/v1/members/{id}
     * DELETE /api/v1/members/{id}

3. **Database Access**
   - MongoDB:
     * Port: 27018 (mapped from 27017)
     * Connection string: mongodb://localhost:27018/kitchensink
   - PostgreSQL:
     * Port: 5433 (mapped from 5432)
     * Connection: postgresql://localhost:5433/kitchensink

### 6. Health Checks

```bash
# Check backend health
curl http://localhost:8081/health

# Check frontend
curl http://localhost:3000

# Check MongoDB
docker compose exec mongodb mongosh --eval "db.runCommand('ping')"

# Check PostgreSQL
docker compose exec postgres pg_isready -U postgres
```

### 7. Troubleshooting

1. **Container Issues**
   ```bash
   # Check container logs
   docker compose logs -f [service_name]

   # Restart specific service
   docker compose restart [service_name]

   # Rebuild service
   docker compose up -d --build [service_name]
   ```

2. **Database Connection Issues**
   ```bash
   # Check database logs
   docker compose logs mongodb
   docker compose logs postgres

   # Verify network connectivity
   docker compose exec backend ping mongodb
   docker compose exec backend ping postgres
   ```

3. **Migration Issues**
   ```bash
   # Check migration status
   docker compose exec backend java -jar app.jar --flyway:info

   # Common solutions:
   # - For checksum mismatches: Use flyway:repair
   # - For failed migrations: Check backend logs
   # - For clean start: Use docker compose down -v
   ```

4. **Common Problems & Solutions**

   - **Frontend can't connect to backend**:
     * Verify backend is running: `docker compose ps`
     * Check backend logs: `docker compose logs backend`
     * Verify API URL in frontend configuration

   - **Database connection failures**:
     * Check database logs
     * Verify credentials in .env file
     * Ensure databases are ready before starting backend

   - **Performance issues**:
     * Check container resources: `docker stats`
     * Verify host system resources
     * Consider scaling services if needed

### 8. Monitoring

1. **Resource Usage**
   ```bash
   # View container resource usage
   docker stats

   # Check disk usage
   docker system df
   ```

2. **Application Metrics**
   - Backend metrics: http://localhost:8081/actuator/metrics
   - Health status: http://localhost:8081/actuator/health
   - Environment info: http://localhost:8081/actuator/info

### 9. Maintenance

```bash
# Update images
docker compose pull

# Clean up unused resources
docker system prune

# Backup databases
docker compose exec postgres pg_dump -U postgres kitchensink > backup.sql
docker compose exec mongodb mongodump --out /backup

# View logs
docker compose logs --tail=100 -f
```

## Security Notes

- Default credentials are for local development only
- Change all passwords in production
- Secure exposed ports in production
- Enable HTTPS in production
- Implement proper authentication

## Next Steps

- Set up monitoring tools
- Configure backup strategy
- Implement CI/CD pipeline
- Plan production deployment
- Set up logging aggregation 