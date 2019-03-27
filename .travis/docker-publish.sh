#!/bin/sh

set -e

echo "$DOCKER_PASSWORD" | docker login -u "$DOCKER_USERNAME" --password-stdin
mvn git-commit-id:revision groovy:execute@docker-tags docker:push