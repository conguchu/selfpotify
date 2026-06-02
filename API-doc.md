# Documentación de la API de Selfpotify

API REST de Spring Boot 4.0.5 con autenticación JWT. El servidor escucha por defecto en `http://localhost:8080`.

**Persistencia:**
- Datos de música, usuarios y playlists → H2 en memoria (`create-drop` en cada arranque, no sobrevive a reinicios).
- Configuración del servidor (branding, rutas a escanear, flags) → fichero YAML externo en `~/.selfpotify/config.yml` (sobrevive a reinicios). Override vía `app.config.path`.

> Estado: este documento refleja el código fuente real (controllers + WebSecurityConfig + DTOs). No incluye comportamientos no implementados.

---

## 0. Convenciones

- **Base URL local:** `http://localhost:8080`
- **Auth:** se envía como `Authorization: Bearer <jwt>`. El endpoint de streaming (`GET /api/listen/{id}`) usa un **stream token** de corta vida (ver §6) en lugar del JWT, para no exponerlo en query params.
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

### `GET /api/songs/random` — Canciones aleatorias
- **Acceso:** `ROLE_USER` o `ROLE_ADMIN`.
- **Query params:** `count` (entero, opcional, por defecto `10`, máximo `50`).
- **Comportamiento:** devuelve hasta `count` canciones disponibles elegidas completamente al azar (sin semilla por usuario ni por fecha). Cada llamada puede devolver un conjunto distinto. Usado por el cliente para el scroll infinito de descubrimientos diarios.
- **Respuesta `200 OK`:** `List<SongDTO>` (puede estar vacía si no hay canciones disponibles).

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
- **Body:** entidad `Song` con los campos a actualizar (`title`, `duration_ms`, `genre`, `bpm`, `picture_url`). La ruta física (`songPath`) se **conserva** si el body no la incluye o llega vacía: la edición de metadatos desde el panel nunca borra la ruta del audio (`SongDTO` tampoco expone `songPath`).
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
- **Comportamiento:** recorre recursivamente la carpeta, extrae metadatos ID3 de los `.mp3` y `.wav` (`SongService.loadFolder`), persiste cada canción y devuelve la lista importada como `List<SongDTO>`. El álbum se resuelve/crea desde la etiqueta `ALBUM`. Tras persistir, completa de forma idempotente (solo si faltan) el género (Last.fm) y las **carátulas** (`CoverApiService`): `Song.picture_url`, `Album.picture_url` y la foto del artista (`ArtistDTO.photoUrl`) se rellenan con la carátula **embebida** del fichero —volcada a `/assets/covers/<sha256>.<ext>` y servida por `/assets/**`— o, si no hay, con fuentes **keyless** (Cover Art Archive vía MusicBrainz → iTunes → Deezer; foto de artista vía Deezer); si no se encuentra nada, el campo queda `null`. Configurable con `COVER_ART_*` (desactivable con `COVER_ART_ENABLED=false`).
- **Respuesta `200 OK`:** lista de las canciones recién persistidas.
- **Errores:** `400 Bad Request` si la ruta está vacía / no existe / no es directorio / no es legible; `500 Internal Server Error` si falla la lectura.
- **Nota:** importación manual one-shot. La ruta NO queda registrada para re-escaneo. Para escaneo persistente con re-escaneo periódico usar `POST /api/config/scan-paths` (§7.5).

### `POST /api/songs/upload` — Subir audios a staging (fase 1)
- **Acceso:** `ROLE_ADMIN`.
- **Content-Type:** `multipart/form-data`. **Campos:** `files` (uno o varios `.mp3`/`.wav`).
- **Comportamiento:** guarda los audios en una carpeta de staging **no escaneada** (`<dataDir>/selfpotify_staging/<token>`) y devuelve `List<SongDraftDTO>` con los metadatos extraídos (título, artista, género, BPM, duración) y la carátula embebida ya volcada a `/assets/covers`. Antes de devolver el borrador lo **enriquece con las mismas APIs externas que el escaneo**, para que el admin vea los datos ya completos en la pantalla de edición previa: **nombre canónico del artista** (Last.fm `artist.getInfo`, autocorrección), **género** si falta (Last.fm a partir de artista + título) y **carátula** si el audio no traía embebida (fuentes keyless: Cover Art Archive → iTunes → Deezer). Cada consulta es tolerante a fallos: si una fuente no responde, el campo queda como estaba. **No persiste** ninguna canción todavía: el panel los revisa/ajusta y confirma con `/commit`.
- **`SongDraftDTO`:** `{ stagingToken, fileName, title, artistName, suggestedArtistId, genre, bpm, duration_ms, picture_url }`. `artistName` es ya el nombre canónico si Last.fm lo resolvió; `suggestedArtistId` es el id de un artista existente cuyo nombre coincide con el extraído (o null).
- **Límites:** `SONG_UPLOAD_MAX_FILE_SIZE` por archivo (def. 50MB), `SONG_UPLOAD_MAX_REQUEST_SIZE` por petición (def. 500MB).
- **Errores:** `415` (formato no admitido), `413` (excede tamaño).

### `POST /api/songs/commit` — Confirmar subida (fase 2)
- **Acceso:** `ROLE_ADMIN`. **Body** (`SongCommitRequest`):
  ```json
  {
    "targetPath": "/music/extra",
    "songs": [
      { "stagingToken": "uuid", "fileName": "a.mp3", "title": "...",
        "artistId": 5, "newArtistName": null,
        "genre": "Rock", "bpm": 120, "duration_ms": 200000, "picture_url": "/assets/covers/..." }
    ]
  }
  ```
- **Comportamiento:** mueve cada audio de staging a `<targetPath>/selfpotify_added` y persiste la canción con los metadatos finales. El artista se resuelve por `artistId`; si es null y hay `newArtistName`, se resuelve/crea **por MBID vía Last.fm** (misma deduplicación que el escaneo: empareja variantes del mismo artista y rellena el MBID). Tras persistir, completa de forma **idempotente** (solo si faltan) el género (Last.fm) y las **carátulas** (`CoverApiService`): `Song.picture_url`, `Album.picture_url` y la **foto del artista** (`ArtistDTO.photoUrl`, Deezer) — esta última no se ve en la pantalla de edición pero deja la canción subida indistinguible de una escaneada del disco. **No** se registra la carpeta como ruta de escaneo (el commit ya persiste cada canción con su `songPath`). Devuelve `List<SongDTO>`.
  - **Destino (`targetPath`):** **obligatorio**. Debe ser una de las rutas de escaneo configuradas (`scan.paths`) y escribible. En Docker el volumen de música se monta en lectura/escritura (`/music`, sin `:ro`), así que `/music` es un destino válido.
- **Errores:** `400` (sin canciones, `targetPath` ausente / no configurada / no escribible, o staging expirado).

### `POST /api/songs/cover` — Subir carátula
- **Acceso:** `ROLE_ADMIN`. **Content-Type:** `multipart/form-data`, campo `file` (PNG/JPEG/WebP).
- **Comportamiento:** guarda la imagen en el mismo almacén que las carátulas normales (`/assets/covers/<sha256>.<ext>`, idempotente) y devuelve `{ "url": "/assets/covers/..." }`. La usa el panel al subir y al editar canciones.
- **Errores:** `400` (sin archivo), `415` (formato no soportado).

### `PUT /api/songs/{id}/artists` — Reasignar artistas
- **Acceso:** `ROLE_ADMIN`. **Body:** `{ "artistIds": [5, 8] }`.
- **Comportamiento:** sustituye los artistas de la canción por los ids dados (lista vacía = sin artista). Devuelve el `SongDTO` actualizado o `404`.

---

## 3. Artistas (`/api/artists`)

| Método | Path | Acceso | Body | Respuesta |
|---|---|---|---|---|
| GET | `/api/artists` | USER/ADMIN | — | `List<ArtistDTO>` |
| GET | `/api/artists/{id}` | USER/ADMIN | — | `ArtistDTO` o `404` |
| GET | `/api/artists/{id}/top-10-tracks` | USER/ADMIN | — | `Top10ArtistTracksDTO` o `404` |
| POST | `/api/artists` | ADMIN | `Artist` | `ArtistDTO` |
| PUT | `/api/artists/{id}` | ADMIN | `ArtistUpdateRequest` | `ArtistDTO` o `404` |
| POST | `/api/artists/{id}/fetch-photo` | ADMIN | — | `{ "url": string }` o `404` |
| POST | `/api/artists/{id}/split` | ADMIN | `SplitArtistRequest` | `List<ArtistDTO>` o `400`/`404` |
| POST | `/api/artists/merge` | ADMIN | `MergeArtistsRequest` | `ArtistDTO` o `400`/`404` |
| DELETE | `/api/artists/{id}` | ADMIN | — | `204` o `404` |

Nota: `ArtistDTO.biography` está declarado pero el `convertToDTO` actual no lo rellena; siempre será `null`.

Nota: `PUT /api/artists/{id}` (edición manual desde el panel) recibe `ArtistUpdateRequest` = `{ "name": string, "photoUrl": string }` y solo actualiza nombre y foto. La foto suele subirse antes con `POST /api/songs/cover` (devuelve `{ "url": "/assets/covers/..." }`) y su URL se manda en `photoUrl`; también admite una URL externa. El `mbid` no se edita por aquí (es identidad resuelta automáticamente).

Nota: `POST /api/artists/{id}/fetch-photo` busca una foto del artista (Deezer, vía `CoverApiService`) por su nombre y devuelve `{ "url": "..." }` **sin persistirla**: el panel la fija en el formulario de edición y se guarda con el `PUT`. Respeta `app.cover-art.enabled`; si no se encuentra (o la resolución online está desactivada) responde `404`.

Nota: `POST /api/artists/{id}/split` separa un artista mal etiquetado en varios reales. Body `SplitArtistRequest` = `{ "names": List<String> }` (mínimo dos). Cada nombre se resuelve con `ArtistResolver` (Last.fm → nombre canónico + MBID, reutilizando un artista existente si ya lo había); **todas** las canciones y álbumes del original se atribuyen a **todos** los resultantes y el original se borra. A los resultantes que aún no tengan foto se les rellena (Deezer) reutilizando `CoverApiService`, **respetando `app.cover-art.enabled`** (si la resolución online está desactivada, no se consulta). Devuelve los artistas resultantes. `400` si llegan menos de dos nombres o no se resuelven al menos dos artistas distintos del original; `404` si el artista no existe.

Nota: `POST /api/artists/merge` une varios artistas duplicados en uno. Body `MergeArtistsRequest` = `{ "ids": List<Long>, "survivorId": Long, "name": String }`. El superviviente (`survivorId`, que debe estar en `ids`) conserva su id y su MBID, absorbe las canciones y álbumes del resto (sin duplicar atribuciones) y el resto se borra; si `name` no viene vacío, se renombra al superviviente. Devuelve el `ArtistDTO` del superviviente. `400` si hay menos de dos ids o el superviviente no está entre ellos; `404` si algún artista no existe.

Nota: `DELETE /api/artists/{id}` desliga primero al artista de `song_artist`, `album_artist` y de los feeds que lo recomiendan, y luego borra la fila. Las canciones y álbumes **no** se borran: solo dejan de atribuirse a ese artista.

Nota: `GET /api/artists/{id}/top-10-tracks` devuelve `{ "tracks": List<SongDTO> }` con las 10 canciones del artista más escuchadas, **derivadas** de `user_song_listen`. Cada track es un `SongDTO` con su campo `listeners` (popularidad derivada) resuelto mediante una única consulta agrupada, igual que el resto de listados.

---

## 4. Álbumes (`/api/albums`)

| Método | Path | Acceso | Body | Respuesta |
|---|---|---|---|---|
| GET | `/api/albums` | USER/ADMIN | — | `List<AlbumDTO>` |
| GET | `/api/albums/{id}` | USER/ADMIN | — | `AlbumDTO` o `404` |
| POST | `/api/albums` | ADMIN | `Album` | `AlbumDTO` |
| PUT | `/api/albums/{id}` | ADMIN | `AlbumUpdateRequest` | `AlbumDTO` o `404` |
| DELETE | `/api/albums/{id}` | ADMIN | — | `204` o `404` |

Nota: `AlbumDTO.releaseDate` y `AlbumDTO.artistId` están declarados pero el `convertToDTO` no los rellena; siempre serán `null`.

Nota: `PUT /api/albums/{id}` (edición manual desde el panel) recibe `AlbumUpdateRequest` = `{ "name": string, "photoUrl": string }` y solo actualiza nombre y portada (no toca las asociaciones de artistas/canciones, que un body parcial sobre la entidad `Album` habría puesto a `null`). La portada suele subirse antes con `POST /api/songs/cover`; también admite una URL externa.

---

## 5. Playlists (`/api/playlists`)

Todos los endpoints requieren `ROLE_USER` o `ROLE_ADMIN`.

### `GET /api/playlists/my`
- Lista todas las playlists del usuario autenticado (privadas y públicas).
- **Respuesta:** `List<PlaylistDTO>`.

### `GET /api/playlists/user/{userId}`
- Playlists **públicas** de otro usuario.
- **Respuesta:** `200 OK List<PlaylistDTO>` o `404` si el usuario no existe.

### `GET /api/playlists/my`
> (definido arriba)

### `GET /api/playlists/shared`
- Playlists en las que el usuario autenticado figura como **colaborador** (no como creador).
- **Respuesta:** `200 OK List<PlaylistDTO>`.

### `GET /api/playlists/{id}`
- **Reglas de acceso:**
  - Si la playlist es pública → cualquier autenticado puede verla.
  - Si es privada → solo el **creador** o un **colaborador**.
- En este endpoint la respuesta incluye `collaboratorIds` poblado.
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
- Solo el **creador** puede modificar los metadatos (nombre, descripción, visibilidad, carátula). Cuerpo igual al de creación. Los colaboradores **no** pueden usar este endpoint.
- **Respuesta:** `200 OK PlaylistDTO`, `403`, o `404`.

### `DELETE /api/playlists/{id}` — Borrar
- El **creador** o un **admin** pueden borrar. Al borrar se eliminan también los colaboradores y los magic links pendientes de la playlist.
- **Respuesta:** `204 No Content`, `403`, o `404`.

### Colaboración (playlists compartidas)

Una playlist puede compartirse con otros usuarios mediante un **magic link de un solo uso**. El creador genera el enlace; quien lo canjea se convierte en **colaborador**. Creador y colaboradores pueden añadir/quitar canciones; solo el creador edita metadatos o borra (ver arriba).

### `POST /api/playlists/{id}/share` — Generar magic link
- **Acceso:** solo el **creador** de la playlist.
- **Comportamiento:** crea un token aleatorio de un solo uso (sin caducidad temporal; se consume al canjearse). El creador puede generar varios.
- **Errores:** `403` si no es el creador; `404` si la playlist no existe.
- **Respuesta:** `200 OK ShareLinkResponse` (ver §8).

### `POST /api/playlists/share/{token}` — Canjear magic link
- **Acceso:** cualquier usuario autenticado.
- **Comportamiento:** añade al usuario autenticado como colaborador y **consume** (elimina) el token. Idempotente respecto al colaborador (si ya lo era, no se duplica), pero el token siempre se gasta.
- **Errores:** `404` si el token no existe o ya fue usado; `409 Conflict` si quien canjea es el propio creador.
- **Respuesta:** `200 OK PlaylistDTO` (con `collaboratorIds`).

### `POST /api/playlists/{id}/songs/{songId}` — Añadir canción
- **Acceso:** **creador o colaborador**.
- **Comportamiento:** añade la canción si no estaba ya y recalcula la duración total. Idempotente.
- **Errores:** `403` si no puede editar; `404` si la playlist o la canción no existen.
- **Respuesta:** `200 OK PlaylistDTO`.

### `DELETE /api/playlists/{id}/songs/{songId}` — Quitar canción
- **Acceso:** **creador o colaborador**.
- **Comportamiento:** quita la canción y recalcula la duración total.
- **Errores:** `403` si no puede editar; `404` si la playlist o la canción no existen.
- **Respuesta:** `200 OK PlaylistDTO`.

### `GET /api/playlists/{id}/collaborators` — Listar colaboradores
- **Acceso:** quien pueda ver la playlist (**creador, colaborador** o cualquiera si es pública).
- **Respuesta:** `200 OK List<UserSummaryDTO>`, `403` o `404`.

### `DELETE /api/playlists/{id}/collaborators/{userId}` — Quitar colaborador
- **Acceso:** solo el **creador**.
- **Comportamiento:** elimina al usuario de la lista de colaboradores. Idempotente (no falla si ya no lo era).
- **Respuesta:** `204 No Content`, `403` o `404`.

---

## 6. Streaming de audio (`/api/listen`)

El flujo de streaming usa **stream tokens** de corta vida para evitar exponer el JWT de sesión en la URL del elemento `<audio>`. El cliente primero pide un token al backend (enviando el JWT en header como siempre), y luego pasa ese token como query param `?st=` en la URL de audio. El stream token no es un JWT, no sirve para ningún otro endpoint, y expira en 4 horas.

### `POST /api/listen/token`

- **Acceso:** `ROLE_USER` o `ROLE_ADMIN` (JWT en `Authorization: Bearer`).
- **Request body:** ninguno.
- **Respuesta `200 OK`:**
  ```json
  { "token": "550e8400-e29b-41d4-a716-446655440000" }
  ```
- **Comportamiento:** emite un stream token UUID ligado al usuario autenticado con TTL de 4 horas. El token es reutilizable dentro de su TTL (necesario para soportar múltiples peticiones HTTP Range del mismo `<audio>`).

### `GET /api/listen/{songId}?st={streamToken}`

- **Acceso:** requiere `?st=<streamToken>` válido emitido por `POST /api/listen/token`. No requiere cabecera `Authorization`.
- **Cabeceras de petición:** opcional `Range: bytes=start-end` para streaming progresivo (HTTP Range, RFC 7233).
- **Respuesta `200 OK`** (sin Range): el archivo completo, con cabeceras
  - `Content-Type: audio/mpeg | audio/wav | audio/ogg | audio/flac | audio/aac | application/octet-stream`
  - `Content-Length: <bytes>`
  - `Accept-Ranges: bytes`
  - `Cache-Control: no-store`
- **Respuesta `206 Partial Content`** (con Range válido): chunk solicitado, cabecera `Content-Range: bytes start-end/total`.
- **Respuesta `401 Unauthorized`** si el stream token es inválido o ha expirado.
- **Respuesta `416 Requested Range Not Satisfiable`** si el Range es inválido.
- **Respuesta `404 Not Found`** si la canción no existe o el archivo (`Song.songPath`) no es accesible.

**Efectos secundarios:** solo la **petición inicial** de la reproducción (sin cabecera `Range`, o con `Range` desde el byte 0) registra una fila en la tabla de eventos `user_song_listen` (`UserSongListenService.recordListen`) y apila el género de la canción en el feed del usuario (`registerGenreListen`). Las peticiones de rango posteriores (los *seeks* dentro de la canción, con `start > 0`) **no** tienen efectos secundarios: no insertan filas ni alteran el feed. **No** se incrementa ningún contador numérico: la popularidad de canciones, álbumes, artistas y géneros se deriva por consulta a partir de esos eventos (ver §6.5 y la nota de `SongDTO` en §8). La tabla de eventos se acota a 1000 registros por usuario con descarte FIFO.

---

## 6.5 Feed del home (`/api/feed`)

Acceso `ROLE_USER` o `ROLE_ADMIN`. El feed se calcula a partir del historial de escuchas del usuario (`user_song_listen`).

### `GET /api/feed`

- **Comportamiento:** regenera y devuelve el feed **personalizado** del usuario autenticado en cada acceso al home. `UserFeedService.recommendArtistsForUser` toma los artistas que más ha escuchado el propio usuario (derivado de `user_song_listen`) y, si no llegan a 10, completa con la popularidad global (artistas más escuchados en toda la instalación) sin repetir. Un usuario sin historial recibe directamente la popularidad global (cold-start). La lista resultante (máx. 10, sin duplicados) sobrescribe los artistas recomendados del feed; la pila de géneros recientes no se altera.
- **Respuesta `200 OK`:** `List<ArtistDTO>` (ver §8) con los artistas recomendados.

### `GET /api/feed/genres`

- **Comportamiento:** devuelve los géneros escuchados más recientemente por el usuario autenticado, del más reciente al más antiguo (pila acotada a 20, índice 0 = más reciente).
- **Respuesta `200 OK`:** `List<String>` con como máximo los 10 géneros más recientes.

### `GET /api/feed/daily-discoveries`

- **Comportamiento:** devuelve los **descubrimientos diarios** del usuario autenticado: 9 canciones compuestas por 3 aleatorias del catálogo, 3 no escuchadas de su **último género** escuchado (cabeza de la pila; cae al siguiente género de la pila si no hay suficientes) y 3 de un **género que el usuario no escucha** (presente en el catálogo pero ausente de su historial; cae al género más antiguo de la pila si no hay candidato). La lista es **estable durante el día** (aleatoriedad sembrada con `userId + fecha`, cambia a medianoche) y se devuelve **mezclada**. Si el catálogo no da para 9 sin repetir, se completa con aleatorias hasta donde alcance. No persiste nada: se recalcula de forma determinista en cada petición (`DailyDiscoveryService`).
- **Respuesta `200 OK`:** `List<SongDTO>` (ver §8) con hasta 9 canciones; `listeners` viene derivado de `user_song_listen`.

---

## 6.6 Búsqueda global (`/api/search`)

Barra de búsqueda transversal sobre el catálogo (canciones, artistas, álbumes, géneros) y la comunidad (playlists públicas, usuarios). Insensible a mayúsculas y diacríticos: la consulta y los textos del catálogo se normalizan con `Normalizer.Form.NFD` + strip de `\p{InCombiningDiacriticalMarks}+` + lowercase `Locale.ROOT` antes de comparar. Tokeniza la consulta por espacios y exige que **todas** las palabras estén presentes en el haystack del documento (estilo barra de YouTube/Spotify: `stairway heaven` empareja con `Stairway to Heaven`).

### `GET /api/search`

- **Acceso:** `ROLE_USER` o `ROLE_ADMIN`.
- **Query params:**

| Parámetro | Default | Descripción |
|---|---|---|
| `q` | `""` | Consulta libre. Si está vacía, se devuelve la forma de respuesta con todas las categorías a 0. |
| `type` | `all` | Modo. `all` rellena todas las categorías recortadas a 5 cada una (vista previa para dropdown). `songs`, `artists`, `albums`, `playlists`, `users`, `genres` rellenan solo esa categoría, paginada. |
| `page` | `0` | Índice 0-based de la página (solo aplica al modo específico). |
| `size` | `20` | Tamaño de página, acotado a `100`. En modo `all` actúa como tope superior y se recorta a `5` por categoría. |

- **Campos buscables por categoría:**

| Categoría | Haystack | Tiebreaker tras el score |
|---|---|---|
| `songs` | título + nombres de artistas + nombre del álbum + género | escuchas desc, luego título asc |
| `artists` | nombre del artista | nº de canciones desc, luego nombre asc |
| `albums` | nombre del álbum + nombres de artistas | nº de canciones desc, luego nombre asc |
| `playlists` | nombre + descripción + username del creador | nº de canciones desc, luego nombre asc |
| `users` | username + nombre del perfil | username asc (score = mejor match entre username y displayName) |
| `genres` | nombre del género (distintos del catálogo) | nº de canciones desc, luego nombre asc |

- **Visibilidad de playlists:** se incluyen las **públicas** y las **propias** (públicas o privadas) del usuario autenticado. Las privadas de otros usuarios nunca aparecen.

- **Scoring:** menor es mejor. `0` = el campo principal es exactamente la consulta; `1` = empieza por la consulta; `2` = alguna palabra empieza por el primer token; `3` = subcadena (caso por defecto, ya garantizado por el filtro).

- **Errores:** `400 Bad Request` si `type` no está en el conjunto válido.

- **Respuesta `200 OK SearchResponseDTO`:** misma forma en ambos modos; las categorías no usadas vienen como `null` y se omiten del JSON.
  ```json
  {
    "query": "rosalia",
    "type": "all",
    "page": 0,
    "size": 5,
    "songs":     { "content": [/* SongDTO */],        "totalElements": 12, "totalPages": 3 },
    "artists":   { "content": [/* ArtistDTO */],      "totalElements": 1,  "totalPages": 1 },
    "albums":    { "content": [/* AlbumDTO */],       "totalElements": 1,  "totalPages": 1 },
    "playlists": { "content": [/* PlaylistDTO */],    "totalElements": 0,  "totalPages": 1 },
    "users":     { "content": [/* UserSummaryDTO */], "totalElements": 0,  "totalPages": 1 },
    "genres":    { "content": [/* GenreResultDTO */], "totalElements": 0,  "totalPages": 1 }
  }
  ```

---

## 6.7 Perfil del usuario (`/api/me`, `/api/users/{id}/public`)

Endpoints para que cada usuario gestione su propio perfil (nombre visible + foto) y para consultar la vista pública mínima de cualquier otro. Todos requieren `ROLE_USER` o `ROLE_ADMIN`.

> Los endpoints administrativos (alta/baja, cambio de rol, listado completo) siguen viviendo en `/api/users` (§7) y son `ROLE_ADMIN`-only.

### `GET /api/me`

- **Comportamiento:** devuelve la vista pública del usuario autenticado, resuelto desde el `SecurityContext`.
- **Respuesta `200 OK UserSummaryDTO`** (ver §8).

### `PUT /api/me/profile`

- **Body:** `ProfileUpdateRequest` — único campo es `name`. Si llega `null`, vacío o solo espacios, el nombre se borra y la UI cae al username.
  ```json
  { "name": "Anton Davila" }
  ```
- **Comportamiento:** si el usuario no tiene `Profile` aún, se crea bajo demanda (cascade ALL en `User.profile`). El username no se modifica desde aquí en ningún caso.
- **Respuesta `200 OK UserSummaryDTO`** con el perfil actualizado.

### `POST /api/me/profile/picture`

- **Body:** `multipart/form-data` con campo `file`.
- **Validaciones:** MIME en `{image/jpeg, image/png, image/webp}`; tamaño máximo **5 MB**.
- **Comportamiento:** recorta la imagen al cuadrado por el centro con `ImageIO`, la guarda como JPEG en `<assets>/avatars/<sha256>.jpg` y persiste `Profile.pictureUrl = "/assets/avatars/<sha256>.jpg"`. Mismo patrón que `POST /api/playlists/{id}/cover`. La operación es **idempotente**: subir dos veces la misma imagen no crea ficheros duplicados.
- **Errores:** `400` sin archivo o ilegible; `413` si excede 5 MB; `415` si el MIME no está soportado.
- **Respuesta `200 OK UserSummaryDTO`** con la nueva `avatarUrl`.

### `DELETE /api/me/profile/picture`

- **Comportamiento:** pone `Profile.pictureUrl = null`. **No** borra el fichero físico de `assets/avatars/` (podría estar referenciado por otra cuenta que subió la misma imagen).
- **Respuesta `200 OK UserSummaryDTO`** con `avatarUrl = null`.

### `GET /api/users/{id}/public`

- **Comportamiento:** vista pública mínima de cualquier usuario por id. Misma forma que la que devuelve `/api/search?type=users`. Se usa para enlazar al perfil desde la búsqueda. Las playlists públicas se siguen consultando vía `GET /api/playlists/user/{userId}` (§5).
- **Respuesta:** `200 OK UserSummaryDTO` o `404 Not Found` si no existe.

---

## 6.8 Seguimiento entre usuarios (follow)

Cada usuario puede seguir y ser seguido por otros (grafo dirigido). El follower de cada arista se infiere siempre del `SecurityContext` del usuario en sesión, nunca del path. Todos los endpoints requieren `ROLE_USER` o `ROLE_ADMIN`.

### `POST /api/users/{id}/follow`

- **Comportamiento:** el usuario autenticado pasa a seguir a `{id}`. Idempotente: si ya lo seguías, no inserta nada pero responde 200.
- **Errores:** `400 Bad Request` si `{id}` es el propio usuario autenticado; `404 Not Found` si `{id}` no existe.
- **Respuesta:** `200 OK UserSummaryDTO` del usuario `{id}` con sus counts recalculados y `isFollowedByMe = true`.

### `DELETE /api/users/{id}/follow`

- **Comportamiento:** el usuario autenticado deja de seguir a `{id}`. Idempotente: si no lo seguías, no borra nada pero responde 200.
- **Errores:** `404 Not Found` si `{id}` no existe.
- **Respuesta:** `200 OK UserSummaryDTO` del usuario `{id}` con sus counts recalculados y `isFollowedByMe = false`.

### `GET /api/users/{id}/followers`

- **Comportamiento:** quién sigue a `{id}`, **más recientes primero** (`createdAt desc`). Para cada fila se rellenan en batch los counts y `isFollowedByMe` respecto al usuario en sesión, evitando N+1.
- **Respuesta:** `200 OK List<UserSummaryDTO>` o `404 Not Found` si `{id}` no existe.

### `GET /api/users/{id}/following`

- **Comportamiento:** a quién sigue `{id}`, más recientes primero. Misma forma y enriquecido que `/followers`.
- **Respuesta:** `200 OK List<UserSummaryDTO>` o `404 Not Found` si `{id}` no existe.

---

## 7. Administración de usuarios (`/api/users`)

Acceso exclusivo `ROLE_ADMIN`.

| Método | Path | Body | Respuesta |
|---|---|---|---|
| GET | `/api/users` | — | `List<User>` (campo `type` = `USER`/`ADMIN`, getter calculado desde la subclase; `password` no se serializa) |
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
- **Comportamiento:** dispara un escaneo inmediato de todas las rutas configuradas. Bloqueado con `ReentrantLock`: si ya hay un escaneo en curso (manual o periódico), responde `409`. Cada escaneo (este, el periódico y el de `scan-paths`) enriquece género y carátulas igual que `POST /api/songs/import`.
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
  "songIds": [1, 2, 3],
  "collaboratorIds": [7, 9]
}
```
> `collaboratorIds` se rellena en las respuestas de "mi contexto": la playlist individual (`GET /api/playlists/{id}`, canje de magic link, add/remove de canciones) y los listados propios `/my` y `/shared` (acotados al usuario). En los listados ajenos (`/user/{userId}`) y en la búsqueda viaja `null` para evitar un N+1.

### `ShareLinkResponse`
```json
{
  "token": "p3qR8s...Base64UrlSafe",
  "shareUrl": "/api/playlists/share/p3qR8s...Base64UrlSafe"
}
```
> Respuesta de `POST /api/playlists/{id}/share`. `token` es el magic link de un solo uso; `shareUrl` es la ruta relativa para canjearlo.

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
  "autoCompleteCoverArt": false,
  "setupComplete": true,
  "scanPaths": ["/Users/antondavila/Music"],
  "scanIntervalSeconds": 3600,
  "lastScanEpochSec": 0,
  "runningInDocker": false
}
```
> `runningInDocker` indica si el backend corre en contenedor (los audios subidos van entonces a la carpeta de datos) y `addedSongsDir` es la carpeta `selfpotify_added` por defecto; guían la subida drag & drop del panel (§2, `POST /api/songs/upload`).

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

### `UserSummaryDTO` (devuelto por `/api/search?type=users`, perfiles y endpoints de follow)
```json
{
  "id": 1,
  "username": "anton",
  "displayName": "Anton Davila",
  "avatarUrl": "/assets/avatars/anton.jpg",
  "type": "USER",
  "followersCount": 12,
  "followingCount": 7,
  "isFollowedByMe": true
}
```
Vista pública mínima de un usuario: no incluye contraseña, feed ni perfil completo. `displayName` y `avatarUrl` provienen del `Profile` asociado y pueden venir `null` si el usuario aún no tiene perfil. Devuelta también por `GET /api/me`, `GET /api/users/{id}/public` y los endpoints `PUT/POST/DELETE /api/me/profile*` (ver §6.7).

Los campos de follow viajan siempre en el JSON con la misma forma, pero **solo los endpoints de perfil y follow** los rellenan; los resultados de búsqueda los emiten en `0` / `null` para no inducir N+1 en `SearchService`. `isFollowedByMe` es `null` cuando no hay viewer (admin listings) o cuando el viewer es el propio usuario representado por el DTO.

### `ProfileUpdateRequest` (PUT `/api/me/profile`)
```json
{ "name": "Anton Davila" }
```
Único campo editable de momento. `null` o blanco limpia el nombre y la UI cae al username.

### `GenreResultDTO` (devuelto por `/api/search?type=genres` y modo `all`)
```json
{ "name": "Rock", "songCount": 42 }
```
`songCount` es el número de canciones del catálogo que tienen ese género.

### `SearchResponseDTO` (devuelto por `/api/search`)
```json
{
  "query": "rosalia",
  "type": "all",
  "page": 0,
  "size": 5,
  "songs":     { "content": [], "totalElements": 0, "totalPages": 1 },
  "artists":   { "content": [], "totalElements": 0, "totalPages": 1 },
  "albums":    { "content": [], "totalElements": 0, "totalPages": 1 },
  "playlists": { "content": [], "totalElements": 0, "totalPages": 1 },
  "users":     { "content": [], "totalElements": 0, "totalPages": 1 },
  "genres":    { "content": [], "totalElements": 0, "totalPages": 1 }
}
```
Cada `content` lista DTOs de su categoría (`SongDTO`, `ArtistDTO`, `AlbumDTO`, `PlaylistDTO`, `UserSummaryDTO`, `GenreResultDTO`). En modo específico, las categorías no pedidas se omiten del JSON (vienen como `null`). `query` ya viene normalizada (lowercased + sin diacríticos), útil para que el cliente la pueda echar como eco sobre la URL.

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
