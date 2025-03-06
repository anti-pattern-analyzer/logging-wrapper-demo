# Use the official OpenJDK 21 image
FROM eclipse-temurin:21-jdk as builder

# Set the working directory
WORKDIR /app

# Copy the JAR file (Assuming it's built via `mvn package` or `gradle build`)
COPY target/serviceapp.jar serviceapp.jar

# Expose the application port
EXPOSE 8080

# Run the Spring Boot application
CMD ["java", "-jar", "serviceapp.jar"]
