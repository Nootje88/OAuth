FROM eclipse-temurin:23-jdk
WORKDIR /app

COPY mvnw ./
COPY .mvn .mvn
COPY pom.xml ./
RUN chmod +x mvnw && ./mvnw -q dependency:go-offline

COPY src ./src
ENV SPRING_PROFILES_ACTIVE=test SERVER_PORT=8080
EXPOSE 8080

CMD ["./mvnw","-q","test"]
