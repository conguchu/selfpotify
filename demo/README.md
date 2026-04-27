# Selfpotify Demo

Front-end mínimo en Next.js + React para probar el servicio de streaming.

## Cómo ejecutar

1. Arranca el backend (Spring Boot) en `localhost:8080`. En el primer arranque,
   `MusicLibraryLoader` escanea `~/Downloads` (configurable con
   `app.music.import-folder`) y crea las canciones.
2. Instala dependencias:
   ```bash
   cd demo
   npm install
   ```
3. Levanta el front:
   ```bash
   npm run dev
   ```
4. Abre http://localhost:3000 y entra con `user` / `password`.

## Variables de entorno

- `NEXT_PUBLIC_API_BASE` (default `http://localhost:8080`)

## Cómo funciona la auth en el `<audio>`

El elemento `<audio>` no permite enviar headers personalizados, así que el
filtro JWT acepta también el token vía query param (`?token=...`). Solo se usa
en este demo; en producción sería preferible una cookie httpOnly o un endpoint
de pre-firma.
