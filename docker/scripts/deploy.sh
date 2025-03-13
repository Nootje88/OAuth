#!/bin/bash

set -e

# Default environment is development
ENV=${1:-dev}
COMPOSE_DIR="docker/compose"
PROJECT_NAME="oauth-${ENV}"

# Validate environment
case "$ENV" in
    dev|pat|prod|test)
        echo "Deploying for environment: $ENV"
        ;;
    *)
        echo "Error: Invalid environment '${ENV}'"
        echo "Usage: $0 [dev|pat|prod|test]"
        exit 1
        ;;
esac

# Check if .env file exists for the specified environment
if [ ! -f ".env.${ENV}" ]; then
    echo "Error: .env.${ENV} file not found"
    echo "Please create this file based on .env.template"
    exit 1
fi

# Copy the environment file to .env for Docker Compose to use
echo "Copying .env.${ENV} to .env for Docker Compose"
cp ".env.${ENV}" .env

# Set env var to help Docker Compose use the right file
export SPRING_PROFILES_ACTIVE=$ENV

# Deploy using Docker Compose
echo "Starting deployment with Docker Compose..."
if [[ "$ENV" == "prod" ]]; then
    echo "Running production deployment with high availability settings"
    docker-compose -p $PROJECT_NAME -f ${COMPOSE_DIR}/docker-compose.yml -f ${COMPOSE_DIR}/docker-compose.${ENV}.yml up -d --build

    # Additional production deployment steps
    echo "Scaling app service for high availability"
    docker-compose -p $PROJECT_NAME -f ${COMPOSE_DIR}/docker-compose.yml -f ${COMPOSE_DIR}/docker-compose.${ENV}.yml up -d --scale app=2
elif [[ "$ENV" == "test" ]]; then
    echo "Running tests in test environment"
    docker-compose -p $PROJECT_NAME -f ${COMPOSE_DIR}/docker-compose.yml -f ${COMPOSE_DIR}/docker-compose.${ENV}.yml up --build --abort-on-container-exit
    exit_code=$?

    echo "Cleaning up test environment"
    docker-compose -p $PROJECT_NAME -f ${COMPOSE_DIR}/docker-compose.yml -f ${COMPOSE_DIR}/docker-compose.${ENV}.yml down -v

    echo "Tests completed with exit code: $exit_code"
    exit $exit_code
else
    echo "Deploying $ENV environment"
    docker-compose -p $PROJECT_NAME -f ${COMPOSE_DIR}/docker-compose.yml -f ${COMPOSE_DIR}/docker-compose.${ENV}.yml up -d --build
fi

echo "Deployment completed successfully"
echo "Services status:"
docker-compose -p $PROJECT_NAME ps

# Print application URL
if [[ "$ENV" == "dev" ]]; then
    echo "Application available at: http://localhost:${SERVER_PORT:-8080}"
    echo "Swagger UI: http://localhost:${SERVER_PORT:-8080}/swagger-ui.html"
elif [[ "$ENV" == "prod" ]]; then
    echo "Application deployed to production environment"
    echo "API available at: https://api.yourdomain.com"
fi