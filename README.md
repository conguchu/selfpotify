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

| Tecnología             | Uso |
|------------------------|---|
| **Spring Boot (REST)** | API, lógica back-end y servidor web |
| **FFMPEG**             | Procesado de audio en fragmentos para streaming |
| **React + Next JS**    | Front-end del cliente web y recepción de streaming |
| **MongoDB**            | Base de datos principal por su flexibilidad con datos dinámicos (playlists, likes…) |
| **Jetpack Compose**    | Aplicación móvil y televisión (Android) |
| **Media3**             | Recepción de streaming en la app móvil |

---

## Decisiones de diseño

### Arquitectura
He decidido crear esta aplicación basada en **microservicios** en vez de usar una arquitectura monolítica. Esto porque pienso que 
así puedo desarrollar una aplicación más escalable, cuyo core sea el servidor API de springboot, del que consumen diferentes clientes
como el web o mobile, dándome la posibilidad a futuro de crear más para otras plataformas.

### Despliegue

**Este proyecto está pensado para usuarios técnicos** que quieren reemplazar Spotify por una tecnología similar, accesible y sobre todo más económica y libre,
por lo que será su responsabilidad montar y mantener el servidor, así como la mía facilitar lo máximo posible la instalación, configuración y set-up de la 
estructura de red para permitir el acceso desde internet. Por esto, al arrancar el servidor por primera vez, tendrá un pequeño wizard web que permite cambiar estos parámetros (IP de acceso, directorios de música...).

El estado del wizard se persiste en un fichero YAML externo gestionado por `ConfigService`, con el flag `features.setupComplete` como interruptor entre "primer arranque" y "servidor ya operativo". El endpoint `POST /api/config/reset` permite al admin devolver el servidor a su estado de fábrica (vaciado de BBDD, usuarios por defecto `admin/admin` y `user/password`, y config en blanco), volviendo a forzar el wizard en el siguiente arranque.

#### Flujo de setup inicial y reset

```mermaid
flowchart TD
    Start([Arranque del servidor]) --> Load{¿Existe<br/>config.yaml?}
    Load -- no --> Defaults[ConfigService crea<br/>config por defecto<br/>setupComplete=false]
    Load -- sí --> Read[ConfigService carga<br/>YAML en memoria]
    Defaults --> Public
    Read --> Public[GET /api/config/public<br/>devuelve setupComplete]
    Public --> Decide{setupComplete?}
    Decide -- false --> Wizard[Cliente muestra<br/>wizard de setup]
    Wizard --> Setup[POST /api/config/setup<br/>appName, scanPaths,<br/>intervalo, features]
    Setup --> Validate{Validaciones OK?<br/>rutas existen,<br/>30 ≤ intervalo ≤ 86400}
    Validate -- no --> Err400[400 Bad Request]
    Validate -- sí --> Persist[ConfigService persiste<br/>YAML + markSetupComplete]
    Persist --> AsyncScan[Lanzar scan inicial<br/>asíncrono]
    AsyncScan --> Ready([Servidor operativo])
    Decide -- true --> Ready
    Ready -. admin pulsa Reset .-> Reset[POST /api/config/reset]
    Reset --> Wipe[ResetService:<br/>deleteAll en playlists,<br/>songs, albums, artists,<br/>profiles, users]
    Wipe --> Seed[Recrear admin/admin<br/>y user/password]
    Seed --> ResetCfg[ConfigService<br/>resetToDefaults]
    ResetCfg --> Public
```

### Funcionamiento del streaming

Para hacer que los clientes puedan recibir la música en pedazos de bytes con la librería media3, he implementado la ruta de la API
``/api/listen/{id}``, endpoint que soporta http range, permitiendo reproducir sin descargar el archivo completo.

### Gestión de la biblioteca musical

La biblioteca musical será gestionada por los admins, que tendrán la posibilidad de añadir carpetas que el backend escaneará periódicamente en busca de cambios o nuevas canciones, para poder administrar la música de forma sencilla con el explorer.

El escaneo lo dispara `SchedulingConfig` mediante un `PeriodicTrigger` que **relee el intervalo configurado en cada tick**, de forma que los cambios en `scan.intervalSeconds` realizados vía `PUT /api/config` se aplican en caliente sin reiniciar el servidor. La concurrencia se protege con un `ReentrantLock` en `ScanService`: si llega un tick (o un `POST /api/config/scan/run` manual) mientras hay otro escaneo activo, se descarta. Al añadir una ruta nueva vía `POST /api/config/scan-paths` se lanza además un escaneo inicial asíncrono solo de esa carpeta para no esperar al siguiente tick.

#### Flujo del escaneo periódico

```mermaid
flowchart TD
    Tick([Tick del scheduler]) --> ReadInt[Leer intervalSeconds<br/>actual del config]
    ReadInt --> TryLock{tryLock<br/>ScanService}
    TryLock -- false --> Skip[Log: scan en curso,<br/>se omite el tick]
    Skip --> Reschedule[Reprogramar tick<br/>con intervalo actual]
    TryLock -- true --> Loop[Para cada ruta<br/>en scan.paths]
    Loop --> Check{¿directorio<br/>legible?}
    Check -- no --> WarnSkip[Log warn,<br/>siguiente ruta]
    WarnSkip --> Loop
    Check -- sí --> Load[songService.loadFolder:<br/>leer ID3, upsert<br/>artista/álbum/canción]
    Load --> Loop
    Loop -->|fin| Sweep[sweepAvailability:<br/>marcar canciones<br/>cuyo fichero ya no existe]
    Sweep --> Mark[markScanFinished:<br/>guardar lastRunEpochSec<br/>en YAML]
    Mark --> Unlock[Liberar lock]
    Unlock --> Reschedule
    Reschedule --> Tick
```

---

## Gestión de recursos

Al ser un aplicativo pensado para un uso personal, normalmente con pocos usuarios, el servidor no requiere de grandes prestaciones hardware. 
Sí serán necesarios unos mínimos para poder emitir correctamente el streaming, como una buena conexión de red (CAT5 mínimo) y 2 GB de RAM. 

La única limitación de recursos en el uso de la aplicación, al estar tratando con archivos multimedia, es el espacio en disco del server para almacenar la música. No hay un mínimo, pero se recomienda tener abundante (200 GB) para poder llegar a disponer 
de un catálogo considerable de música, sobre todo si el usuario se preocupa por la calidad de la misma. 

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
        - int bpm
        - String songPath
        - List~Artist~ artists
        - Album album
        - String picture_url
        + copy(Song)
    }

    class Album {
        - Long id
        - String name
        - int duration_ms
        - String picture_url
        - List~Artist~ artists
        - List~Song~ songs
        + copy(Album)
    }

    class Artist {
        - Long id
        - String name
        - int listeners
        - String picture_path
        - List~Album~ albums
        - List~Song~ songs
        + copy(Artist)
    }

    class Playlist {
        - Long id
        - List~Song~ songs
        - int duration_ms
        - boolean isPublic
        - User creator
        + copy(Playlist)
    }

    class User {
        - Long id
        - Profile profile
        - String username
        - String password
        + copy(User)
    }

    class Admin {
    }

    class Profile {
        - Long id
        - String name
        - String avatarURL
        - Song favouriteSong
        + copy(Profile)
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

### UC6 — Setup inicial del servidor

```mermaid
graph LR
    Admin["👤 Administrador"]

    subgraph Sistema Self-Potify
        UC6("Completar setup inicial")
        UC6a("Definir branding<br/>(appName, colores, logo)")
        UC6b("Registrar rutas de escaneo")
        UC6c("Fijar intervalo de escaneo")
        UC6d("Persistir config en YAML<br/>y marcar setupComplete")
        UC6e("Lanzar escaneo inicial")
    end

    Admin --> UC6
    UC6 -.->|include| UC6a
    UC6 -.->|include| UC6b
    UC6 -.->|include| UC6c
    UC6 -.->|include| UC6d
    UC6d -.->|include| UC6e
```

### UC7 — Reset del servidor

```mermaid
graph LR
    Admin["👤 Administrador"]

    subgraph Sistema Self-Potify
        UC7("Resetear servidor")
        UC7a("Vaciar base de datos")
        UC7b("Recrear usuarios por defecto")
        UC7c("Restaurar config a valores de fábrica")
    end

    Admin --> UC7
    UC7 -.->|include| UC7a
    UC7 -.->|include| UC7b
    UC7 -.->|include| UC7c
```

### UC8 — Gestionar branding y logo

```mermaid
graph LR
    Admin["👤 Administrador"]

    subgraph Sistema Self-Potify
        UC8("Actualizar branding")
        UC8a("Validar appName / colores hex")
        UC8b("Subir logo (PNG/JPG/SVG/WebP, ≤2MB)")
        UC8c("Persistir en YAML y servir vía /assets/**")
    end

    Admin --> UC8
    UC8 -.->|include| UC8a
    UC8 -.->|include| UC8b
    UC8b -.->|include| UC8c
```

## Diagrama de arquitectura

![Diagrama sin título.drawio.png](img/Diagrama%20sin%20t%C3%ADtulo.drawio.png)