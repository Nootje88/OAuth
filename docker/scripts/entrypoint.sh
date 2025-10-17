#!/bin/bash
set -e

JAVA_OPTS=${JAVA_OPTS:-"-Xms512m -Xmx1g"}
SPRING_PROFILE=${SPRING_PROFILES_ACTIVE:-"dev"}

echo "Starting application with profile: $SPRING_PROFILE"
echo "Java options: $JAVA_OPTS"

# Required envs
missing=0
for v in DB_URL DB_USERNAME DB_PASSWORD JWT_SECRET; do
  if [ -z "${!v}" ]; then
    echo "ERROR: $v is not set!"
    missing=1
  fi
done
[ "$missing" -eq 1 ] && exit 1

echo "All required environment variables are set."
exec java $JAVA_OPTS -jar /app/app.jar --spring.profiles.active="$SPRING_PROFILE"
