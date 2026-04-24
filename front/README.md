# Selfpotify Frontend (demo)

Next.js 14 + React 18 + Tailwind. Demo UI para la API Selfpotify.

## Requisitos

- Node 18+
- Backend Spring Boot corriendo en `http://localhost:8080`

## Arranque

```bash
npm install
npm run dev
```

Abrir http://localhost:3000. Las llamadas a `/api/*` se proxean al backend via `next.config.js` (evita CORS).

## Notas

- El JWT se guarda en `localStorage`. Suficiente para demo local, no para producción.
- Para crear el primer admin, usa el endpoint `POST /api/auth/signup-admin` con la cabecera `X-Admin-Signup-Key` configurada en el backend, o haz seed en la BD.
