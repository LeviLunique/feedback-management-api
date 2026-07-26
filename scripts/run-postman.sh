#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
COLLECTION="$ROOT_DIR/postman/feedback-management-api.postman_collection.json"
ENVIRONMENT="$ROOT_DIR/postman/feedback-management-api.environment.json"
EXTRA_ARGS=()
NETWORK_ARG=()

if [[ ! -f "$COLLECTION" ]]; then
  echo "Coleção não encontrada em $COLLECTION" >&2
  exit 1
fi

if [[ ! -f "$ENVIRONMENT" ]]; then
  echo "Ambiente não encontrado em $ENVIRONMENT" >&2
  exit 1
fi

BASE_URL_OVERRIDE="${BASE_URL:-}"
DEFAULT_BASE_URL="http://host.docker.internal:8080"

APP_CONTAINER="${APP_CONTAINER_NAME:-feedback-management-api}"
if docker inspect "$APP_CONTAINER" >/dev/null 2>&1; then
  NETWORK_NAME="$(docker inspect -f '{{range $k,$v := .NetworkSettings.Networks}}{{println $k}}{{end}}' "$APP_CONTAINER" | head -n1 | tr -d '[:space:]')"
  if [[ -n "$NETWORK_NAME" ]]; then
    NETWORK_ARG+=(--network "$NETWORK_NAME")
    DEFAULT_BASE_URL="http://$APP_CONTAINER:8080"
  fi
fi

if [[ -n "$BASE_URL_OVERRIDE" ]]; then
  echo "Usando BASE_URL override: $BASE_URL_OVERRIDE"
  EXTRA_ARGS+=(--env-var "base_url=$BASE_URL_OVERRIDE")
else
  echo "Usando base_url $DEFAULT_BASE_URL ${NETWORK_ARG[*]:+na rede ${NETWORK_ARG[1]}}"
  EXTRA_ARGS+=(--env-var "base_url=$DEFAULT_BASE_URL")
fi

PLATFORM_ARG=()
if [[ -n "${NEWMAN_PLATFORM:-}" ]]; then
  PLATFORM_ARG+=(--platform "${NEWMAN_PLATFORM}")
fi

docker run --rm ${PLATFORM_ARG+"${PLATFORM_ARG[@]}"} ${NETWORK_ARG+"${NETWORK_ARG[@]}"} \
  --add-host=host.docker.internal:host-gateway \
  -v "$ROOT_DIR:/etc/newman" postman/newman:5-alpine \
  run /etc/newman/postman/feedback-management-api.postman_collection.json \
  -e /etc/newman/postman/feedback-management-api.environment.json \
  --reporters cli \
  ${EXTRA_ARGS+"${EXTRA_ARGS[@]}"}
