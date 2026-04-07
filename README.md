# Self-Potify

## Objetivos

Mi idea para mi proyecto de fin de grado es crear un "clon" alternativo de código abierto de Spotify. Funcionará con tecnologías de streaming, permitiendo escuchar la música con baja latencia sin tener que esperar a que descargue ningún archivo igual que en el original, y tendrá sistemas de playlist creadas automáticamente como "Recomendaciones Diarias" o "Selección del artista".

El proyecto incluiría:

- **Servidor Self-Potify** — Contiene toda la librería musical organizada en carpetas, además de la BBDD que almacenará tanto los usuarios como sus likes / playlists.
- **Cliente web** — Para escuchar la música del servidor en streaming desde un ordenador. Esto será a través de un servidor web en el que puedes acceder solamente con tu login de usuario.
- **Cliente móvil / televisión** — Aplicación para Android con las mismas funciones que la web pero mayor rendimiento. Al entrar por primera vez, se tendrá que configurar para poner los datos de conexión al servidor (IP / puerto) y el login, que permanecerá activo. El traspaso de datos será mediante una API con JWT, que mantendrá la sesión activa por varios meses.

## Justificación de la necesidad

Este software permitiría a los usuarios poder disfrutar de escuchar música libremente, sin anuncios y gestionándolo todo desde su servidor, necesidad cada vez más creciente debido al abuso de estas empresas de streaming hacia sus consumidores cada vez dando servicios de menos calidad solo para intentar recaudar más dinero.

## Tecnologías a emplear

| Tecnología | Uso |
|---|---|
| **Spring Boot (REST)** | API, lógica back-end y servidor web |
| **FFMPEG** | Procesado de audio en fragmentos para streaming |
| **Thymeleaf + Tailwind + hls.js** | Front-end del cliente web y recepción de streaming |
| **MongoDB** | Base de datos principal por su flexibilidad con datos dinámicos (playlists, likes…) |
| **Jetpack Compose** | Aplicación móvil y televisión (Android) |
| **Media3** | Recepción de streaming en la app móvil |

---

## Diagrama de clases

```mermaid
classDiagram
    direction TB

    class Song {
        - Long id
        - String title
        - int duration_ms
        - String genre
        - int listeners
        - String picture_url
        + getId() Long
        + getTitle() String
        + getDuration_ms() int
        + getGenre() String
        + getListeners() int
        + getPicture_url() String
        + setTitle(String)
        + setDuration_ms(int)
        + setGenre(String)
        + setListeners(int)
        + setPicture_url(String)
        + setAlbum(Album)
        + setArtists(List~Artist~)
    }

    class Album {
        - Long id
        - String name
        - int duration_ms
        - String picture_url
        + getId() Long
        + getName() String
        + getDuration_ms() int
        + getPicture_url() String
        + setName(String)
        + setDuration_ms(int)
        + setPicture_url(String)
        + setArtists(List~Artist~)
        + setSongs(List~Song~)
    }

    class Artist {
        - Long id
        - String name
        - int listeners
        - String picture_path
        + getId() Long
        + getName() String
        + getListeners() int
        + getPicture_path() String
        + setName(String)
        + setListeners(int)
        + setPicture_path(String)
        + setAlbums(List~Album~)
        + setSongs(List~Song~)
    }

    class Playlist {
        - Long id
        - int duration_ms
        - boolean isPublic
        + Playlist()
        + Playlist(List~Song~)
        + getId() Long
        + getDuration_ms() int
        + isPublic() boolean
        + setSongs(List~Song~)
        + setCreator(User)
        + setPublic(boolean)
    }

    class User {
        - Long id
        - String username
        + getId() Long
        + getUsername() String
        + getProfile() Profile
        + setUsername(String)
        + setProfile(Profile)
    }

    class Admin {
    }

    class Profile {
        - Long id
        - String name
        - String avatarURL
        + getId() Long
        + getName() String
        + getAvatarURL() String
        + getFavouriteSong() Song
        + setName(String)
        + setAvatarURL(String)
        + setFavouriteSong(Song)
    }

    %% Herencia
    Admin --|> User : es un

    %% Relaciones User
    User "1" --> "1" Profile : tiene
    User "1" --> "N" Playlist : crea

    %% Relaciones Profile
    Profile "N" --> "1" Song : tiene como favorita

    %% Relaciones música
    Album "N" o--o "N" Artist : es grabado por
    Album "1" --> "N" Song : contiene
    Song "N" o--o "N" Artist : es interpretada por
    Playlist "N" o--o "N" Song : agrupa
```

---

## Diagramas de casos de uso

### UC1 — Añadir carpeta al path

```mermaid
graph LR
    Admin["👤 Administrador"]

    subgraph Sistema Self-Potify
        UC1("Añadir carpeta al path")
        UC1a("Leer etiquetas ID3")
        UC1b("Crear / actualizar Artista")
        UC1c("Crear / actualizar Álbum")
        UC1d("Persistir Canción")
    end

    Admin --> UC1
    UC1 -.->|include| UC1a
    UC1a -.->|include| UC1b
    UC1a -.->|include| UC1c
    UC1a -.->|include| UC1d
```

### UC2 — Crear playlist y añadir canciones

```mermaid
graph LR
    User["👤 Usuario"]

    subgraph Sistema Self-Potify
        UC2("Crear playlist")
        UC2a("Buscar canción")
        UC2b("Añadir canción a playlist")
    end

    User --> UC2
    User --> UC2b
    UC2b -.->|include| UC2a
```

### UC3 — Registro y creación de perfil

```mermaid
graph LR
    NewUser["👤 Usuario nuevo"]

    subgraph Sistema Self-Potify
        UC3("Registrarse")
        UC3a("Crear cuenta de usuario")
        UC3b("Completar perfil")
    end

    NewUser --> UC3
    UC3 -.->|include| UC3a
    UC3 -.->|include| UC3b
```

### UC5 — Escuchar una canción

```mermaid
graph LR
    User["👤 Usuario"]

    subgraph Sistema Self-Potify
        UC5("Escuchar canción")
        UC5a("Hacer streaming de audio")
        UC5b("Incrementar contador de reproducciones")
    end

    User --> UC5
    UC5 -.->|include| UC5a
    UC5 -.->|include| UC5b
```

### UC4 — Login

```mermaid
graph LR
    User["👤 Usuario"]

    subgraph Sistema Self-Potify
        UC4("Iniciar sesión")
        UC4a("Validar credenciales")
        UC4b("Emitir JWT")
    end

    User --> UC4
    UC4 -.->|include| UC4a
    UC4a -.->|include| UC4b
```
