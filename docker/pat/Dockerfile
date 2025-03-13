FROM eclipse-temurin:23-jdk-focal as builder

WORKDIR /app

# Copy Maven files
COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .

# Make mvnw executable
RUN chmod +x mvnw

# Download dependencies
RUN ./mvnw dependency:go-offline

# Copy source code
COPY src src

# Build application
RUN ./mvnw clean package -DskipTests

FROM eclipse-temurin:23-jre-focal

WORKDIR /app
COPY --from=builder /app/target/*.jar app.jar

# Create directories
RUN mkdir -p /app/data/uploads/profiles \
    && mkdir -p /var/log/oauth-app

ENV SPRING_PROFILES_ACTIVE=pat \
    JAVA_OPTS="-Xms512m -Xmx1g" \
    SERVER_PORT=8080

EXPOSE ${SERVER_PORT}

ENTRYPOINT ["sh", "-c", "java ${JAVA_OPTS} -jar app.jar"]version: '3.8'

services:
  app:
    build:
      context: .
      dockerfile: .docker/test/Dockerfile
    environment:
      - SPRING_PROFILES_ACTIVE=test
      - DB_URL=jdbc:h2:mem:testdb
      - DB_USERNAME=sa
      - DB_PASSWORD=sa
    volumes:
      - ./src:/app/src
      - ./target:/app/target
    networks:
      - test-network

  db:
    image: h2:latest
    environment:
      - H2_OPTIONS=-ifNotExists
    ports:
      - "9092:9092"
    networks:
      - test-network

networks:
  test-network:
    driver: bridge