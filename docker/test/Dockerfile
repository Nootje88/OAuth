FROM eclipse-temurin:23-jdk-focal

WORKDIR /app

# Copy Maven files
COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .

# Make mvnw executable
RUN chmod +x mvnw

# Copy source code
COPY src src

# Create test directories
RUN mkdir -p /app/data/uploads/profiles/test \
    && mkdir -p /var/log/oauth-app/test

ENV SPRING_PROFILES_ACTIVE=test \
    SERVER_PORT=8080

EXPOSE ${SERVER_PORT}

# Run tests
CMD ["./mvnw", "test"]