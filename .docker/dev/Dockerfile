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

ENV SERVER_PORT=8080

EXPOSE ${SERVER_PORT}

ENTRYPOINT ["sh", "-c", "java -jar app.jar"]