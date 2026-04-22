# Documentación de la API de Selfpotify

Esta API utiliza autenticación basada en JWT. El token debe enviarse en el encabezado `Authorization` como `Bearer <token>`.

## 1. Autenticación y Registro

### Login
*   **Endpoint:** `POST /api/auth/login`
*   **Acceso:** Público
*   **Cuerpo de la petición:**
    ```json
    {
      "username": "usuario",
      "password": "password"
    }
    ```
*   **Respuesta (200 OK):**
    ```json
    {
      "token": "eyJhbG...",
      "username": "usuario",
      "roles": ["ROLE_USER"]
    }
    ```

### Registro Público
*   **Endpoint:** `POST /api/auth/signup`
*   **Acceso:** Público
*   **Descripción:** Crea un nuevo usuario con el rol `ROLE_USER`.
*   **Cuerpo de la petición:**
    ```json
    {
      "username": "nuevo_usuario",
      "password": "password"
    }
    ```
*   **Respuesta (200 OK):** `"User registered successfully!"`

---

## 2. Gestión de Música (Canciones, Álbumes, Artistas)

### Canciones
| Método | Endpoint | Acceso | Descripción |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/songs` | USER, ADMIN | Lista todas las canciones (formato DTO) |
| `GET` | `/api/songs/{id}` | USER, ADMIN | Obtiene una canción por ID |
| `POST` | `/api/songs` | ADMIN | Crea una nueva canción |
| `PUT` | `/api/songs/{id}` | ADMIN | Actualiza una canción existente |
| `DELETE` | `/api/songs/{id}` | ADMIN | Elimina una canción |

**Estructura de SongDTO (Respuesta):**
```json
{
  "id": 1,
  "title": "Song Title",
  "duration_ms": 180000,
  "genre": "Pop",
  "bpm": 120,
  "picture_url": "http://...",
  "artistIds": [1, 2],
  "albumId": 5
}
```

### Álbumes
| Método | Endpoint | Acceso | Descripción |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/albums` | USER, ADMIN | Lista todos los álbumes |
| `GET` | `/api/albums/{id}` | USER, ADMIN | Obtiene un álbum por ID |
| `POST` | `/api/albums` | ADMIN | Crea un nuevo álbum |
| `PUT` | `/api/albums/{id}` | ADMIN | Actualiza un álbum |
| `DELETE` | `/api/albums/{id}` | ADMIN | Elimina un álbum |

### Artistas
| Método | Endpoint | Acceso | Descripción |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/artists` | USER, ADMIN | Lista todos los artistas |
| `GET` | `/api/artists/{id}` | USER, ADMIN | Obtiene un artista por ID |
| `POST` | `/api/artists` | ADMIN | Crea un nuevo artista |
| `PUT` | `/api/artists/{id}` | ADMIN | Actualiza un artista |
| `DELETE` | `/api/artists/{id}` | ADMIN | Elimina un artista |

---

## 3. Playlists

| Método | Endpoint | Acceso | Descripción |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/playlists` | USER, ADMIN | Lista todas las playlists |
| `GET` | `/api/playlists/{id}` | USER, ADMIN | Obtiene una playlist por ID |
| `POST` | `/api/playlists` | USER, ADMIN | Crea una nueva playlist |
| `PUT` | `/api/playlists/{id}` | ADMIN | Actualiza una playlist |
| `DELETE` | `/api/playlists/{id}` | ADMIN | Elimina una playlist |

---

## 4. Gestión de Usuarios (Administración)

Todos estos endpoints requieren el rol `ROLE_ADMIN`.

### Listar Usuarios
*   **Endpoint:** `GET /api/users`
*   **Acceso:** ADMIN

### Crear Usuario/Admin
*   **Endpoint:** `POST /api/users`
*   **Acceso:** ADMIN
*   **Cuerpo de la petición:**
    ```json
    {
      "username": "admin_creado",
      "password": "password",
      "isAdmin": true
    }
    ```
*   **Respuesta (200 OK):** `"User created successfully by admin!"`

### Actualizar/Eliminar
| Método | Endpoint | Descripción |
| :--- | :--- | :--- |
| `PUT` | `/api/users/{id}` | Actualiza datos de usuario (incluyendo password cifrada) |
| `DELETE` | `/api/users/{id}` | Elimina un usuario |
