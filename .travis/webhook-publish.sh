#!/bin/bash

set -e

PROJECT_NAME=${PROJECT_NAME:=elasticsearch-proxy}
PROJECT_VERSION=$PROJECT_VERSION
WEBHOOK_URL=$WEBHOOK_URL
WEBHOOK_DATA='
{
    "text": "A new *'$PROJECT_NAME'* version *'$PROJECT_VERSION'* has been released.
             \nMore info about changes: https://github.com/novomatic-tech/elasticsearch-proxy/releases
             \nDocker image: https://hub.docker.com/r/novomatic/elasticsearch-proxy"
}'

if [[ "$PROJECT_VERSION" =~ -SNAPSHOT$ ]]; then
      echo "Notifications for unstable versions are disabled."
      exit 0
fi

echo $WEBHOOK_DATA | curl --fail -X POST -H 'content-type: application/json' --data @- $WEBHOOK_URL