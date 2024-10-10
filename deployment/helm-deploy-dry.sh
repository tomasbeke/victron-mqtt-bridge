#!/usr/bin/env bash
export DOCKER_REGISTRY_BASE=localhost:5000
export PROJECT_NAME=home-monitoring
helm upgrade --dry-run --debug --install -f ./helm/values-local.yaml --wait --timeout 1m30s --set image.version=latest --set image.prefix=$DOCKER_REGISTRY_BASE/$PROJECT_NAME victron-mqtt-bridge ./helm