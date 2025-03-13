#!/bin/bash

# Default Java options if not provided
JAVA_OPTS=${JAVA_OPTS:-"-Xms512m -Xmx1g"}

# Get active Spring profile
SPRING_PROFILE=${SPRING_PROFILES_ACTIVE:-"dev"}

echo "Starting application with profile: $SPRING_PROFILE"
echo "Java options: $JAVA_OPTS"

# Log critical environment variables (without values for security)
echo "Checking required environment variables..."
# Check database configuration
if [ -z "$DB_URL" ]; then
    echo "ERROR: DB_URL is not set!"
    exit 1
fi
if [ -z "$DB_USERNAME" ]; then
    echo "ERROR: DB_USERNAME is not set!"
    exit 1
fi
if [ -z "$DB_PASSWORD" ]; then
    echo "ERROR: DB_PASSWORD is not set!"
    exit 1
fi
if [ -z "$JWT_SECRET" ]; then
    echo "ERROR: JWT_SECRET is not set!"
    exit 1
fi

echo "All required environment variables are set."

# Run the application
exec java $JAVA_OPTS -jar app.jar