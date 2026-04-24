# Documentación de la API de Selfpotify

Esta API utiliza autenticación basada en JWT. El token debe enviarse en el encabezado `Authorization` como `Bearer <token>`.

## 1. Autenticación y Registro

### Login
*   **Endpoint:** `POST /api/auth/login`
*   **Acceso:** Público
*   **Cuerpo:**
    ```json
    {
      "username": "usuario",
      "password": "password"
    }
    ```
*   **Respuesta:** Token JWT y roles del usuario.

### Registro Público (Auto-registro)
*   **Endpoint:** `POST /api/auth/signup`
*   **Acceso:** Público
*   **Descripción:** Permite a cualquier persona registrarse. Por defecto, se asigna el rol `ROLE_USER`.
*   **Cuerpo:**
    ```json
    {
      "username": "nuevo_usuario",
      "password": "password"
    }
    ```

---

## 2. Música (Canciones, Álbumes, Artistas)

### Reglas de Acceso
*   **Lectura (`GET`):** Usuarios (`ROLE_USER`) y Administradores (`ROLE_ADMIN`).
*   **Escritura (`POST`, `PUT`, `DELETE`):** Solo Administradores (`ROLE_ADMIN`).

### Canciones (`/api/songs`)
*   `GET /api/songs`: Listado general.
*   `GET /api/songs/{id}`: Detalle de canción.
*   `POST /api/songs`: Crear (Requiere título, género, etc.).
*   `PUT /api/songs/{id}`: Modificar metadatos.

### Álbumes (`/api/albums`) y Artistas (`/api/artists`)
*   Siguen el mismo esquema CRUD que las canciones. Los objetos devueltos son DTOs que contienen IDs de relaciones para evitar redundancia.

---

## 3. Playlists (`/api/playlists`)

Las playlists tienen lógica de propiedad y visibilidad basada en el usuario autenticado.

### Endpoints de Consulta
*   `GET /api/playlists/my`: Lista **todas** tus playlists (públicas y privadas).
*   `GET /api/playlists/user/{userId}`: Lista las playlists **públicas** de otro usuario.
*   `GET /api/playlists/{id}`: Acceso permitido si:
    *   La playlist es pública (`isPublic: true`).
    *   Eres el creador de la playlist.

### Endpoints de Gestión
*   `POST /api/playlists`: Crea una playlist. El creador se asigna automáticamente.
*   `PUT /api/playlists/{id}`: Solo el **dueño** puede modificarla.
*   `DELETE /api/playlists/{id}`: Solo el **dueño** o un **Administrador** pueden eliminarla.

**Cuerpo (JSON):**
```json
{
  "name": "Nombre Playlist",
  "description": "Descripción opcional",
  "isPublic": true,
  "songIds": [1, 2, 3]
}
```

---

## 4. Administración de Usuarios (`/api/users`)

Acceso exclusivo para `ROLE_ADMIN`.

### Crear Usuarios o Administradores
*   **Endpoint:** `POST /api/users`
*   **Descripción:** Un administrador puede crear otros usuarios y decidir si son administradores.
*   **Cuerpo:**
    ```json
    {
      "username": "nombre",
      "password": "password",
      "isAdmin": true 
    }
    ```

### Gestión General
*   `GET /api/users`: Lista completa de usuarios del sistema.
*   `PUT /api/users/{id}`: Modificar datos de cualquier usuario (incluye cifrado de password automático).
*   `DELETE /api/users/{id}`: Eliminar usuario.
