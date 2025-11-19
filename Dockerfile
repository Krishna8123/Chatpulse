# Use official JDK 17
FROM eclipse-temurin:17-jdk

# Set working directory
WORKDIR /app

# Copy jar file from target folder
COPY target/chatpulse-1.0.0.jar app.jar


# Expose the port your app runs on
EXPOSE 8070

# Run the jar
ENTRYPOINT ["java", "-jar", "app.jar"]
