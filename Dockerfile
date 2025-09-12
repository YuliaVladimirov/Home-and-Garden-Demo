# Use Eclipse Temurin JRE 23 (Alpine variant for smaller size)
FROM eclipse-temurin:23-jre-alpine

# Set a working directory
WORKDIR /app

# Copy your built JAR file into the container
COPY target/*.jar app.jar

# Expose port (if needed for health checks or future REST API)
EXPOSE 8080

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]
