#!/usr/bin/env bash

set -e

if [ -z "${1}" ]
then
  docker compose down
  docker compose up -d
else
    COMPOSE_FILES=""
    for file in "$@"; do
        COMPOSE_FILES="$COMPOSE_FILES -f $file"
    done

    eval "docker compose $COMPOSE_FILES down"
    eval "docker compose $COMPOSE_FILES up -d"
fi

echo "Wait until mysql initializes..."

while [ "$(docker inspect -f {{.State.Health.Status}} easypay-mysql)" != "healthy" ]; do
    sleep 1
done

cd ../
./gradlew flywayClean flywayMigrate
cd ..