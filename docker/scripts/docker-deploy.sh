#!/bin/bash

ENV=$1
COMPOSE_DIR="docker/compose"

if [ -z "$ENV" ]; then
    echo "Usage: $0 <env> (dev|pat|prod|test)"
    exit 1
fi

if [ ! -f ".env.${ENV}" ]; then
    echo "Error: .env.${ENV} file not found"
    exit 1
fi

export $(cat .env.${ENV} | grep -v '^#' | xargs)

case "$ENV" in
    "dev")
        docker-compose -f ${COMPOSE_DIR}/docker-compose.yml \
                      -f ${COMPOSE_DIR}/docker-compose.dev.yml \
                      up --build -d
        ;;
    "pat")
        docker-compose -f ${COMPOSE_DIR}/docker-compose.yml \
                      -f ${COMPOSE_DIR}/docker-compose.pat.yml \
                      up --build -d
        ;;
    "prod")
        docker-compose -f ${COMPOSE_DIR}/docker-compose.yml \
                      -f ${COMPOSE_DIR}/docker-compose.prod.yml \
                      up --build -d
        ;;
    "test")
        docker-compose -f ${COMPOSE_DIR}/docker-compose.yml \
                      -f ${COMPOSE_DIR}/docker-compose.test.yml \
                      up --build
        ;;
    *)
        echo "Invalid environment: $ENV"
        exit 1
        ;;
esac