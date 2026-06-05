

<div align="center">
  <img src="img/selfpotify-logo.png" alt="selfpotify-logo" width="240"/>
  <h1>SelfPotify</h1>
</div>

# Instalación

> **Primer arranque.** Independientemente del modo de despliegue, la primera vez que accedas al cliente web se abrirá un **wizard de configuración inicial** (sin login) donde configurarás el branding, las carpetas de música y los usuarios. Al completarlo el servidor queda operativo.

## Bare metal

**Requisitos:** Java 21+, Node.js 20+, Maven 3.9+.

1. Clona el repositorio.

2. Copia `.env.example` a `.env` y ajusta al menos estas variables:

   | Variable | Qué poner |
   |---|---|
   | `DB_URL` | `jdbc:h2:file:./data/selfpotify;AUTO_SERVER=TRUE` (ruta relativa al proyecto) |
   | `JWT_SECRET` | Cadena aleatoria ≥ 32 caracteres (`openssl rand -base64 48`) |
   | `ADMIN_USERNAME` / `ADMIN_PASSWORD` | Credenciales del primer admin (auto-bootstrap) |
   | `LASTFM_API_KEY` | *(Opcional)* Activa la clasificación automática de géneros |

3. Arranca el backend:

   ```bash
   mvn spring-boot:run
   ```

   Escucha en el puerto indicado en `SERVER_PORT` (por defecto `8080`).

4. Configura el frontend. Crea `front/.env.local` con:

   ```
   NEXT_PUBLIC_API_BASE=http://localhost:8080
   ```

5. Arranca el frontend:

   ```bash
   cd front
   npm install
   npm run dev      # desarrollo
   # o
   npm run build && npm start   # producción
   ```

   Escucha en `http://localhost:3000`.

6. Abre `http://localhost:3000` y completa el wizard.

   Las carpetas de música se añaden desde el wizard (y también desde el panel admin una vez instalado). Las canciones subidas vía drag & drop en el panel se guardan en `selfpotify_added` dentro de la carpeta de música que elijas.

---

## Docker

**Requisitos:** Docker con el plugin Compose (o `docker-compose` v1).

1. Clona el repositorio.

2. Copia `.env.example` a `.env` y ajusta al menos estas variables:

   | Variable | Qué poner |
   |---|---|
   | `MUSIC_LIBRARY_PATH` | Ruta absoluta del host con tus MP3 (obligatorio) |
   | `JWT_SECRET` | Cadena aleatoria ≥ 32 caracteres (`openssl rand -base64 48`) |
   | `ADMIN_USERNAME` / `ADMIN_PASSWORD` | Credenciales del primer admin (auto-bootstrap) |
   | `DB_URL` | `jdbc:h2:file:/data/selfpotify/db/selfpotify;AUTO_SERVER=TRUE` |
   | `WEB_PORT` | Puerto público de Nginx (por defecto `80`; cámbialo si está ocupado) |
   | `LASTFM_API_KEY` | *(Opcional)* Activa la clasificación automática de géneros |

   > En macOS, si `MUSIC_LIBRARY_PATH` apunta a una carpeta fuera de `$HOME`, añádela en **Docker Desktop → Settings → Resources → File Sharing**.

3. Construye y levanta la pila:

   ```bash
   docker-compose up --build -d
   ```

   Esto arranca tres contenedores:
   - **`api`** — Spring Boot en `:8080` (solo red interna).
   - **`next`** — Frontend Next.js en `:3000` (solo red interna).
   - **`web`** — Nginx en el puerto `WEB_PORT` (único puerto público). Enruta `/api/*` y `/assets/*` al backend y el resto al frontend.

4. Abre `http://localhost` (o el puerto que hayas puesto en `WEB_PORT`) y completa el wizard.

   Los datos del servidor (`config.yml`, BBDD, assets y canciones subidas) se persisten en el volumen Docker `selfpotify-data`, por lo que sobreviven a reinicios y rebuilds.
