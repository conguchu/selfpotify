# Documentación de la API de Selfpotify

API REST de Spring Boot 4.0.5 con autenticación JWT. El servidor escucha por defecto en `http://localhost:8080` y la persistencia es H2 en memoria (`create-drop` en cada arranque).

> Estado: este documento refleja el código fuente real (controllers + WebSecurityConfig + DTOs). No incluye comportamientos no implementados.

---

## 0. Convenciones

- **Base URL local:** `http://localhost:8080`
- **Auth:** se envía como `Authorization: Bearer <jwt>` salvo en el endpoint de streaming, donde también se acepta el JWT vía query param `?token=<jwt>` (necesario porque `<audio>` no permite headers personalizados — implementado en `AuthTokenFilter.java:60-65`).
- **Tipo de contenido:** `application/json` para request/response salvo el streaming, que devuelve `audio/*`.
- **Roles:** `ROLE_USER` y `ROLE_ADMIN`. El discriminador JPA `users.type` (`USER` / `ADMIN`) determina el rol — un usuario no puede cambiar de rol post-creación.
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

### `POST /api/songs/import` — Escanear carpeta del servidor *(NUEVO)*
- **Acceso:** `ROLE_ADMIN`.
- **Body:**
  ```json
  { "path": "/ruta/absoluta/a/carpeta" }
  ```
- **Comportamiento:** recorre recursivamente la carpeta, extrae metadatos ID3 de los `.mp3` y `.wav` (`SongService.loadFolder`), persiste cada canción y devuelve la lista importada como `List<SongDTO>`.
- **Respuesta `200 OK`:** lista de las canciones recién persistidas.
- **Errores:** `400 Bad Request` si la ruta está vacía / no existe / no es directorio / no es legible; `500 Internal Server Error` si falla la lectura.

---

## 3. Artistas (`/api/artists`)

| Método | Path | Acceso | Body | Respuesta |
|---|---|---|---|---|
| GET | `/api/artists` | USER/ADMIN | — | `List<ArtistDTO>` |
| GET | `/api/artists/{id}` | USER/ADMIN | — | `ArtistDTO` o `404` |
| POST | `/api/artists` | ADMIN | `Artist` | `ArtistDTO` |
| PUT | `/api/artists/{id}` | ADMIN | `Artist` | `ArtistDTO` o `404` |
| DELETE | `/api/artists/{id}` | ADMIN | — | `204` o `404` |

Nota: `ArtistDTO.biography` está declarado pero el `convertToDTO` actual no lo rellena; siempre será `null`.

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

**Efectos secundarios:** cada llamada exitosa incrementa `Song.listeners` en 1 y `Artist.listeners` en 1 por **cada** artista de la canción.

---

## 7. Administración de usuarios (`/api/users`)

Acceso exclusivo `ROLE_ADMIN`.

| Método | Path | Body | Respuesta |
|---|---|---|---|
| GET | `/api/users` | — | `List<User>` (campo `type` indica USER/ADMIN; `password` no se serializa) |
| GET | `/api/users/{id}` | — | `User` o `404` |
| POST | `/api/users` | `{ "username", "password", "isAdmin" }` | `200 OK "User created successfully by admin!"`; `400` si el username está cogido |
| PUT | `/api/users/{id}` | `User` (si trae `password` se reencripta automáticamente) | `200 OK User` o `404` |
| DELETE | `/api/users/{id}` | — | `204 No Content` o `404` |

**Limitación conocida:** cambiar el rol de un usuario existente no es posible — el discriminador JPA `users.type` no se reasigna mediante `PUT`. Para "promover" hay que borrar y volver a crear.

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
  "albumId": 5
}
```
> Nótese el snake_case en `duration_ms` y `picture_url` (heredado de la entidad).

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
```

**Consola H2:** `http://localhost:8080/h2-console` (JDBC URL `jdbc:h2:mem:testdb`, user `sa`, sin contraseña).

---

## 11. Endpoints públicos / sin autenticación

Definidos en `WebSecurityConfig`:

- `OPTIONS /**` (preflight CORS)
- `/api/auth/**`
- `/api/test/public`
- `/h2-console/**`
- `/error`

Cualquier otro endpoint requiere JWT válido.
