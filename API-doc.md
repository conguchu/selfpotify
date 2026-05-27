# Documentación de la API de Selfpotify

API REST de Spring Boot 4.0.5 con autenticación JWT. El servidor escucha por defecto en `http://localhost:8080`.

**Persistencia:**
- Datos de música, usuarios y playlists → H2 en memoria (`create-drop` en cada arranque, no sobrevive a reinicios).
- Configuración del servidor (branding, rutas a escanear, flags) → fichero YAML externo en `~/.selfpotify/config.yml` (sobrevive a reinicios). Override vía `app.config.path`.

> Estado: este documento refleja el código fuente real (controllers + WebSecurityConfig + DTOs). No incluye comportamientos no implementados.

---

## 0. Convenciones

- **Base URL local:** `http://localhost:8080`
- **Auth:** se envía como `Authorization: Bearer <jwt>` salvo en el endpoint de streaming, donde también se acepta el JWT vía query param `?token=<jwt>` (necesario porque `<audio>` no permite headers personalizados — implementado en `AuthTokenFilter.java:60-65`).
- **Tipo de contenido:** `application/json` para request/response salvo el streaming, que devuelve `audio/*`.
- **Roles:** `ROLE_USER` y `ROLE_ADMIN`. El discriminador JPA `users.type` (`USER` / `ADMIN`) determina el rol. Un `ADMIN` puede reasignarlo con `PUT /api/users/{id}/role` (ver §7).
- **CORS:** abierto a cualquier origen (`*`); credenciales permitidas; cabeceras expuestas: `Content-Range`, `Accept-Ranges`, `Content-Length`.
- **Sesión:** stateless, sin cookies. El token caduca a las 24 horas.
- **Datos seed (`DataLoader`):** en cada arranque se garantizan los usuarios `user / password` (USER) y `admin / admin123` (ADMIN).

---

## 1. Autenticación y registro (`/api/auth`)

### `POST /api/auth/login` — Iniciar sesión

- **Acceso:** público.
- **Body:**
  ```json
  { "username": "user", "password": "password" }
  ```
- **Respuesta `200 OK`:**
  ```json
  {
    "token": "eyJhbGciOiJIUzI1NiJ9...",
    "type": "Bearer",
    "username": "user",
    "roles": ["ROLE_USER"]
  }
  ```
- **Errores:** `401 Unauthorized` si las credenciales no son válidas.

### `POST /api/auth/signup` — Registro de usuario normal

- **Acceso:** público.
- **Body:**
  ```json
  { "username": "nuevo", "password": "secret" }
  ```
- **Respuesta `200 OK`:** `"User registered successfully!"`
- **Notas:** el campo `isAdmin` del body es **ignorado**; siempre crea un `ROLE_USER`.
- **Errores:** `400` con `"Error: Username is already taken!"`.

### `POST /api/auth/signup-admin` — Registro de administrador

- **Acceso:** público pero requiere la cabecera `X-Admin-Signup-Key` con el valor de `app.admin.signup-key` (por defecto `changeme-admin-key`, configurable en `application.properties`).
- **Body:**
  ```json
  { "username": "admin2", "password": "secret" }
  ```
- **Respuesta `200 OK`:** `"Admin registered successfully!"`
- **Errores:** `403 Forbidden` con `"Error: Invalid admin signup key"` si la cabecera falta o es incorrecta; `400` si el username está cogido.

---

## 2. Canciones (`/api/songs`)

### `GET /api/songs`
- **Acceso:** `ROLE_USER` o `ROLE_ADMIN`.
- **Respuesta `200 OK`:** `List<SongDTO>` (ver §8).

### `GET /api/songs/{id}`
- **Acceso:** `ROLE_USER` o `ROLE_ADMIN`.
- **Respuesta:** `200 OK SongDTO` o `404 Not Found`.

### `GET /api/songs/{genre}/top` — Top 10 canciones de un género
- **Acceso:** `ROLE_USER` o `ROLE_ADMIN`.
- **Comportamiento:** devuelve las 10 canciones disponibles del género ordenadas por escuchas (desc), **derivadas** de `user_song_listen`.
- **Respuesta:** `200 OK Top10GenreSongsDTO` (`{ "genre": "...", "top": List<SongDTO> }`) o `404 Not Found` si el género no tiene canciones.

### `POST /api/songs` — Crear canción manual
- **Acceso:** `ROLE_ADMIN`.
- **Body:** entidad `Song` completa (`title`, `duration_ms`, `genre`, `bpm`, `songPath` absoluto, `picture_url`, `available`, opcionalmente `artists`, `album`).
- **Respuesta:** `200 OK SongDTO` de la canción creada.

### `PUT /api/songs/{id}`
- **Acceso:** `ROLE_ADMIN`.
- **Body:** entidad `Song` con los campos a actualizar.
- **Respuesta:** `200 OK SongDTO` o `404 Not Found`.

### `DELETE /api/songs/{id}`
- **Acceso:** `ROLE_ADMIN`.
- **Respuesta:** `204 No Content` o `404 Not Found`.

### `POST /api/songs/import` — Escanear carpeta del servidor (one-shot)
- **Acceso:** `ROLE_ADMIN`.
- **Body:**
  ```json
  { "path": "/ruta/absoluta/a/carpeta" }
  ```
- **Comportamiento:** recorre recursivamente la carpeta, extrae metadatos ID3 de los `.mp3` y `.wav` (`SongService.loadFolder`), persiste cada canción y devuelve la lista importada como `List<SongDTO>`.
- **Respuesta `200 OK`:** lista de las canciones recién persistidas.
- **Errores:** `400 Bad Request` si la ruta está vacía / no existe / no es directorio / no es legible; `500 Internal Server Error` si falla la lectura.
- **Nota:** importación manual one-shot. La ruta NO queda registrada para re-escaneo. Para escaneo persistente con re-escaneo periódico usar `POST /api/config/scan-paths` (§7.5).

---

## 3. Artistas (`/api/artists`)

| Método | Path | Acceso | Body | Respuesta |
|---|---|---|---|---|
| GET | `/api/artists` | USER/ADMIN | — | `List<ArtistDTO>` |
| GET | `/api/artists/{id}` | USER/ADMIN | — | `ArtistDTO` o `404` |
| GET | `/api/artists/{id}/top-10-tracks` | USER/ADMIN | — | `Top10ArtistTracksDTO` o `404` |
| POST | `/api/artists` | ADMIN | `Artist` | `ArtistDTO` |
| PUT | `/api/artists/{id}` | ADMIN | `Artist` | `ArtistDTO` o `404` |
| DELETE | `/api/artists/{id}` | ADMIN | — | `204` o `404` |

Nota: `ArtistDTO.biography` está declarado pero el `convertToDTO` actual no lo rellena; siempre será `null`.

Nota: `GET /api/artists/{id}/top-10-tracks` devuelve `{ "tracks": List<SongDTO> }` con las 10 canciones del artista más escuchadas, **derivadas** de `user_song_listen`. Cada track es un `SongDTO` con su campo `listeners` (popularidad derivada) resuelto mediante una única consulta agrupada, igual que el resto de listados.

---

## 4. Álbumes (`/api/albums`)

| Método | Path | Acceso | Body | Respuesta |
|---|---|---|---|---|
| GET | `/api/albums` | USER/ADMIN | — | `List<AlbumDTO>` |
| GET | `/api/albums/{id}` | USER/ADMIN | — | `AlbumDTO` o `404` |
| POST | `/api/albums` | ADMIN | `Album` | `AlbumDTO` |
| PUT | `/api/albums/{id}` | ADMIN | `Album` | `AlbumDTO` o `404` |
| DELETE | `/api/albums/{id}` | ADMIN | — | `204` o `404` |

Nota: `AlbumDTO.releaseDate` y `AlbumDTO.artistId` están declarados pero el `convertToDTO` no los rellena; siempre serán `null`.

---

## 5. Playlists (`/api/playlists`)

Todos los endpoints requieren `ROLE_USER` o `ROLE_ADMIN`.

### `GET /api/playlists/my`
- Lista todas las playlists del usuario autenticado (privadas y públicas).
- **Respuesta:** `List<PlaylistDTO>`.

### `GET /api/playlists/user/{userId}`
- Playlists **públicas** de otro usuario.
- **Respuesta:** `200 OK List<PlaylistDTO>` o `404` si el usuario no existe.

### `GET /api/playlists/{id}`
- **Reglas:**
  - Si la playlist es pública → cualquier autenticado puede verla.
  - Si es privada → solo el creador.
- **Respuesta:** `200 OK PlaylistDTO`, `403 Forbidden`, o `404 Not Found`.

### `POST /api/playlists` — Crear
- **Body:**
  ```json
  {
    "name": "Mi mix",
    "description": "Opcional",
    "isPublic": false,
    "songIds": [1, 2, 3]
  }
  ```
- El `creatorId` se asigna automáticamente al usuario autenticado.
- **Respuesta:** `201 Created PlaylistDTO`.

### `PUT /api/playlists/{id}` — Actualizar
- Solo el creador puede modificarla. Cuerpo igual al de creación.
- **Respuesta:** `200 OK PlaylistDTO`, `403`, o `404`.

### `DELETE /api/playlists/{id}` — Borrar
- El creador o un admin pueden borrar.
- **Respuesta:** `204 No Content`, `403`, o `404`.

---

## 6. Streaming de audio (`/api/listen`)

### `GET /api/listen/{songId}`

- **Acceso:** autenticado. El JWT puede llegar en `Authorization: Bearer <token>` **o** como query param `?token=<token>` (única forma compatible con el elemento HTML `<audio>`).
- **Cabeceras de petición:** opcional `Range: bytes=start-end` para streaming progresivo (HTTP Range, RFC 7233).
- **Respuesta `200 OK`** (sin Range): el archivo completo, con cabeceras
  - `Content-Type: audio/mpeg | audio/wav | audio/ogg | audio/flac | audio/aac | application/octet-stream`
  - `Content-Length: <bytes>`
  - `Accept-Ranges: bytes`
  - `Cache-Control: public, max-age=3600, immutable`
- **Respuesta `206 Partial Content`** (con Range válido): chunk solicitado, cabecera `Content-Range: bytes start-end/total`.
- **Respuesta `416 Requested Range Not Satisfiable`** si el Range es inválido.
- **Respuesta `404 Not Found`** si la canción no existe o el archivo (`Song.songPath`) no es accesible (en cuyo caso además se marca `Song.available = false`).

**Efectos secundarios:** cada llamada exitosa registra una fila en la tabla de eventos `user_song_listen` (`UserSongListenService.recordListen`) y apila el género de la canción en el feed del usuario (`registerGenreListen`). **No** se incrementa ningún contador numérico: la popularidad de canciones, álbumes, artistas y géneros se deriva por consulta a partir de esos eventos (ver §6.5 y la nota de `SongDTO` en §8). La tabla de eventos se acota a 1000 registros por usuario con descarte FIFO.

---

## 6.5 Feed del home (`/api/feed`)

Acceso `ROLE_USER` o `ROLE_ADMIN`. El feed se calcula a partir del historial de escuchas del usuario (`user_song_listen`).

### `GET /api/feed`

- **Comportamiento:** regenera y devuelve el feed **personalizado** del usuario autenticado en cada acceso al home. `UserFeedService.recommendArtistsForUser` toma los artistas que más ha escuchado el propio usuario (derivado de `user_song_listen`) y, si no llegan a 10, completa con la popularidad global (artistas más escuchados en toda la instalación) sin repetir. Un usuario sin historial recibe directamente la popularidad global (cold-start). La lista resultante (máx. 10, sin duplicados) sobrescribe los artistas recomendados del feed; la pila de géneros recientes no se altera.
- **Respuesta `200 OK`:** `List<ArtistDTO>` (ver §8) con los artistas recomendados.

### `GET /api/feed/genres`

- **Comportamiento:** devuelve los géneros escuchados más recientemente por el usuario autenticado, del más reciente al más antiguo (pila acotada a 20, índice 0 = más reciente).
- **Respuesta `200 OK`:** `List<String>` con como máximo los 10 géneros más recientes.

---

## 7. Administración de usuarios (`/api/users`)

Acceso exclusivo `ROLE_ADMIN`.

| Método | Path | Body | Respuesta |
|---|---|---|---|
| GET | `/api/users` | — | `List<User>` (campo `type` indica USER/ADMIN; `password` no se serializa) |
| GET | `/api/users/{id}` | — | `User` o `404` |
| POST | `/api/users` | `{ "username", "password", "isAdmin" }` | `200 OK "User created successfully by admin!"`; `400` si el username está cogido |
| PUT | `/api/users/{id}` | `User` (si trae `password` se reencripta automáticamente) | `200 OK User` o `404` |
| PUT | `/api/users/{id}/role` | `{ "isAdmin": true\|false }` | `200 OK User` (con el `type` ya actualizado); `400` si se intenta degradar al último administrador; `404` si no existe |
| DELETE | `/api/users/{id}` | — | `204 No Content` o `404` |

**Cambio de rol:** `PUT /api/users/{id}/role` reasigna el discriminador JPA `users.type` (`USER` ⇄ `ADMIN`) mediante una query nativa, refrescando el contexto de persistencia. Si el body no cambia el rol actual, devuelve el usuario sin tocarlo. No permite degradar al último `ADMIN` existente (responde `400`).

---

## 7.5 Configuración del servidor (`/api/config`)

La configuración (branding, rutas a escanear, flags) vive en `~/.selfpotify/config.yml` y se mantiene en memoria como copia volátil. Las escrituras se hacen a fichero temporal + `ATOMIC_MOVE`. **No** vive en H2 (que está en `create-drop`).

### `GET /api/config/public`
- **Acceso:** público (sin auth). El frontend lo consume antes del login para aplicar branding y para decidir si mostrar el wizard de setup.
- **Respuesta `200 OK PublicConfigDTO`:**
  ```json
  {
    "branding": {
      "appName": "selfpotify",
      "logoUrl": "/assets/logo.png",
      "colors": { "--color-bg": "#0a0a0a", "--color-accent": "#b91c1c", ... }
    },
    "setupComplete": false,
    "lastfmEnabled": true,
    "musicLibraryPath": "/music",
    "logoMaxBytes": 2097152
  }
  ```
  - `setupComplete`: `false` mientras no se haya completado el wizard inicial.
  - `lastfmEnabled`: `true` si hay `LASTFM_API_KEY` configurada (habilita autocompletar metadatos).
  - `musicLibraryPath`: ruta de librería musical auto-detectada del `.env` (`/music` en Docker o `MUSIC_LIBRARY_PATH` en host), o `null` si no hay ninguna.
  - `logoMaxBytes`: tamaño máximo en bytes admitido por `POST /api/config/logo` (configurable vía `LOGO_MAX_FILE_SIZE`). El cliente lo usa para mostrar el límite y redimensionar la imagen antes de subirla.

### `GET /api/config`
- **Acceso:** `ROLE_ADMIN`.
- **Respuesta `200 OK ServerConfigDTO`** (ver §8).

### `PUT /api/config`
- **Acceso:** `ROLE_ADMIN`.
- **Body:** `ConfigUpdateRequest` — todos los campos opcionales; los nulos se dejan sin tocar.
  ```json
  {
    "branding": { "appName": "myTunes", "colors": { "--color-accent": "#22c55e" } },
    "autoCompleteMetadata": true,
    "scanIntervalSeconds": 600
  }
  ```
- **Validaciones:**
  - `appName`: no en blanco, máx 64 caracteres.
  - Cada color: hex `#RGB` o `#RRGGBB`.
  - `scanIntervalSeconds`: entre 30 y 86400.
- **Respuesta:** `ServerConfigDTO` actualizado.
- **Hot-reload:** el cambio de `scanIntervalSeconds` se aplica en el siguiente tick del scheduler **sin reinicio**.

### `POST /api/config/setup` — Wizard de configuración inicial (commit)
- **Acceso:** `ROLE_ADMIN` **o "modo setup"** (ver nota al final de §7.5): mientras `setupComplete=false` es accesible **sin login**.
- **Body:** `SetupRequest` — todos los campos opcionales.
  ```json
  {
    "appName": "selfpotify",
    "scanPaths": ["/ruta/extra"],
    "scanIntervalSeconds": 3600,
    "autoCompleteMetadata": false
  }
  ```
- **Comportamiento:** valida y persiste branding/intervalo/flags y cada ruta de `scanPaths` (deben existir y ser legibles), marca `setupComplete=true` y **lanza un escaneo inicial asíncrono si hay CUALQUIER ruta configurada** — incluida la librería auto-añadida del `.env` (ver nota), no solo las del body. Tras esto el wizard queda inaccesible y los endpoints del modo setup vuelven a exigir `ROLE_ADMIN`.
- **Validaciones:** `appName` máx 64; `scanIntervalSeconds` entre 30 y 86400; rutas existentes y legibles.
- **Errores:** `409` si ya estaba completado; `400` por validación.
- **Respuesta:** `ServerConfigDTO` con `setupComplete=true`.

### `POST /api/config/scan-paths`
- **Acceso:** `ROLE_ADMIN`.
- **Body:** `{ "path": "/ruta/absoluta" }`.
- **Comportamiento:** valida que la ruta exista, sea directorio y legible. La normaliza (`toAbsolutePath().normalize()`), la añade a la lista persistente, y dispara un escaneo inicial **asíncrono** sobre esa ruta.
- **Errores:** `400` si la ruta es inválida; `409` si ya estaba en la lista.
- **Respuesta:** `ServerConfigDTO` actualizado.

### `DELETE /api/config/scan-paths?path=<ruta>`
- **Acceso:** `ROLE_ADMIN`.
- **Comportamiento:** quita la ruta de la lista (no borra las canciones ya importadas; solo deja de vigilarla).
- **Errores:** `404` si la ruta no estaba registrada.
- **Respuesta:** `ServerConfigDTO` actualizado.

### `POST /api/config/logo`
- **Acceso:** `ROLE_ADMIN`.
- **Body:** `multipart/form-data` con campo `file`.
- **Validaciones:** tamaño máx configurable vía `LOGO_MAX_FILE_SIZE` (por defecto **2 MB**); MIME en `{image/png, image/jpeg, image/svg+xml, image/webp}`. La extensión se deriva del MIME, no del nombre del cliente.
- **Comportamiento:** borra logos previos (cualquier extensión aceptada), guarda como `~/.selfpotify/assets/logo.<ext>` y actualiza `branding.logoUrl` a `/assets/logo.<ext>`.
- **Errores:** `400` sin archivo; `413` si excede el máximo; `415` si el MIME no está soportado. El `413` por exceder el límite multipart lo emite un manejador global con cuerpo JSON `{ "status": 413, "error": "Payload Too Large", "message": "El archivo excede el tamaño máximo permitido (N MB)." }`.
- **Respuesta:** `BrandingDTO`.

### `POST /api/config/scan/run`
- **Acceso:** `ROLE_ADMIN`.
- **Comportamiento:** dispara un escaneo inmediato de todas las rutas configuradas. Bloqueado con `ReentrantLock`: si ya hay un escaneo en curso (manual o periódico), responde `409`.
- **Respuesta `200 OK`:**
  ```json
  { "status": "ok", "lastRunEpochSec": 1714492800 }
  ```

### Ejemplo de `~/.selfpotify/config.yml`
```yaml
branding:
  appName: selfpotify
  logoUrl: /assets/logo.png
  colors:
    "--color-bg": "#0a0a0a"
    "--color-accent": "#b91c1c"
features:
  autoCompleteMetadata: false
scan:
  paths:
    - /Users/antondavila/Music
  intervalSeconds: 3600
  lastRunEpochSec: 1714492800
```

### Modo setup (acceso sin login en el primer arranque)
Mientras `setupComplete=false`, el backend reabre **sin login** los endpoints que necesita el wizard, gracias a `@setupGuard.inSetupMode()`: `POST /api/config/setup`, `PUT /api/config`, `POST /api/config/logo`, y `GET`/`POST /api/users`. Al completar el setup (`POST /api/config/setup`), todos vuelven a exigir `ROLE_ADMIN`.

Además, en el primer arranque y solo mientras `setupComplete=false`, la librería musical configurada en el `.env` se **auto-añade** a `scan.paths`: `/music` si se ejecuta en Docker (`SELFPOTIFY_DOCKER=true` o `/.dockerenv`) o `MUSIC_LIBRARY_PATH` en host. Por eso el escaneo inicial de `POST /api/config/setup` se dispara aunque el body no traiga rutas.

---

## 7.6 Recursos estáticos (`/assets/**`)

- **Acceso:** público (sin auth).
- El servidor mapea `/assets/**` al directorio `~/.selfpotify/assets/` (al lado del `config.yml`). Si se override `app.config.path`, los assets viven en el `assets/` hermano del fichero indicado.
- Caso de uso principal: servir el logo subido vía `POST /api/config/logo`.

---

## 8. Forma de los DTOs

### `SongDTO`
```json
{
  "id": 1,
  "title": "Stairway to Heaven",
  "duration_ms": 482000,
  "genre": "Rock",
  "bpm": 82,
  "picture_url": "https://...",
  "artistIds": [1],
  "artistNames": ["Led Zeppelin"],
  "listeners": 1234,
  "albumId": 5
}
```
> Nótese el snake_case en `duration_ms` y `picture_url` (heredado de la entidad).
>
> `listeners` es **popularidad derivada**: el número de escuchas de la canción contado por el backend sobre la tabla de eventos `user_song_listen`, no un campo almacenado en `Song`. En los listados se calcula con una única consulta agrupada (evita el N+1). Todas las respuestas que exponen canciones lo hacen como `SongDTO`, así que el campo siempre está presente (incluidos los tracks de `GET /api/artists/{id}/top-10-tracks`).

### `ArtistDTO`
```json
{
  "id": 1,
  "name": "Led Zeppelin",
  "biography": null,
  "photoUrl": "https://...",
  "albumIds": [5, 6],
  "songIds": [1, 2, 3]
}
```

### `AlbumDTO`
```json
{
  "id": 5,
  "name": "Led Zeppelin IV",
  "releaseDate": null,
  "pictureUrl": "https://...",
  "artistId": null,
  "songIds": [1, 2]
}
```
> `AlbumDTO` ya no incluye `listeners`: la popularidad del álbum se deriva por consulta (`UserSongListenRepository.countByAlbumId`) y no se expone en este DTO.

### `PlaylistDTO`
```json
{
  "id": 10,
  "name": "Mi mix",
  "description": "Para conducir",
  "isPublic": false,
  "creatorId": 3,
  "songIds": [1, 2, 3]
}
```

### `JwtResponse`
```json
{
  "token": "eyJ...",
  "type": "Bearer",
  "username": "user",
  "roles": ["ROLE_USER"]
}
```

### `BrandingDTO`
```json
{
  "appName": "selfpotify",
  "logoUrl": "/assets/logo.png",
  "colors": {
    "--color-bg": "#0a0a0a",
    "--color-accent": "#b91c1c"
  }
}
```

### `ServerConfigDTO`
```json
{
  "branding": { "appName": "selfpotify", "logoUrl": null, "colors": { "...": "..." } },
  "autoCompleteMetadata": false,
  "scanPaths": ["/Users/antondavila/Music"],
  "scanIntervalSeconds": 3600,
  "lastScanEpochSec": 0
}
```

### `ConfigUpdateRequest` (PUT `/api/config`)
```json
{
  "branding": { "appName": "myTunes", "colors": { "--color-accent": "#22c55e" } },
  "autoCompleteMetadata": true,
  "scanIntervalSeconds": 600
}
```
Todos los campos son opcionales; los nulos se dejan sin tocar.

### `ScanPathRequest` (POST `/api/config/scan-paths`)
```json
{ "path": "/ruta/absoluta/a/carpeta" }
```

### `User` (devuelto por `/api/users`)
```json
{
  "id": 1,
  "username": "user",
  "profile": null,
  "type": "USER"
}
```
(`password` está marcada como `WRITE_ONLY` y nunca aparece en respuestas.)

---

## 9. Endpoints de prueba (`/api/test`)

| Método | Path | Acceso | Respuesta |
|---|---|---|---|
| GET | `/api/test/public` | público | `"Public Content."` |
| GET | `/api/test/user` | autenticado | `"User Content."` |
| GET | `/api/test/admin` | `ROLE_ADMIN` | `"Admin Board."` |

Útiles para verificar manualmente la cadena de autenticación y autorización.

---

## 10. Configuración relevante (`application.properties`)

```properties
spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE
spring.h2.console.enabled=true
spring.h2.console.path=/h2-console
spring.jpa.hibernate.ddl-auto=create-drop

# Clave para registrar admins vía /api/auth/signup-admin
app.admin.signup-key=changeme-admin-key

# Límites de upload (logo) — controlado por LOGO_MAX_FILE_SIZE (.env)
spring.servlet.multipart.max-file-size=${LOGO_MAX_FILE_SIZE:2MB}
spring.servlet.multipart.max-request-size=${LOGO_MAX_FILE_SIZE:2MB}
app.logo.max-file-size=${LOGO_MAX_FILE_SIZE:2MB}

# Override opcional de la ruta del YAML de configuración
# app.config.path=~/.selfpotify/config.yml
```

**Consola H2:** `http://localhost:8080/h2-console` (JDBC URL `jdbc:h2:mem:testdb`, user `sa`, sin contraseña).

**Configuración del servidor (no en BD):** `~/.selfpotify/config.yml` se crea en el primer arranque. Si existe `app.music.import-folder` apuntando a un directorio válido, se siembra como primera ruta de escaneo.

---

## 11. Endpoints públicos / sin autenticación

Definidos en `WebSecurityConfig`:

- `OPTIONS /**` (preflight CORS)
- `/api/auth/**`
- `/api/test/public`
- `/api/config/public`
- `/assets/**`
- `/h2-console/**`
- `/error`

Cualquier otro endpoint requiere JWT válido.
