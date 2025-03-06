# Use a lightweight JDK runtime image
FROM eclipse-temurin:21-jdk

# Set working directory
WORKDIR /opt/app

# Copy the built JAR file from GitHub Actions
COPY target/serviceapp.jar serviceapp.jar

# Expose the application port
EXPOSE 8080

# Run the Spring Boot application
ENTRYPOINT ["java", "-jar", "/opt/app/serviceapp.jar"]
