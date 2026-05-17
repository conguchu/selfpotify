#!/usr/bin/env bash
# Variante de start.sh que borra la configuración del servidor almacenada
# localmente ANTES de arrancar la app web y el backend (arranque "limpio").
set -euo pipefail

ROOT="$(cd "$(dirname "$0")" && pwd)"
cd "$ROOT"

# Resuelve la ruta del config.yml: APP_CONFIG_PATH del .env si existe,
# o el default de application.properties (~/.selfpotify/config.yml).
CONFIG_PATH="$HOME/.selfpotify/config.yml"
if [[ -f ".env" ]]; then
  ENV_PATH="$(grep -E '^[[:space:]]*APP_CONFIG_PATH=' .env | tail -n1 | cut -d= -f2- || true)"
  if [[ -n "${ENV_PATH:-}" ]]; then
    CONFIG_PATH="${ENV_PATH/#\~/$HOME}"
  fi
fi

if [[ -f "$CONFIG_PATH" ]]; then
  echo "[selfpotify] borrando configuración local: $CONFIG_PATH"
  rm -f "$CONFIG_PATH"
else
  echo "[selfpotify] no hay configuración local que borrar ($CONFIG_PATH)"
fi

BACK_PID=""
FRONT_PID=""

cleanup() {
  echo ""
  echo "[selfpotify] parando procesos..."
  for pid in "$BACK_PID" "$FRONT_PID"; do
    if [[ -n "$pid" ]] && kill -0 "$pid" 2>/dev/null; then
      kill "$pid" 2>/dev/null || true
    fi
  done
  wait 2>/dev/null || true
}
trap cleanup INT TERM EXIT

if [[ ! -d "front" ]]; then
  echo "[selfpotify] No existe la carpeta 'front/'. Aborting." >&2
  exit 1
fi

echo "[selfpotify] arrancando backend en :8080..."
./mvnw -q spring-boot:run &
BACK_PID=$!

echo "[selfpotify] arrancando frontend en :8081..."
( cd front && pnpm dev ) &
FRONT_PID=$!

echo "[selfpotify] backend pid=$BACK_PID  frontend pid=$FRONT_PID"
echo "[selfpotify] Pulsa Ctrl+C para detener ambos."

wait
