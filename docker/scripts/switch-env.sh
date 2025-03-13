#!/bin/bash

# Script to switch between different environments
# Usage: ./switch-env.sh [dev|test|pat|prod]

# Set of valid environments
VALID_ENVS=("dev" "test" "pat" "prod")

# Function to show usage information
function show_usage() {
    echo "Usage: $0 [environment]"
    echo "Available environments: ${VALID_ENVS[*]}"
    echo "Example: $0 dev"
}

# Check if environment argument is provided
if [ $# -ne 1 ]; then
    show_usage
    exit 1
fi

ENV=$1

# Validate environment
VALID=false
for valid_env in "${VALID_ENVS[@]}"; do
    if [ "$ENV" == "$valid_env" ]; then
        VALID=true
        break
    fi
done

if [ "$VALID" = false ]; then
    echo "Error: Invalid environment '$ENV'"
    show_usage
    exit 1
fi

# Check if .env.$ENV file exists
if [ ! -f ".env.$ENV" ]; then
    echo "Error: Environment file .env.$ENV not found"
    echo "Please create this file based on .env.template"
    exit 1
fi

echo "Switching to $ENV environment..."

# Running with Docker
if [ "$USE_DOCKER" = true ] || [ "$2" = "--docker" ]; then
    echo "Using Docker Compose for $ENV environment"
    docker-compose -f docker/compose/docker-compose.yml -f docker/compose/docker-compose.$ENV.yml down
    SPRING_PROFILES_ACTIVE=$ENV docker-compose -f docker/compose/docker-compose.yml -f docker/compose/docker-compose.$ENV.yml up -d
else
    # Running with Maven
    echo "Using Maven for $ENV environment"
    echo "Stopping any running instance..."
    pkill -f "spring-boot:run" || true

    echo "Starting application with $ENV profile..."
    ./mvnw spring-boot:run -Dspring-boot.run.profiles=$ENV
fi

echo "Environment switched to $ENV"