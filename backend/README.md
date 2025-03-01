# Kitchen Sink Backend Service

A modernized Spring Boot REST API service for member management with CRUD operations and validation.

## Overview

This service provides a RESTful API for managing members with features including:
- Complete CRUD operations for members
- Email uniqueness validation
- Name-based search functionality
- Comprehensive error handling
- OpenAPI/Swagger documentation
- Health monitoring endpoint

## Tech Stack

- Java 17
- Spring Boot 3.1.0
- Spring Data JPA
- H2 Database
- OpenAPI 3.0 (Swagger)
- Lombok
- Maven

## Error Handling

The service implements comprehensive error handling through `GlobalExceptionHandler`:

### Error Types
- `400 Bad Request`: Invalid input validation
- `404 Not Found`: Member not found
- `409 Conflict`: Email already exists
- `500 Internal Server Error`: Server error

## Data Validation

### Member Entity Validation
- Name: Required field
- Email: Required field, must be unique and valid email format
- Phone Number: Optional field, must match international format pattern

## Project Structure
    src/main/java/com/mongo/kitchensink/
    ├── config/
    │ └── OpenApiConfig.java # Swagger/OpenAPI configuration
    ├── controller/
    │ ├── MemberController.java # Member REST endpoints
    │ └── HealthController.java # Health check endpoint
    ├── exception/
    │ ├── DuplicateEmailException.java
    │ ├── MemberNotFoundException.java
    │ ├── GlobalExceptionHandler.java
    │ └── ErrorResponse.java
    ├── model/
    │ └── Member.java # Member entity with validation
    ├── repository/
    │ └── MemberRepository.java # Data access layer
    └── service/
    └── MemberService.java # Business logic layer

## Running the Application

1. Prerequisites:
   - JDK 17
   - Maven 3.x

2. Build:
    mvn clean install

3. Run:
```bash
mvn spring-boot:run
```

4. Access:
   - API: `http://localhost:8080`
   - Swagger UI: `http://localhost:8080/swagger-ui.html`
   - API Docs: `http://localhost:8080/api-docs`
   - H2 Console: `http://localhost:8080/h2-console`


### Testing
```bash
mvn test
```

### Building for Production
```bash
mvn clean package
```

## Documentation

The API is documented using OpenAPI 3.0 (Swagger) and can be accessed at:
- Swagger UI: `http://localhost:8080/swagger-ui.html`
- OpenAPI Spec: `http://localhost:8080/api-docs`

## License

This project is licensed under the Apache License 2.0