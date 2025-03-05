# Multi-stage build
FROM eclipse-temurin:21-jdk-alpine as build
WORKDIR /workspace/app

# Install Maven
RUN apk add --no-cache maven

# Copy the project files
COPY . .

# Package the application
RUN mvn package -DskipTests

# Create a slim runtime image
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Copy the jar file from the build stage
COPY --from=build /workspace/app/target/*.jar app.jar

# Add CORS configuration
ENV SPRING_PROFILES_ACTIVE=docker

# Run the application
ENTRYPOINT ["java","-jar","/app/app.jar"]

# Expose the port
EXPOSE 8080