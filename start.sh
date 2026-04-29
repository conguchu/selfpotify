#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "$0")" && pwd)"
cd "$ROOT"

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

echo "[selfpotify] arrancando frontend en :3000..."
( cd front && pnpm dev ) &
FRONT_PID=$!

echo "[selfpotify] backend pid=$BACK_PID  frontend pid=$FRONT_PID"
echo "[selfpotify] Pulsa Ctrl+C para detener ambos."

wait
