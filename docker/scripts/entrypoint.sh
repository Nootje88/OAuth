#!/bin/bash

# Default Java options if not provided
JAVA_OPTS=${JAVA_OPTS:-"-Xms512m -Xmx1g"}

# Get active Spring profile
SPRING_PROFILE=${SPRING_PROFILES_ACTIVE:-"dev"}

echo "Starting application with profile: $SPRING_PROFILE"
echo "Java options: $JAVA_OPTS"

# Run the application
exec java $JAVA_OPTS -jar app.jar