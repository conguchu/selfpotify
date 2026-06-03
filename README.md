

<div align="center">
  <img src="img/selfpotify-logo.png" alt="selfpotify-logo" width="240"/>
  <h1>SelfPotify</h1>
</div>

## Objetivos

Mi idea para mi proyecto de fin de grado es crear un "clon" alternativo de código abierto de Spotify. Funcionará con tecnologías de streaming, permitiendo escuchar la música con baja latencia sin tener que esperar a que descargue ningún archivo igual que en el original, y tendrá una pequeña recreación de los sistemas de recomendaciones para que el usuario pueda descubrir música y organizarla a su manera en playlists.

El proyecto incluiría:

- **Backend Self-Potify** — Sirve la API a los clientes. Contiene toda la librería musical organizada en carpetas, además de la BBDD que almacenará tanto los usuarios como sus likes / playlists.
- **Cliente web** — Para escuchar la música del servidor en streaming desde un ordenador. Esto será a través de un servidor web en el que puedes acceder solamente con tu login de usuario.
- **Cliente móvil / televisión** — Aplicación para Android con las mismas funciones que la web pero mayor rendimiento. Al entrar por primera vez, se tendrá que configurar para poner los datos de conexión al servidor (IP / puerto) y el login, que permanecerá activo. El traspaso de datos será mediante una API con JWT, que mantendrá la sesión activa por varios meses.

## Justificación de la necesidad

Este software permitiría a los usuarios administradores levantar una app para sí mismos y sus amigos (creandoles usuarios a parte) poder disfrutar de escuchar música libremente, sin anuncios y gestionándolo todo desde su servidor, necesidad cada vez más creciente debido al abuso de estas empresas de streaming hacia sus consumidores cada vez dando servicios de menos calidad solo para intentar recaudar más dinero.

## Tecnologías a emplear

| Tecnología             | Uso                                                                          |
|------------------------|------------------------------------------------------------------------------|
| **Spring Boot (REST)** | API, lógica back-end y servidor web                                          |
| **FFMPEG**             | Procesado de audio en fragmentos para streaming                              |
| **React + Next JS**    | Front-end del cliente web y recepción de streaming                           |
| **MariaDB**            | Base de datos principal por su fiabilidad y experiencia con ella.            |
| **Jetpack Compose**    | Aplicación móvil y televisión (Android)                                      |
| **Media3**             | Recepción de streaming en la app móvil                                       |
| **Docker Compose**     | Despliegue de la aplicación en contenedores                                  |
| **Nginx**              | Proxy interno para enrutar los servidores Next y Spring cuando se usa docker |


### Uso de Claude Code

Muchas de las líneas de código de este proyecto se han escrito usando Claude Code. Esto ha permitido centrarse más en la experiencia de los usuarios finales y en las features de la app. Para poder seguir un desarrollo
sostenible en la app sin perderse y dejar de entender su funcionamiento, se han seguido prácticas como documentar todas
las features y decisiones en este readme, crear feature branches con commits muy descriptivos, etc.

El flujo de trabajo normalmente fue:

1. **Detectar necesidad**: Pienso una feature que me gustaría agregar, abro la app y encuentro algún bug... 
2. **Crear un plan**: Teniendo una feature o un fix pendiente, explico lo más detallada y técnica posible a la herramienta qué es lo que quiero cambiar o arreglar. La herramienta me propone una solución al problema en forma de archivo markdown, la cual leo entera y voy moldeando y corrigiendo. 
3. **Lanzo el plan**: Claude se ocupa de ejecutarlo y de modificar automáticamente los archivos escribiendo o cambiando el código. 
4. **Revisión**: Me encargo de asegurarme de que todo lo que ha escrito claude funciona correctamente sin romper nada y tal y como lo había planeado.
5. **Documentación**: Escribo en el README.md las decisiones de desarrollo y diseño que he tomado y porqué lo he hecho, para guardar constancia de ello.

#### Skills

A la hora de trabajar con claude, una de las características que más he utilizado son las skills.

Son colecciones de archivos de texto que le dan contexto a la inteligencia artifical para poder seguir por ejemplo buenas prácticas en un lenguaje de programación o framework. El desarrollo del proyecto se ha apoyado sobre todo en las skills next-js-best-practices, para todo la creación del front end, y otra llamada java-springboot, que fue usada para trabajar en el backend.

#### Archivo Claude.md

En el proyecto, sin versionar, se almacenó un archivo llamado CLAUDE.md. Este archivo son las directrices que claude va a seguir cuando lo ejecuto en el mismo directorio donde se encuentra el archivo markdown. En este caso, ha sido de gran ayuda ya que así no hay que todo el rato estar dándole las convecciones del proyecto, arquitectura, etc.

Algunas de las directrices a destacar han sido:

- Si un cambio colisiona con las decisiones del readme, preguntar para saber si adaptarse al readme o cambiarlo.
- Actualizar automáticamente los diagramas al cambiar cosas que los afecten.



---

## Decisiones de diseño

### Arquitectura
He decidido crear esta aplicación basada en **microservicios** en vez de usar una arquitectura monolítica. Esto porque pienso que 
así puedo desarrollar una aplicación más escalable, cuyo core sea el servidor API de springboot, del que consumen diferentes clientes
como el web o mobile, dándome la posibilidad a futuro de crear más para otras plataformas.

Estos microservicios están todos alojados en este **monorepo**, con solamente ejecutar `docker-compose up` se pone la aplicación a funcionar.

### Despliegue e instalación

Como se comentó antes, Selfpotify es un monorepo y ofrece la posibilidad de **desplegarlo con docker**, precisando especificar una ruta con la música para que se monte como volumen (con posibilidad de reescanearlo para no reiniciar el contenedor cada vez que se quiere añadir música). También es posible **hacer un despliegue bare metal**, ideal para trabajar por ejemplo con unidades externas permitiendo gestionar varias carpetas de source para la biblioteca musical.

**Este proyecto está pensado para usuarios técnicos** que quieren reemplazar Spotify por una tecnología similar, accesible y sobre todo más económica y libre, por lo que será su responsabilidad montar y mantener el servidor, así como la mía facilitar lo máximo posible la instalación, configuración y set-up de la estructura de red para permitir el acceso desde internet.

Por esto, en el **primer arranque** el servidor entra en **modo setup** y la web sirve un **wizard de configuración inicial al que se accede sin login**: mientras la instalación no esté completada, cualquier acceso al cliente web redirige siempre a este wizard. En él, el administrador deja el servidor operativo de una pasada — **branding** (nombre, **colores del tema** y logo de la app), **biblioteca musical** (directorios a escanear e intervalo de escaneo) y **usuarios** (cuentas iniciales). El wizard funciona sin autenticación porque, en modo setup, el backend reabre temporalmente los endpoints que necesita (`POST /api/config/setup`, `PUT /api/config`, `POST /api/config/logo`, `POST /api/users`); el control real lo ejerce un guard dinámico (`@setupGuard.inSetupMode()`) ligado al flag `features.setupComplete`.


El estado del wizard se persiste en un fichero YAML externo gestionado por `ConfigService`, con el flag `features.setupComplete` como interruptor entre "primer arranque" y "servidor ya operativo". Al confirmar el wizard, `POST /api/config/setup` marca `setupComplete=true`: el wizard queda **inaccesible** (el cliente deja de redirigir a él) y esos endpoints vuelven a exigir rol `ADMIN`. El endpoint `POST /api/config/reset` permite al admin devolver el servidor al mismo estado en que arrancaría tras un primer despliegue: vacía la BBDD y la config, y reproduce los bootstraps de arranque — reseedea el admin desde `ADMIN_USERNAME`/`ADMIN_PASSWORD` del `.env` (si no están definidos no se crea ningún usuario) y reañade la librería musical del `.env` a `scan.paths` (si está configurada y accesible). Tras el reset, el wizard se vuelve a forzar en el siguiente acceso.

**Decisión de diseño: el selector de colores no deja elegir combinaciones inaccesibles.** Tanto en el wizard como en los ajustes del panel (`ThemeSettings`), el branding de color se controla con **dos semillas** —primario (acento) y secundario (fondo)— de las que se **deriva la paleta completa de 14 colores** en el espacio HCT de Material (`lib/palette.ts`, `derivePalette`), calculando los textos por **contraste WCAG real** (AAA/AA) contra el fondo. Encima hay una galería de **presets accesibles** (semillas curadas) para arrancar de un tema válido con un clic. El color del texto sobre botones (`--color-on-accent`) y el del acento usado como texto/icono sobre el fondo (`--color-accent-text`) **no se almacenan**: se recalculan siempre al aplicar, de modo que sigan al acento/fondo aunque se editen a mano. El **modo avanzado** permite editar los 14 colores uno a uno, pero pasa por una **red de seguridad** (`enforceContrast`) que, tanto en el preview como al pintar la app real, empuja cualquier color de texto ilegible al tono legible más cercano conservando su matiz. Así, ninguna combinación —ni siquiera una editada a mano o heredada de una config antigua— puede dejar textos o iconos invisibles.

Además del wizard, se pueden tocar otras configuraciones que no están ahí (normalmente porque son más técnicas) en el envfile (ver sección "Variables clave del .env").

#### Flujo de setup inicial y reset

```mermaid
flowchart TD
    Start([Arranque del servidor]) --> Load{¿Existe<br/>config.yml?}
    Load -- no --> Defaults[ConfigService crea<br/>config por defecto<br/>setupComplete=false]
    Load -- sí --> Read[ConfigService carga<br/>YAML en memoria]
    Defaults --> Public
    Read --> Public[GET /api/config/public<br/>devuelve setupComplete]
    Public --> Decide{setupComplete?}
    Decide -- false --> Wizard[Cliente redirige SIEMPRE al wizard<br/>sin login: branding+colores+logo,<br/>biblioteca y usuarios<br/>PUT /api/config · POST /logo · POST /api/users]
    Wizard --> Setup[POST /api/config/setup<br/>commit final: appName, scanPaths,<br/>intervalo, features]
    Setup --> Validate{Validaciones OK?<br/>rutas existen,<br/>30 ≤ intervalo ≤ 86400}
    Validate -- no --> Err400[400 Bad Request]
    Validate -- sí --> Persist[ConfigService persiste<br/>YAML + markSetupComplete]
    Persist --> AsyncScan[Lanzar scan inicial<br/>asíncrono]
    AsyncScan --> Ready([Servidor operativo])
    Decide -- true --> Ready
    Ready -. admin pulsa Reset .-> Reset[POST /api/config/reset]
    Reset --> Wipe[ResetService:<br/>deleteAll en escuchas, follows,<br/>tokens y colaboradores, playlists,<br/>songs, álbumes, artistas,<br/>perfiles, usuarios]
    Wipe --> ResetCfg[ConfigService<br/>resetToDefaults]
    ResetCfg --> Reboot[Re-ejecutar bootstraps:<br/>admin desde .env<br/>+ librería del .env a scan.paths]
    Reboot --> Public
```

#### Empaquetado y arranque con Docker

Para facilitar al máximo el set-up al usuario técnico, el servidor se empaqueta como una pila de **tres contenedores** orquestada con `docker compose`, manteniendo la filosofía de microservicios y permitiendo escalar o reiniciar cada pieza por separado:

- **`api`** — Spring Boot (`Dockerfile.api`, build multi-stage con Maven → JRE Alpine). Escucha en `:8080`, expuesto al host para los clientes Android/TV. Persiste `config.yml`, logo y assets en el volumen Docker `selfpotify-data` montado en `/data/selfpotify`.
- **`next`** — Frontend Next.js (`front/Dockerfile`, build con `output: "standalone"`). Escucha en `:3000` **solo en la red interna del compose**; nunca se publica al host.
- **`web`** — Nginx (`docker/web/`) escuchando en `:80` (único puerto público del front). Sirve los estáticos `_next/static/`, hace `proxy_pass` a `next:3000` para SSR y a `api:8080` para `/api/*` y `/assets/*`. Con esto, el navegador habla siempre con un único host (`:80`) y se evita CORS y la exposición pública directa del backend a través del front.

Los clientes web pasan por Nginx (`:80`); los clientes Android/TV consumen la API directamente (`:8080`).

```mermaid
flowchart LR
    Browser["Navegador web"]
    Mobile["Cliente Android / TV"]

    subgraph Host["Host (docker compose)"]
        direction LR
        Web["web<br/>(Nginx :80)"]
        Next["next<br/>(Next standalone :3000)"]
        Api["api<br/>(Spring Boot :8080)"]
        Vol[("volumen<br/>selfpotify-data<br/>/data/selfpotify")]
    end

    Browser -->|http :80| Web
    Mobile -->|http :8080| Api

    Web -->|/| Next
    Web -->|/api/, /assets/| Api
    Api --- Vol
```

##### Variables clave del `.env`

Toda la configuración por instalación se declara en `.env` (ver `.env.example`). En modo Docker conviene revisar especialmente:

| Variable | Valor recomendado en Docker | Para qué sirve |
|---|---|---|
| `SERVER_PORT` | `8080` | Puerto interno de la API (no cambiar salvo conflicto). |
| `WEB_PORT` | `80` | Puerto público de Nginx (cliente web). Cambiar si `:80` está ocupado o no hay permisos de root. |
| `WEB_ORIGIN` | `http://localhost` (o el host público) | CORS del backend. Sin puerto porque el navegador entra por Nginx en `:80`. |
| `JWT_SECRET` | cadena aleatoria ≥32 chars | Firma de JWT; obligatorio cambiarlo del valor del ejemplo. |
| `ADMIN_USERNAME` / `ADMIN_PASSWORD` | credenciales iniciales | Admin auto-bootstrap en el primer arranque si la BBDD está vacía. |
| `DB_URL` | `jdbc:h2:file:/data/selfpotify/db/selfpotify;AUTO_SERVER=TRUE` | Para persistir la BBDD entre reinicios del contenedor (con `DB_DDL_AUTO=update`). El valor por defecto (H2 in-memory) pierde los datos al reiniciar. |
| `APP_CONFIG_PATH` | **no sobreescribir** | Lo fija el contenedor a `/data/selfpotify/config.yml`, que vive en el volumen `selfpotify-data` y sobrevive a reinicios. |
| `H2_CONSOLE_ENABLED` | `false` | Deshabilitar la consola H2 en despliegue. |


### Funcionamiento del streaming

Para hacer que los clientes puedan recibir la música en pedazos de bytes con la librería media3, he implementado la ruta de la API
``/api/listen/{id}``, endpoint que soporta HTTP Range, permitiendo reproducir sin descargar el archivo completo.

**Decisión de diseño: stream tokens para no exponer el JWT de sesión en la URL de audio.** El elemento HTML `<audio>` y el player de Media3 (Android) no permiten añadir cabeceras personalizadas (`Authorization`) a las peticiones que generan automáticamente, lo que obligaría a pasar el JWT como query param (`?token=<jwt>`). Un JWT en la URL queda registrado en logs del servidor, historial del navegador y cabeceras `Referer`, comprometiendo la sesión completa.

En lugar de eso, el cliente solicita primero un **stream token** ligero vía `POST /api/listen/token` (con el JWT en la cabecera `Authorization`, como cualquier otra llamada a la API). El stream token es un UUID aleatorio, sin claims JWT, que solo sirve para `/api/listen/{id}`. Se pasa como `?st=<streamToken>` en la URL de audio. Características del token:

- **Sin claims de sesión:** no autentica ante ningún otro endpoint.
- **Corta vida:** expira a las 4 horas (suficiente para una sesión de escucha continua).
- **Reutilizable dentro de su TTL:** necesario porque el navegador/player hace múltiples peticiones HTTP Range a la misma URL al hacer seek; invalidarlo en la primera petición rompería la reproducción.
- **Ligado al usuario:** el `StreamTokenService` almacena el username junto al token y lo recupera al validar, sin necesidad de contexto de seguridad de Spring.

### Gestión de la biblioteca musical

La biblioteca musical será gestionada por los admins, que tendrán la posibilidad de añadir carpetas que el backend escaneará periódicamente en busca de cambios o nuevas canciones, para poder administrar la música de forma sencilla con el explorer.

El escaneo lo dispara `SchedulingConfig` mediante un `PeriodicTrigger` que **relee el intervalo configurado en cada tick**, de forma que los cambios en `scan.intervalSeconds` realizados vía `PUT /api/config` se aplican en caliente sin reiniciar el servidor. La concurrencia se protege con un `ReentrantLock` en `ScanService`: si llega un tick (o un `POST /api/config/scan/run` manual) mientras hay otro escaneo activo, se descarta. Al añadir una ruta nueva vía `POST /api/config/scan-paths` se lanza además un escaneo inicial asíncrono solo de esa carpeta para no esperar al siguiente tick.

#### Subida de audios desde el panel (drag & drop)

Además de registrar carpetas del servidor, el panel admin permite **subir audios sueltos** (`POST /api/songs/upload`, gestionado por `SongUploadService`). La decisión de diseño clave es **dónde** se escriben: el volumen de música se monta **read-only** en Docker (`/music:ro`), así que los audios subidos no pueden ir ahí. Se guardan en una carpeta `selfpotify_added` **escribible**:

- **En Docker**, dentro del volumen de datos persistente (`/data/selfpotify/selfpotify_added`), el mismo que ya guarda `config.yml` y los assets. El panel no deja elegir ruta porque solo ese volumen es escribible.
- **En local**, dentro de la ruta de música que elija el admin de entre las ya configuradas (`<ruta>/selfpotify_added`) o, por defecto, la carpeta de datos (`~/.selfpotify/selfpotify_added`).

La subida ocurre en **dos fases** (`SongUploadService`) para que el admin revise y ajuste los metadatos **antes** de incorporar la canción, pero pasando por las mismas APIs externas que cualquier otra importación:

- **Staging** (`POST /api/songs/upload`): el audio se guarda en una carpeta temporal `selfpotify_staging/<token>` que **no** está en las rutas de escaneo (para que el escaneo periódico no la importe a medias). Se extraen los metadatos ID3 y, antes de devolver el borrador editable (`SongDraftDTO`), se **enriquece con las mismas fuentes externas que el escaneo** para que el admin vea los datos ya completos en la pantalla de edición previa: **nombre canónico del artista** (Last.fm), **género** si falta (Last.fm) y **carátula** si el audio no traía embebida (Cover Art Archive → iTunes → Deezer).
- **Commit** (`POST /api/songs/commit`): con los metadatos ya ajustados, el audio se mueve a la carpeta `selfpotify_added` **escribible** y se persiste la canción. El artista se resuelve **por MBID** (Last.fm), igual que en el escaneo; tras guardar se rellenan de forma **idempotente** el género/carátula que aún falten y la **foto del artista** (Deezer), que no se ve en la pantalla de edición.

La carpeta `selfpotify_added` **no** se registra como ruta de escaneo: el commit ya persiste cada canción con su `songPath` definitivo y el barrido de disponibilidad del escaneo la mantiene mientras el fichero exista. Así una canción subida es indistinguible de una escaneada del disco. La resolución de identidad del artista (limpieza del nombre, consulta a Last.fm y emparejamiento por MBID) es lógica compartida en `ArtistResolver`, usada tanto por el escaneo como por el commit.

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

#### Resolución de identidad de artistas

El artista de cada canción se deduce del tag ID3 `ARTIST` (o del nombre de archivo), un valor que escribe quien etiquetó el MP3 y que es inconsistente entre archivos del mismo artista: emojis, espacios sobrantes, mayúsculas, alias o abreviaciones. Emparejar por comparación exacta del nombre hacía que el mismo artista real acabara en varias filas `Artist` distintas (caso observado: `El Alfa`, `✅EL ALFA EL JEFE` y `Alfa` como tres artistas separados; `Mala  fe` y `Mala Fe` como dos).

La decisión es **no fiarse del string del tag y resolver cada artista contra una fuente de verdad externa**. Se descartaron la normalización pura del nombre (no resuelve alias ni abreviaciones), la tabla de alias manual (requiere mantenimiento) y el *fuzzy matching* (riesgo de fusionar artistas reales distintos). Se eligió Last.fm porque el proyecto ya lo integra para clasificar géneros, así que no añade ni dependencias ni variables de entorno nuevas.

Durante el escaneo, `SongService.resolveArtist` limpia el nombre de adornos, lo consulta en Last.fm (`artist.getInfo` con `autocorrect=1`) y obtiene el **nombre canónico** y el **MBID** (MusicBrainz ID, identificador estable). El emparejamiento pasa a hacerse por MBID —no por nombre—, persistido en la columna `Artist.mbid`. Si una fila ya existía sin MBID, se le rellena. Si Last.fm no está configurado o no reconoce al artista, se cae al emparejamiento por nombre limpio, que ya unifica los casos triviales (espacios, mayúsculas). Una caché por lote evita repetir llamadas HTTP dentro del mismo escaneo.

Esta estrategia previene **nuevos** duplicados; los ya existentes en BD se limpian a mano desde el panel con las operaciones de **separar** y **juntar** artistas (ver [Gestión de artistas desde el panel](#gestión-de-artistas-desde-el-panel-edición-separar-y-juntar)).

```mermaid
flowchart TD
    Raw([Nombre del tag ID3]) --> Clean[Limpiar nombre:<br/>quitar emojis/símbolos,<br/>colapsar espacios]
    Clean --> LastFm[Last.fm artist.getInfo<br/>autocorrect=1]
    LastFm --> HasId{¿Devuelve MBID?}
    HasId -- sí --> ByMbid{findByMbid}
    ByMbid -- existe --> Reuse[Reusar artista]
    ByMbid -- no --> ByName1{findByNameIgnoreCase<br/>nombre canónico}
    ByName1 -- existe --> Backfill[Rellenar MBID<br/>en la fila existente]
    ByName1 -- no --> Create[Crear artista<br/>con nombre canónico + MBID]
    HasId -- no / sin API key --> ByName2{findByNameIgnoreCase<br/>nombre limpio}
    ByName2 -- existe --> Reuse
    ByName2 -- no --> CreatePlain[Crear artista<br/>solo con nombre]
    Backfill --> Reuse
```

### Gestión de artistas desde el panel (edición, separar y juntar)

El panel de administración incluye una pestaña **Artistas** (vista de lista) para gestionar el catálogo de artistas más allá de lo que resuelve el escaneo automático. Cada artista se puede **editar** (nombre y foto) en una página aparte, y la lista ofrece dos operaciones de limpieza de duplicados/etiquetas: **separar** y **juntar**.

**Decisión de diseño: la edición y la subida de foto reutilizan la infraestructura existente.** La foto del artista se sube por drag&drop al mismo almacén que las carátulas (`POST /api/songs/cover` → `/assets/covers/<sha256>`) y su URL se guarda en `Artist.picture_path` vía `PUT /api/artists/{id}`. No se añade ni endpoint de imagen ni almacén nuevos: es el mismo patrón que la carátula de canción. La página de edición ofrece además un botón **«Conseguir foto automáticamente»** (`POST /api/artists/{id}/fetch-photo`) que busca la foto en Deezer por el nombre y la propone en el formulario sin persistirla (se guarda al confirmar), respetando `app.cover-art.enabled`. El `PUT` solo toca **nombre y foto**; el `MBID` no se edita a mano porque es identidad resuelta automáticamente (un nombre editado a mano sobrevive a futuros escaneos, que emparejan por MBID sin renombrar las filas existentes).

**Separar un artista (split).** Resuelve el caso de un único tag que en realidad son varios artistas (p. ej. `Ill Pekeño / Ergo Pro`). El admin teclea los nombres reales (mínimo dos) y `POST /api/artists/{id}/split`:

1. Resuelve **cada nombre con el mismo `ArtistResolver` que el escaneo** (Last.fm → nombre canónico + MBID), reutilizando un artista existente si ya estaba. Se eligió reusar el resolver —en vez de crear filas planas por el nombre tecleado— para mantener la coherencia con la decisión de identidad de artistas: los artistas resultantes nacen ya con su MBID. En la UI cada campo tiene un **buscador con lupa** sobre la BBDD para localizar y reutilizar un artista que ya exista.
2. **Atribuye todas las canciones y álbumes del original a TODOS los resultantes** (no las reparte: cada canción pasa a tener a los dos/tres artistas).
3. **Rellena la foto** (Deezer) de los resultantes que aún no la tengan —los recién creados—, reutilizando `CoverApiService` igual que el escaneo y **respetando `app.cover-art.enabled`**: si la resolución online de carátulas está desactivada en config, no se consulta nada.
4. **Borra el artista original.**

**Juntar artistas (merge).** Resuelve los duplicados que ya están en BD (p. ej. `El alfa` y `El Alfa`, que el escaneo creó como filas distintas antes de tener MBID). El admin selecciona dos o más artistas y elige un **superviviente**; `POST /api/artists/merge`:

1. El superviviente **conserva su id y su MBID**.
2. Absorbe las canciones y álbumes del resto (sin duplicar atribuciones).
3. **Borra los demás**; opcionalmente se renombra al superviviente.

Se eligió el modelo **superviviente** (en lugar de crear un artista nuevo y borrar todos) para **preservar un id estable** —cualquier referencia existente al superviviente sigue siendo válida— y el **MBID ya resuelto**, evitando además una llamada extra a Last.fm. Es la "limpieza puntual" de duplicados que anticipaba la sección de resolución de identidad.

**Soltar las FKs antes de borrar.** Un artista está referenciado por tres tablas cruzadas: `song_artist`, `album_artist` y la de `recommendedArtists` del feed. Tanto separar como juntar (y el borrado individual) **desligan al artista de esas tres** antes de eliminar su fila, para no chocar con las restricciones de clave foránea. Para el feed basta con quitar la referencia: el feed se **regenera en el siguiente acceso al home**, así que no hace falta repuntar nada. Borrar un artista nunca borra sus canciones ni sus álbumes: solo dejan de atribuírsele.

**Edición de álbumes desde el artista.** Desde la página de un artista se accede a la lista de sus álbumes (`/admin/artists/{id}/albums`), donde cada álbum se edita (nombre y portada) con `PUT /api/albums/{id}`. Ese `PUT` pasó a recibir solo **nombre y portada** (`AlbumUpdateRequest`) en lugar de la entidad `Album` completa: un body parcial sobre la entidad habría puesto a `null` las asociaciones (`album_artist`, canciones) al copiarlas, borrando el vínculo con el artista.

```mermaid
flowchart TD
    subgraph Split[Separar artista]
        S0([POST /api/artists/id/split<br/>names]) --> S1{¿≥2 nombres?}
        S1 -- no --> SErr[400 Bad Request]
        S1 -- sí --> S2[Resolver cada nombre<br/>con ArtistResolver<br/>Last.fm → nombre + MBID]
        S2 --> S3{¿≥2 artistas distintos<br/>del original?}
        S3 -- no --> SErr
        S3 -- sí --> S4[Atribuir TODAS las canciones<br/>y álbumes del original<br/>a TODOS los resultantes]
        S4 --> SPhoto[Rellenar foto de los nuevos<br/>Deezer · si cover-art enabled]
        SPhoto --> S5[Soltar FKs del original:<br/>song_artist · album_artist · feed]
        S5 --> S6[Borrar el original]
        S6 --> SOk([200 OK · artistas resultantes])
    end

    subgraph Merge[Juntar artistas]
        M0([POST /api/artists/merge<br/>ids · survivorId · name]) --> M1{¿≥2 ids y survivor<br/>entre ellos?}
        M1 -- no --> MErr[400 Bad Request]
        M1 -- sí --> M2[Por cada absorbido:<br/>mover canciones y álbumes<br/>al superviviente sin duplicar]
        M2 --> M3[Soltar FKs del absorbido<br/>y borrarlo]
        M3 --> M4{¿quedan absorbidos?}
        M4 -- sí --> M2
        M4 -- no --> M5[Renombrar superviviente<br/>si llega name]
        M5 --> MOk([200 OK · superviviente])
    end
```

### Panel de administración web

Todo lo administrativo del cliente web vive en un **grupo de rutas aparte, `front/app/(admin)/`**, separado del grupo `(app)` de usuario. Su layout monta un `AdminShell` envuelto en `ProtectedRoute requireAdmin`: un visitante sin sesión va a `/login` y un usuario autenticado **sin rol admin** se redirige a `/home`, de modo que el panel nunca se renderiza para quien no debe verlo. El backend vuelve a exigir rol `ADMIN` en cada uno de estos endpoints, así que el guard del front es **conveniencia de UX, no la frontera de seguridad real**.

**Decisión de diseño: un único `AdminShell` con navegación superior fija, no sidebar.** Todas las páginas admin comparten la misma cabecera con cinco entradas —**Resumen** (`/admin`), **Usuarios** (`/admin/users`), **Canciones** (`/admin/songs`), **Artistas** (`/admin/artists`) y **Ajustes** (`/admin/settings`)— más un acceso «Ir a la app» que devuelve al `/home` de usuario. El logo y el nombre de la cabecera son el mismo branding dinámico que el resto de la app.

Las secciones:

- **Resumen** (`/admin`) — tarjetas de recuento (canciones, artistas, álbumes, usuarios, playlists) que enlazan a su gestión, y un botón **«Re-escanear biblioteca»** (`POST /api/config/scan/rescan`) que dispara un escaneo de cambios bajo demanda y reporta el resultado (`added`/`recovered`/`skipped`/`failed`), devolviendo `409` si ya hay un escaneo en curso. Aloja también la **Zona de peligro** (ver más abajo).
- **Usuarios** (`/admin/users`) — alta (`POST /api/users`, con interruptor «Administrador»), cambio de rol en vivo (`PUT /api/users/{id}/role`), cambio de contraseña (`PUT /api/users/{id}`) y borrado (`DELETE /api/users/{id}`). El interruptor de rol respeta el guard del backend de **no degradar al último admin** (`400`), y la fila de la propia cuenta avisa de que quitarse el rol implica perder el acceso al panel.
- **Canciones** (`/admin/songs`) — catálogo con búsqueda, subida drag & drop en dos fases y edición de metadatos por canción (ver [Subida de audios desde el panel](#subida-de-audios-desde-el-panel-drag--drop) y el caso de uso UC12). La edición reasigna el artista con `PUT /api/songs/{id}/artists` **sin tocar el `songPath`** (la ruta física no se expone en el formulario).
- **Artistas** (`/admin/artists`) — lista con editar, **separar** y **juntar**, más la edición de álbumes por artista (ver [Gestión de artistas desde el panel](#gestión-de-artistas-desde-el-panel-edición-separar-y-juntar)).
- **Ajustes** (`/admin/settings`) — dos pestañas, **Apariencia** y **Biblioteca**, descritas a continuación.

#### Ajustes: Apariencia y Biblioteca

**Apariencia** reúne el nombre de la app, el **logo** (`POST /api/config/logo`, con redimensionado automático en el cliente si supera `LOGO_MAX_FILE_SIZE`) y el **selector de colores** de dos semillas con presets accesibles y modo avanzado (la lógica de derivación de paleta y contraste se detalla en [Despliegue e instalación](#despliegue-e-instalación)). El preview es **no destructivo**: aplica los colores a `document.documentElement` en vivo, pero los restaura a los guardados si se abandona la pestaña sin confirmar. Guardar hace `PUT /api/config` con el branding y re-tematiza toda la app al invalidar la config pública.

**Biblioteca** gestiona las **rutas de escaneo** (`POST`/`DELETE /api/config/scan-paths`, con escaneo inicial automático de la carpeta nueva), el **intervalo de escaneo** (30–86 400 s, aplicado en caliente; ver [Gestión de la biblioteca musical](#gestión-de-la-biblioteca-musical)) y dos interruptores de enriquecimiento: **autocompletar metadatos** (géneros vía Last.fm) y **autocompletar carátulas/fotos**. **Decisión de diseño: cada interruptor solo se habilita si la integración está activa en el `.env`** (`LASTFM_API_KEY` presente, `COVER_ART_ENABLED=true`). Si no lo está, la UI deshabilita el toggle y explica qué variable hay que tocar, dejando claro que es configuración de instalación —no de runtime— y evitando prometer un enriquecimiento que el servidor no puede hacer.

#### Zona de peligro: reset del servidor

La pestaña Resumen aloja una **Zona de peligro** con el **reset total** del servidor (`POST /api/config/reset`; el efecto se detalla en [Despliegue e instalación](#despliegue-e-instalación)). Para evitar un borrado accidental, el botón **exige teclear literalmente `RESET`** en un campo de confirmación antes de habilitarse; al completarse, el cliente cierra sesión y vuelve a `/login`, coherente con que el reset vacía la BBDD y reseedea el admin desde el `.env`.

```mermaid
flowchart TD
    Guard{ProtectedRoute<br/>requireAdmin}
    Guard -- sin sesión --> Login([/login])
    Guard -- rol no admin --> HomeR([/home])
    Guard -- admin --> Shell[AdminShell<br/>navegación superior fija]
    Shell --> Resumen[Resumen /admin<br/>recuentos + re-escaneo<br/>POST /api/config/scan/rescan]
    Shell --> Usuarios[Usuarios /admin/users<br/>alta · rol · pass · borrado]
    Shell --> Canciones[Canciones /admin/songs<br/>catálogo · subida · edición]
    Shell --> Artistas[Artistas /admin/artists<br/>editar · separar · juntar]
    Shell --> Ajustes[Ajustes /admin/settings]
    Ajustes --> Apariencia[Apariencia<br/>nombre · logo · colores<br/>PUT /api/config · POST /logo]
    Ajustes --> Biblioteca[Biblioteca<br/>rutas · intervalo · toggles<br/>scan-paths · PUT /api/config]
    Resumen --> Reset[Zona de peligro<br/>POST /api/config/reset<br/>confirmar escribiendo RESET]
```

### Conteo de escuchas derivado de la base de datos

Para crear el feed del usuario con sus recomendaciones, he decidido basarme en las escuchas del usuario para canciones, géneros y artistas en mi algoritmo.

No existe ningún contador numérico de escuchas en las entidades. Los campos
`Song.listeners`, `Album.listeners` y `Artist.listeners` se eliminaron: toda la
popularidad (de canciones, álbumes, artistas **y** géneros) se **deriva por
consulta** a partir de la tabla de eventos `user_song_listen`, la misma fuente
que ya alimentaba las recomendaciones por usuario.

**Decisión de diseño: derivar en vez de duplicar tablas de evento.** Una
escucha de canción ya implica una escucha de su álbum, de cada uno de sus
artistas y de su género. En lugar de mantener contadores incrementales
(propensos a desincronizarse) o tablas de evento separadas por entidad
(redundantes, porque toda la información está en el evento de canción), se
cuenta sobre `user_song_listen` con consultas JPA agrupadas:

| Conteo | Consulta (en `UserSongListenRepository`) |
|---|---|
| Por canción | `countBySong_Id` / `countListensGroupedBySong` (mapa id→escuchas) |
| Por álbum | `countByAlbumId` (`where e.song.album.id = :albumId`) |
| Por artista | `countByArtistId` (`join e.song s join s.artists a`) |
| Por género | `countByGenre` (`where e.song.genre = :genre`) |
| Top artistas global | `findArtistsByGlobalListensDesc` (`group by a order by count(e) desc`) |
| Top canciones de un género/artista | `findSongsByGenreOrderByGlobalListensDesc` / `findSongsByArtistOrderByGlobalListensDesc` |

Ventajas: no hay que mantener nada al hacer streaming (basta registrar el
evento), no hay riesgo de contadores desincronizados, el límite FIFO de 1000
escuchas por usuario acota el coste de las consultas, y el mismo modelo sirve
para popularidad global y para historial por usuario. El precio es contar en
lectura; para los listados (`GET /api/songs`) se usa una única consulta
agrupada (`countListensGroupedBySong`) y un mapa id→escuchas, evitando el N+1.

#### Registro de escuchas por usuario

La tabla cruzada `user_song_listen` (entidad `UserSongListen`, con `@ManyToOne`
a `User` y a `Song`) registra, fila a fila, qué usuario escuchó qué canción y
cuándo. Es la **única fuente** de los conteos.

El registro se dispara en `StreamingController` junto al `registerGenreListen`,
llamando a `UserSongListenService.recordListen(userId, songId)`. Al hacer
streaming **ya no se incrementa ningún contador numérico** (esos métodos y sus
`@Query`/`@Modifying` desaparecieron): el único efecto sobre el conteo es
insertar la fila del evento. La decisión es **registrar la escucha una sola vez
por reproducción**, en la **petición inicial** de `/api/listen/{id}` (la que no
trae cabecera `Range`, o la que pide un rango desde el byte 0). Las peticiones
de rango posteriores —que el reproductor genera al hacer *seek* dentro de la
canción— **no** insertan filas: así un *seek* no infla los conteos ni bloquea el
streaming con escrituras síncronas a la base de datos antes de enviar los bytes.

Para que la tabla no crezca sin control, se acota a **1000 registros por
usuario** con descarte **FIFO**: tras insertar, `recordListen` cuenta las filas
del usuario y, si superan 1000, borra las más antiguas hasta volver al límite
(constante `MAX_ESCUCHAS`, fija en el servicio igual que `MAX_GENEROS` en
`UserFeed` — es un límite de diseño, no configuración por instalación). 1000
escuchas recientes son suficientes para alimentar las recomendaciones y evitan
que el histórico se dispare con muchos usuarios o reproducciones largas. La FK
`song_id` obliga además a vaciar esta tabla antes de borrar canciones, tanto en
el borrado individual (`SongService.delete`) como en el reset
(`ResetService.resetAll`).

```mermaid
flowchart TD
    IssueToken([Cliente pide stream token<br/>POST /api/listen/token<br/>Authorization: Bearer JWT]) --> Token[StreamTokenService<br/>.issue — UUID ligado al usuario]
    Token --> Stream([GET /api/listen/id?st=UUID])
    Stream --> Validate{¿token válido?}
    Validate -- no --> Reject([401 Unauthorized])
    Validate -- sí --> Record[UserSongListenService<br/>.recordListen]
    Record --> Resolve[Resolver usuario<br/>y canción por id]
    Resolve --> Save[Guardar fila en<br/>user_song_listen]
    Save --> Count{¿más de 1000<br/>escuchas del usuario?}
    Count -- no --> End([Fin])
    Count -- sí --> Evict[Borrar las N más<br/>antiguas FIFO]
    Evict --> End
```

### Feed de recomendaciones del home

Cada usuario tiene asociado obligatoriamente un `UserFeed` (relación `@OneToOne` con `cascade = ALL` y `orphanRemoval`, garantizada por un `@PrePersist` que lo crea si falta). El feed almacena la lista de artistas recomendados que el usuario ve al abrir el home.

El endpoint `GET /api/feed` regenera el feed **en cada acceso al home** con
recomendaciones **personalizadas por usuario** (`UserFeedService.regenerateFeedForUser`
→ `recommendArtistsForUser`). 

El feed devuelve:

1. **Cold-start.** Si *el servidor* no tiene ninguna escucha registrada, o si
   *este* usuario no tiene escuchas propias, no hay historial con el que
   personalizar y se devuelven **todos** los artistas del catálogo.
2. **Descubrimientos diarios**: explicado más abajo.
3. **Por géneros recientes.** Con historial, los 7 huecos personalizados se
   llenan primero con los artistas **más escuchados globalmente dentro de los
   géneros que el usuario ha escuchado últimamente** (la pila reciente
   `last20GenresListened`, cabeza = más reciente, vía
   `findArtistsByGenreOrderByGlobalListensDesc`).
4. **Relleno afín del catálogo.** Si aún quedan huecos, se amplían con más
   artistas de esos mismos géneros según el catálogo (`findArtistsByGenre`),
   aunque todavía no tengan escuchas, para no reducir el feed al único artista ya
   escuchado.
5. **Relleno por popularidad global.** Si todavía faltan, se completan con la
   popularidad global (`findArtistsByGlobalListensDesc`).
6. **3 aleatorios + relleno final.** Se añaden siempre 3 artistas aleatorios del
   catálogo (sin repetir) y, si con todo no se llega a 10 (catálogo pequeño), se
   rellena de nuevo con popularidad global hasta donde se pueda.

La lista resultante (máx. 10, sin repetidos) sobrescribe los artistas
recomendados del feed. La pila de géneros escuchados (`last20GenresListened`) es
historial del usuario y **no** se vacía al regenerar.

#### Flujo de regeneración del feed

```mermaid
flowchart TD
    Home([Usuario abre el home]) --> Get[GET /api/feed]
    Get --> Auth[Resolver usuario autenticado<br/>desde el SecurityContext]
    Auth --> Regen[regenerateFeedForUser]
    Regen --> Cold{¿Servidor o usuario<br/>sin escuchas?}
    Cold -- sí --> All[Cold-start:<br/>todos los artistas<br/>del catálogo]
    Cold -- no --> Genres[Llenar 7 huecos:<br/>artistas top por géneros<br/>recientes del usuario]
    Genres --> Akin{¿quedan huecos?}
    Akin -- sí --> Catalog[Ampliar con artistas<br/>afines del catálogo<br/>+ popularidad global]
    Akin -- no --> Random
    Catalog --> Random[Añadir SIEMPRE<br/>3 artistas aleatorios]
    Random --> Fill{¿llega a 10?}
    Fill -- no --> Pad[Rellenar con<br/>popularidad global]
    Fill -- sí --> Has
    Pad --> Has{¿El usuario<br/>ya tiene feed?}
    All --> Has
    Has -- no --> Save[Guardar feed nuevo<br/>y asociarlo al usuario]
    Has -- sí --> Over[Sobrescribir artistas<br/>recomendados]
    Save --> DTO[Mapear artistas a ArtistDTO]
    Over --> DTO
    DTO --> Render([Cliente renderiza<br/>los artistas recomendados])
```

### Carátulas de playlist

Las playlists pueden tener una imagen de portada cuadrada que solo el creador puede subir o cambiar, tanto al crear la playlist como al editarla más tarde.

**Subida por endpoint separado (`POST /api/playlists/{id}/cover`).** El payload JSON de crear/editar playlist (`PlaylistInput`) no incluye la imagen: el campo `pictureUrl` solo está en el DTO de lectura (`PlaylistDTO`). La carátula viaja como `multipart/form-data` por su propio endpoint. Esto evita convertir todos los endpoints de playlist a multipart y mantiene la API limpia; el único efecto observable es que en la creación el frontend hace dos peticiones consecutivas (crear → subir carátula si la hay). Si la segunda falla, la playlist queda sin carátula, estado válido y recuperable editando.

**Recorte al cuadrado en el servidor con `ImageIO`.** Si la imagen subida no es cuadrada, el backend la recorta por el centro a `min(w, h) × min(h, w)` usando `ImageIO` del JDK, sin dependencias extra. La imagen resultante se guarda siempre como JPEG. El frontend muestra un preview con `object-fit: cover` en un contenedor cuadrado, que refleja visualmente el recorte que aplicará el servidor — el usuario ve el resultado final antes de confirmar.

Alternativa descartada: recorte en el cliente con Canvas antes de subir. Añade complejidad al frontend (exportar Blob, gestionar URLs efímeras de `URL.createObjectURL`) sin ninguna ventaja real, ya que el servidor garantiza el resultado correcto independientemente del cliente.

**Almacenamiento en `assets/playlist-covers/`, mismo patrón que las carátulas de canciones.** El archivo se nombra con el SHA-256 del original — igual que hace `EmbeddedCoverExtractor` — lo que hace la operación idempotente (subir la misma imagen dos veces no crea duplicados). Se sirve mediante el handler estático `/assets/**` ya configurado en `WebMvcConfig`, sin ningún cambio de infraestructura.

### Playlists compartidas (colaboración vía magic link)

Una playlist deja de ser estrictamente individual: su creador puede **invitar a otros usuarios a colaborar** generando un *magic link* de un solo uso. Quien canjea el enlace queda añadido como **colaborador** y, a partir de ahí, propietario y colaboradores comparten la edición del contenido, pero **no** la del continente.

**Reparto de permisos: el continente es del dueño, el contenido es compartido.** Propietario y colaboradores pueden **añadir y quitar canciones** (`POST`/`DELETE /api/playlists/{id}/songs/{songId}`). En cambio, solo el **propietario** puede editar los metadatos de la playlist —nombre, descripción, visibilidad y carátula (`PUT /api/playlists/{id}`, `POST /api/playlists/{id}/cover`)— y **borrarla** (`DELETE /api/playlists/{id}`, que además puede hacer un admin). Un colaborador con acceso a una playlist privada puede verla (`GET /api/playlists/{id}`) aunque no sea pública.

**Decisión de diseño: tabla cruzada explícita (`PlaylistCollaborator`) en lugar de `@ManyToMany` en `Playlist`.** Igual que con `UserFollow`, modelar el vínculo como entidad propia con `playlist`, `user` y `createdAt` (unique key `(playlist_id, user_id)`) evita hidratar la lista entera de colaboradores al leer una playlist, deja hueco para metadatos (cuándo se unió) y permite un borrado controlado: al eliminar una playlist se limpian sus colaboradores y tokens antes de borrar la fila para no chocar con la FK (`PlaylistSharingService.deleteSharingData`). La relación `Playlist ↔ Song` se mantiene como `@ManyToMany` porque ahí no se necesita ningún metadato por arista.

**Decisión de diseño: magic link de un solo uso, sin caducidad temporal.** El token (`PlaylistShareToken`) es un valor aleatorio no adivinable generado con `SecureRandom` (32 bytes, Base64 URL-safe). El "un solo uso" se garantiza **eliminando la fila al canjearla**, no con una flag `used`: una vez consumido, el mismo enlace responde `404`. No se añade caducidad por tiempo (no hay variable de configuración nueva); mientras el token no se canjee, sigue siendo válido. El creador puede generar varios enlaces para una misma playlist (uno por persona a invitar).

**Decisión de diseño: el que canjea se resuelve del `SecurityContext`, nunca del path.** `POST /api/playlists/share/{token}` solo lleva el token; el colaborador a añadir es siempre el usuario autenticado. Canjear un enlace de **tu propia** playlist responde `409 Conflict` (ya eres el dueño). El canje es idempotente respecto al colaborador (si ya lo eras no se duplica la fila), pero el token siempre se consume.

#### Flujo de compartir y canjear

```mermaid
flowchart TD
    Gen([Propietario pulsa Compartir]) --> Share[POST /api/playlists/id/share]
    Share --> OwnerChk{¿Es el creador?}
    OwnerChk -- no --> Err403[403 Forbidden]
    OwnerChk -- sí --> Token[Generar PlaylistShareToken<br/>SecureRandom + Base64]
    Token --> Link([200 OK ShareLinkResponse<br/>token + shareUrl])
    Link -. comparte enlace .-> Redeem[POST /api/playlists/share/token]
    Redeem --> Find{¿Token existe?}
    Find -- no --> Err404[404 Not Found<br/>enlace inválido o ya usado]
    Find -- sí --> Self{¿Soy el propietario?}
    Self -- sí --> Err409[409 Conflict]
    Self -- no --> Exists{¿Ya soy colaborador?}
    Exists -- sí --> Consume[Consumir token: borrar fila]
    Exists -- no --> Insert[Insertar PlaylistCollaborator<br/>createdAt = now]
    Insert --> Consume
    Consume --> Resp([200 OK PlaylistDTO<br/>con collaboratorIds])
```

#### Apertura en la app móvil (deep link `selfpotify://`)

Un enlace de invitación compartido (`<servidor>/playlist/share/{token}`) sigue
siendo una **URL web normal** —para que cualquiera pueda abrirlo en un navegador—,
pero cuando se abre desde un **móvil con la app instalada** queremos que el canje
ocurra dentro de la app nativa, no en el navegador.

**Decisión de diseño: esquema propio `selfpotify://`, no App Links verificadas.**
Las App Links (apertura automática sin diálogo) exigirían publicar un
`assetlinks.json` (Digital Asset Links) en el dominio de cada servidor. Como
Selfpotify es **self-hosted** —el host/puerto del servidor es arbitrario y
desconocido en tiempo de compilación— eso es inviable. Se usa por tanto un
**esquema de URI propio**: `selfpotify://playlist/share/{token}` (host `playlist`,
path `/share/{token}`, paralelo a la ruta web). La app Android lo registra con un
`intent-filter` en `MainActivity` (`launchMode="singleTask"`, para recibir el
intent en `onNewIntent` cuando ya está abierta).

**El que detecta el móvil y hace el puente es la página web, no el enlace.** El
enlace compartido NO cambia de formato (sigue siendo http(s), con fallback web
intacto). Es la página `/playlist/share/{token}` la que, al cargar, detecta el
dispositivo y decide cómo abrir la app, distinguiendo plataforma:

- **Android** → redirige a una URL `intent:` de Chrome/Samsung/Firefox con
  `browser_fallback_url`:
  `intent://playlist/share/{token}#Intent;scheme=selfpotify;package=davila.anton.selfpotify;S.browser_fallback_url=<…/mobile?origin=playlist-share>;end`.
  Es el **propio sistema** quien decide: si la app está instalada la abre; si no,
  navega automáticamente al `browser_fallback_url`. No hace falta heurística de
  temporizador: no hay forma fiable de "preguntar" desde el navegador si la app
  está instalada, así que se delega la decisión al SO vía `intent:`. El fallback
  apunta a la pantalla de bienvenida `/mobile?origin=playlist-share`, que muestra
  un copy de invitación específico (ver "Redirección a la app móvil").
- **iOS / otros móviles** → intenta el esquema propio
  `selfpotify://playlist/share/{token}` y, si no hay handoff dentro de una ventana
  corta (`DEEP_LINK_FALLBACK_MS`, la pestaña no se oculta → la app no está
  instalada), **cae al canje web**.
- **Escritorio** → no se intenta el deep link: se canjea en web como hasta ahora.

**Respaldo manual: botón "Ya tengo la app".** El intento automático al cargar la
página no siempre basta: algunos navegadores **bloquean lanzar un esquema propio
sin un gesto del usuario**. Por eso, mientras se intenta el handoff, la página
muestra en móvil un botón **"Ya tengo la app"** —un `<a href>` real con el mismo
deep link (`intent:` en Android, `selfpotify://` en iOS/otros)—; al ser una
interacción explícita es más fiable que el redirect automático y sirve de salida
si este no salta. Limitación conocida: dentro de **navegadores embebidos**
(WhatsApp, Telegram, Instagram… que usan un `WebView`) ni el automático ni el
botón funcionan, porque esos `WebView` no resuelven esquemas propios ni `intent:`;
ahí la única vía es abrir el enlace en un navegador real (Chrome/Firefox).

> **Nota: la página vive fuera del grupo protegido `(app)`.** `/playlist/share/{token}`
> es una ruta de **nivel superior** (`front/app/playlist/share/[token]/`), no bajo
> `(app)`, precisamente para que el puente al deep link se ejecute **aunque el
> visitante no tenga sesión web** —el caso típico de quien recibe la invitación en
> el móvil—. Si estuviera bajo `ProtectedRoute`, un usuario sin sesión sería
> redirigido a `/login` (y el middleware lo mandaría a `/mobile`) **antes** de poder
> intentar abrir la app. Solo el *fallback* de canje web requiere sesión; sin ella
> redirige a `/login`. Además es la **única ruta exenta** del middleware que
> redirige los móviles a `/mobile` (ver "Redirección a la app móvil"), para poder
> **cargarse** en el móvil y ejecutar el handoff.

**Qué hace la app al recibir el deep link.** `MainActivity` extrae el `token` del
URI y canjea el enlace contra el servidor configurado
(`POST /api/playlists/share/{token}`, mismo endpoint que la web), añade al usuario
como colaborador y navega al detalle de la playlist. Si no hay sesión activa, el
canje queda pendiente hasta completar el login y se ejecuta a continuación. El
token es server-relativo: la app lo canjea contra **su** servidor configurado
(coherente con el modelo single-server de cada instalación).

### Descubrimientos diarios

Junto al feed de artistas, el home ofrece una sección de **descubrimientos
diarios**: el endpoint `GET /api/feed/daily-discoveries` devuelve **9 canciones**
(`SongDTO`) pensadas para que el cliente las muestre en un deslizable horizontal.
La lista se compone de tres bloques de tres canciones cada uno
(`DailyDiscoveryService`):

1. **3 aleatorias** del catálogo disponible.
2. **3 no escuchadas** del **último género** que el usuario ha estado escuchando
   (la cabeza de su pila `last20GenresListened`). Si ese género no tiene
   suficientes canciones nuevas, se recorre la pila hacia atrás (al siguiente
   género más reciente) hasta reunir tres.
3. **3 de un género que el usuario no escucha**: un género presente en el
   catálogo pero ausente de su historial de escuchas, elegido al azar entre los
   candidatos. Si el usuario ya escucha todos los géneros disponibles, se cae al
   **género más antiguo de su pila** (último elemento de `last20GenresListened`).

**Decisión de diseño: estable por día, sin persistencia.** Aunque el bloque 1 es
"aleatorio", la sección se llama *diaria* porque toda la aleatoriedad (el muestreo
de cada bloque, la elección del género desconocido y el barajado final) usa un
único generador sembrado con `userId + fecha`, y las consultas devuelven IDs
ordenados por id como base determinista. Así, todas las llamadas del mismo usuario
durante el mismo día devuelven **exactamente la misma lista**, que cambia a
medianoche (estilo "Daily Mix"). No se introduce ninguna entidad ni columna nueva:
el resultado se **recalcula** en cada petición de forma determinista, igual que el
feed de artistas se regenera en cada acceso. Las 9 canciones se devuelven
**mezcladas**, de modo que los tres bloques no se distinguen en el orden final. Si
el catálogo es demasiado pequeño para llenar los tres bloques sin repetir, se
completa con canciones aleatorias hasta llegar a 9 (o menos, si no hay más).

**Scroll infinito.** El carrusel de descubrimientos diarios es desplazable de forma
ilimitada. Cuando el usuario llega a las dos últimas canciones cargadas, el cliente
llama a `GET /api/songs/random?count=10` para obtener 10 canciones totalmente
aleatorias (sin semilla, distintas en cada llamada) y las añade al final del
carrusel. Mientras se carga el siguiente lote se muestra un spinner de espera.
Esto combina la lista diaria estable (personalizada y determinista) con la
posibilidad de explorar el catálogo sin límite desde la misma pantalla.

```mermaid
flowchart TD
    Get([GET /api/feed/daily-discoveries]) --> Seed[Sembrar Random<br/>con userId + fecha]
    Seed --> B1[Bloque 1: 3 aleatorias<br/>del catálogo]
    B1 --> B2{Bloque 2: 3 no escuchadas<br/>del último género de la pila}
    B2 -- pocas --> B2b[Caer al siguiente<br/>género de la pila]
    B2b --> B2
    B2 -- 3 reunidas --> B3{Bloque 3: 3 de un género<br/>que el usuario NO escucha}
    B3 -- no hay candidato --> B3b[Fallback: género más<br/>antiguo de la pila]
    B3b --> Fill
    B3 -- elegido --> Fill{¿llega a 9?}
    Fill -- no --> Pad[Completar con<br/>aleatorias sin repetir]
    Fill -- sí --> Shuffle[Barajar las 9<br/>con el Random sembrado]
    Pad --> Shuffle
    Shuffle --> Map[Mapear a SongDTO<br/>con escuchas derivadas]
    Map --> Render([Cliente renderiza<br/>el deslizable de descubrimientos])
    Render -- usuario llega al penúltimo slide --> Random([GET /api/songs/random?count=10])
    Random --> Append[Añadir al final del carrusel]
    Append --> Render
```

### Búsqueda global

Un único endpoint, `GET /api/search`, cubre canciones, artistas, álbumes,
playlists, usuarios y géneros con la misma forma de respuesta. Es el cimiento
de cualquier barra de búsqueda que monten los clientes.

**Decisión de diseño: un solo endpoint, dos modos.** En lugar de exponer una
ruta por entidad (`/api/songs/search`, `/api/artists/search`…) el backend
ofrece un único endpoint con un parámetro `type`. En modo `all` (default)
devuelve hasta 5 elementos por categoría, pensado para una vista previa
multi-categoría. En modo específico (`type=songs|artists|albums|playlists|users|genres`)
devuelve solo esa categoría paginada (`page`/`size`). La forma de la respuesta
es la misma en ambos casos (`SearchResponseDTO` con un slice por categoría);
las categorías no usadas se omiten del JSON.

**Decisión de diseño: normalización en aplicación, no en SQL.** Para que la
búsqueda sea insensible a mayúsculas, acentos y signos diacríticos —
`"rosalia"` debe encontrar `"Rosalía"` y viceversa — tanto la consulta como el
texto buscable se pasan por la misma rutina: `Normalizer.Form.NFD` + strip de
`\p{InCombiningDiacriticalMarks}+` + `toLowerCase(Locale.ROOT)` + colapso de
espacios. Esto se hace en Java, no en SQL, porque H2 (desarrollo) y MariaDB
(producción) no comparten sintaxis para desdiacritizar y mantener una única
rutina compartida garantiza que la query y los haystacks acaben exactamente en
la misma forma canónica. La query normalizada se tokeniza por espacios y se
exige que **todos** los tokens estén presentes en el haystack (estilo barra de
YouTube/Spotify: `"stairway heaven"` empareja con `"Stairway to Heaven"`
aunque `"to"` no esté en la consulta).

**Decisión de diseño: filtrado en memoria, no índice invertido.** El servicio
carga la lista completa de cada repositorio (`findAll`) y filtra en memoria.
Es una elección consciente para esta versión: selfpotify está pensado como
servidor personal con catálogos acotados, así que cargar las pocas miles de
filas que cualquier instalación realista va a tener cuesta menos que mantener
un índice o atarse a particularidades del motor SQL. El contrato del endpoint
no expone esta decisión, así que se puede sustituir por Lucene/PostgreSQL
full-text en el futuro sin tocar a los clientes si llegado el caso hace falta.
Para evitar el N+1 al exponer el conteo de escuchas de las canciones se
reutiliza la consulta agrupada de `SongService.getListenCountsBySong()` (la
misma que ya usan los listados generales).

**Decisión de diseño: scoring de relevancia simple, predecible.** El orden de
los resultados sigue una jerarquía explícita sobre el campo principal de cada
categoría (título de canción, nombre de artista/álbum/playlist/género,
username): `0` = exacto · `1` = empieza por la consulta · `2` = alguna palabra
empieza por el primer token · `3` = subcadena. Los empates se rompen con una
métrica natural por categoría (escuchas desc para canciones, nº de canciones
desc para artistas/álbumes/playlists/géneros, orden alfabético para usuarios).
No hay tf-idf ni boosting cruzado: el comportamiento debe poder explicarse en
una frase para que un usuario que escribe `"rock"` entienda por qué la canción
titulada exactamente "Rock" aparece antes que "Bohemian Rhapsody (Rock)".

**Decisión de diseño: visibilidad de playlists igual que en el resto de la
app.** La búsqueda nunca devuelve playlists privadas ajenas. Solo aparecen las
**públicas** y las **propias** del usuario autenticado, replicando exactamente
la regla que ya aplican `GET /api/playlists/{id}` y `GET /api/playlists/user/{userId}`,
para que la búsqueda no sea un canal lateral de fuga. Para el resto de
entidades no hay nada que ocultar: canciones, artistas, álbumes, géneros y
usuarios son visibles para cualquier sesión autenticada.

#### Flujo de una búsqueda

```mermaid
flowchart TD
    Call([GET /api/search?q=...&type=...]) --> Norm[SearchService.normalize:<br/>NFD + strip diacríticos<br/>+ lowercase + colapsar espacios]
    Norm --> Tokens[Tokenizar por espacios]
    Tokens --> Mode{¿type?}
    Mode -- all --> LoadAll[Cargar findAll de las 6<br/>categorías + listen counts]
    Mode -- categoría única --> LoadOne[Cargar findAll de esa<br/>categoría + listen counts si aplica]
    LoadAll --> Filter[Para cada entidad:<br/>matchesAll tokens vs haystack]
    LoadOne --> Filter
    Filter --> Vis{¿playlist privada<br/>ajena?}
    Vis -- sí --> Drop[Descartar]
    Vis -- no --> Score[Score 0/1/2/3 sobre<br/>el campo principal]
    Score --> Sort[Ordenar por score asc<br/>+ tiebreaker por categoría]
    Sort --> Slice{¿type?}
    Slice -- all --> Top5[Recortar a 5/categoría]
    Slice -- categoría única --> Page[Recortar a page/size]
    Top5 --> Resp([SearchResponseDTO con<br/>6 CategoryPage rellenas])
    Page --> Resp2([SearchResponseDTO con<br/>1 CategoryPage rellena])
```

### Perfil de usuario (nombre visible + foto)

Además del `username` —identificador único e inmutable usado para el login— cada usuario tiene asociado un `Profile` con un **nombre visible** (`name`, libre y editable) y una **foto de perfil** (`pictureUrl`). Ambos campos son opcionales: si están vacíos, la UI cae al username y a la inicial.

**Decisión de diseño: editar el perfil propio vive bajo `/api/me/*`, no bajo `/api/users/{id}`.** El controlador `UserController` está reservado a operaciones de administrador (alta de cuentas, cambio de rol, borrado); meter ahí los endpoints "editar mi propio nombre" o "subir mi foto" forzaría guards condicionales por id en cada método. El nuevo `ProfileController` separa los dos casos: `GET /api/me`, `PUT /api/me/profile`, `POST /api/me/profile/picture` y `DELETE /api/me/profile/picture` operan **siempre sobre el usuario autenticado** —el id sale del `SecurityContext`, no del path— y `GET /api/users/{id}/public` devuelve la misma `UserSummaryDTO` que ya usa la búsqueda para que cualquier autenticado pueda abrir el perfil de otro. Así no se cruzan permisos: el admin nunca edita el perfil de otro usuario por error y un usuario corriente nunca tiene que pasar por un endpoint admin.

**Decisión de diseño: la pantalla del propio perfil es la misma que ven los demás; la edición vive en una página aparte.** En el cliente hay tres rutas: `/profile` (mi perfil), `/user/[id]` (perfil de otro) y `/profile/edit` (formulario para tocar nombre y foto). `/profile` y `/user/[id]` montan el **mismo componente** `UserProfileView`, que consume `GET /api/users/{id}/public` + `GET /api/playlists/user/{userId}`; lo único que cambia es un icono de lápiz junto al nombre que se pinta cuando el username del perfil coincide con el del auth store. El menú del topbar pasa de "Editar perfil" a "Ver tu perfil" y enlaza a `/profile`. La ventaja: el dueño ve exactamente lo que va a ver el resto de gente —si su nombre o su avatar quedan raros, lo nota sin tener que abrir un perfil ajeno para comparar—. Y separar la edición evita modos en la vista: la pantalla pública nunca contiene inputs, así que pulsar accidentalmente sobre el avatar no abre un selector de archivo cuando no toca.

**Decisión de diseño: subida del avatar por endpoint multipart separado, mismo patrón que la carátula de playlist.** El `PUT /api/me/profile` es un JSON pequeño (`{ "name": "..." }`) y la foto viaja por su propio endpoint multipart, recortándose al cuadrado en el servidor con `ImageIO` y persistiéndose como `assets/avatars/<sha256>.jpg`. Es la misma decisión que ya tomamos para `POST /api/playlists/{id}/cover`: mantenemos la API JSON limpia y reusamos el handler estático `/assets/**` para servir la imagen sin más infraestructura. El nombrado por SHA-256 hace la operación idempotente —subir dos veces la misma imagen no crea duplicados— y permite que `DELETE /api/me/profile/picture` se limite a poner el campo a `null` sin borrar el fichero físico (podría estar referenciado por otra cuenta que subió la misma imagen).

**Decisión de diseño: buscar también por nombre visible sin penalizar el score.** La búsqueda de usuarios (`/api/search?type=users`) ya incluía `Profile.name` en el haystack —los matches "se notaban"— pero el score se calculaba **solo sobre el username**, así que un usuario con `displayName="María López"` y `username="maria_l"` aparecía peor posicionado al buscar "María" que el usuario `username="maria"`. Ahora el score por usuario es `min(score(username), score(displayName))`: el campo que mejor coincide con la consulta es el que cuenta. El tiebreaker sigue siendo alfabético por username, que es único y siempre está presente.

#### Flujo: ver tu perfil y editarlo

```mermaid
flowchart TD
    Menu([Click en avatar del topbar]) --> View[Cliente navega a /profile]
    View --> Me[GET /api/me<br/>(resolver mi id)]
    Me --> Public[GET /api/users/id/public<br/>+ GET /api/playlists/user/userId]
    Public --> Render[Render UserProfileView:<br/>avatar, nombre, badge admin,<br/>playlists públicas]
    Render --> Owner{¿El username del perfil<br/>coincide con el del auth store?}
    Owner -- sí --> Pencil[Pintar icono de lápiz<br/>junto al nombre]
    Owner -- no --> NoPencil[Sin lápiz: vista pública pura]
    Pencil --> Click{¿Click en el lápiz?}
    Click -- no --> End([Fin])
    Click -- sí --> Edit[Navegar a /profile/edit]
    Edit --> Choice{¿Qué cambia?}
    Choice -- Nombre --> Put[PUT /api/me/profile<br/>body: name]
    Put --> Persist[ProfileController:<br/>crear Profile si no existía<br/>cascade ALL y actualizar name]
    Choice -- Foto nueva --> Upload[POST /api/me/profile/picture<br/>multipart: file]
    Upload --> Crop[Recortar al cuadrado<br/>con ImageIO + SHA-256]
    Crop --> Save[Guardar assets/avatars/sha.jpg<br/>+ persistir pictureUrl]
    Choice -- Quitar foto --> Clear[DELETE /api/me/profile/picture]
    Clear --> Null[ProfileController:<br/>pictureUrl = null]
    Persist --> DTO[Devolver UserSummaryDTO]
    Save --> DTO
    Null --> DTO
    DTO --> Invalidate[React Query invalida key 'me'<br/>y la vista pública]
    Invalidate --> View
```

#### Carátulas y fotos automáticas

Durante el escaneo, el servidor completa de forma **idempotente** (solo si falta) la carátula de cada canción y álbum y la foto de cada artista, gemelo de cómo `GenreApiService` rellena el género. El orden de prioridad es:

1. **Carátula embebida** en el propio archivo `.mp3`/`.wav` (etiqueta ID3/APIC). Si existe, se vuelca a `<assets>/covers/<sha256>.<ext>` y se guarda la ruta `/assets/covers/…` (servida por el mismo handler `/assets/**` que el logo); **no se consulta internet** para esa canción. Sirve también como portada del álbum, al ser la del propio lanzamiento.
2. **Fuentes online sin API key** (links a CDN en la nube), "lo más oficial primero": **Cover Art Archive** vía MusicBrainz (portada canónica del *release*) → **iTunes Search API** (CDN de Apple) → **Deezer**. La foto del artista sale de **Deezer** (`picture_xl`), ya que iTunes no la expone y MusicBrainz no aloja fotografías.
3. Si no se encuentra nada (o el link externo muere), el campo queda **`null`** y el frontend pinta su icono/inicial; no se generan placeholders en el backend.

Para poder rellenar `Album.picture_url`, el escaneo ahora **resuelve o crea el álbum** a partir de la etiqueta `ALBUM` del fichero. Todas las fuentes funcionan sin registrar ninguna clave; MusicBrainz solo exige un `User-Agent` descriptivo (`COVER_ART_USER_AGENT`). La resolución online puede desactivarse con `COVER_ART_ENABLED=false` (la extracción de carátula embebida se mantiene).

### Grafo de seguimiento entre usuarios

Cada usuario puede seguir y ser seguido por otros, formando un **grafo dirigido**: la arista `follower → followed` significa que `follower` ve a `followed` en su lista de "siguiendo". `UserSummaryDTO` incorpora dos contadores derivados (`followersCount`, `followingCount`) y una flag `isFollowedByMe` que indica si el usuario en sesión ya sigue al usuario representado por el DTO.

**Decisión de diseño: tabla cruzada explícita (`UserFollow`) en lugar de `@ManyToMany` en `User`.** Modelar las aristas como una entidad propia con `follower`, `followed` y `createdAt` (con unique key `(follower_id, followed_id)`) sigue el mismo patrón que ya usa `UserSongListen` y aporta tres cosas que un `@ManyToMany(User → User)` no daría:

1. **No se hidratan listas al cargar un usuario.** Si los seguidores vivieran como `Set<User>` en la entidad `User`, leer un perfil arrastraría el set por defecto (o forzaría a tocar el fetch en cada caller). Con la tabla cruzada, los counts se piden por consulta agregada (`countByFollowed_Id`, `countByFollower_Id`) y nunca cargan listas.
2. **Aristas con metadatos**. `createdAt` se rellena en `@PrePersist` y permite ordenar la lista de seguidores por "más recientes primero" sin sacarlo del aire en cada llamada; queda hueco para añadir más metadatos (notificaciones, *muted*, etc.) si hace falta.
3. **Borrado simétrico controlado.** Cuando se borra un usuario hay que limpiar las aristas en las que aparece como `follower` <em>o</em> como `followed`. `UserFollowRepository.deleteAllInvolving(userId)` lo hace con un único `DELETE` JPQL, y tanto `UserService.delete` como `ResetService.resetAll` lo invocan antes de borrar el `User` para no chocar con la FK. Con un `@ManyToMany` en `User` el cascade habría sido posible pero menos predecible (Hibernate no garantiza el orden de borrado de las dos direcciones).

**Decisión de diseño: el path del POST/DELETE solo nombra al *followed*, nunca al follower.** El cliente llama a `POST /api/users/{id}/follow` y el servidor sustituye el `follower` por <strong>el usuario autenticado</strong> resuelto desde el `SecurityContext`. Que un cliente nunca pueda firmar la arista con un follower que no sea él mismo evita por construcción el caso "Alice fuerza a Bob a seguir a Carol". `POST` y `DELETE` son <strong>idempotentes</strong>: seguir a quien ya sigues, o dejar de seguir a quien no sigues, responden 200 con el `UserSummaryDTO` actualizado sin error; el cliente no tiene que mantener estado para distinguir "primer click" del segundo.

**Decisión de diseño: counts y `isFollowedByMe` solo se rellenan en los endpoints de perfil; la búsqueda los manda a 0/null.** El DTO lleva los tres campos siempre (contrato JSON estable), pero solo los endpoints de perfil (`/api/me`, `/api/users/{id}/public`, `/api/users/{id}/follow`, `/followers`, `/following`) los calculan. `SearchService` se mantiene a salvo de un N+1 que duplicaría el coste de cada búsqueda sin un beneficio visible (la UI de búsqueda no pinta esos números). Para los listados de followers/following se evita el N+1 con dos consultas agregadas (`countFollowersGrouped`, `countFollowingGrouped`) y una sola query batch que devuelve el subconjunto de ids ya seguidos por el viewer (`findFollowedIdsByFollowerAmong`).

**Decisión de diseño: en el frontend, los botones de seguir/dejar de seguir por fila viven solo en mis propias listas.** Los contadores son enlaces estilo Spotify a `/user/{id}/followers` y `/user/{id}/following`, accesibles desde cualquier perfil. La página de lista compara `me.id` (de `/api/me`) con el `[id]` de la URL: si coincide, las filas incluyen un botón "Siguiendo / Seguir" que llama a `useFollowUser`/`useUnfollowUser`; si no, las filas son puramente navegables (clic = ir al perfil de esa persona). La razón es no convertir la página en un panel de moderación inverso: si ves a quién sigue otro usuario, no eres tú quien decide a quién quitar de su lista, así que el botón solo aparece cuando estás operando sobre tu propio grafo.

#### Flujo de seguir y dejar de seguir

```mermaid
flowchart TD
    UI([Usuario pulsa botón Seguir/Siguiendo]) --> Mut{¿Estoy siguiendo?}
    Mut -- no --> Post[POST /api/users/id/follow]
    Mut -- sí --> Delete[DELETE /api/users/id/follow]
    Post --> Resolve[Resolver follower del SecurityContext]
    Delete --> Resolve
    Resolve --> Check{¿follower == followed?}
    Check -- sí --> Err400[400 Bad Request<br/>No te puedes seguir a ti mismo]
    Check -- no --> Op{Operación}
    Op -- follow --> Exists{¿Existe arista?}
    Exists -- sí --> NoOp[No-op: no insertar]
    Exists -- no --> Insert[Insertar UserFollow<br/>createdAt = now]
    Op -- unfollow --> Find{¿Existe arista?}
    Find -- sí --> Drop[Borrar UserFollow]
    Find -- no --> NoOp2[No-op]
    Insert --> Enrich[Recalcular counts<br/>+ isFollowedByMe del target]
    Drop --> Enrich
    NoOp --> Enrich
    NoOp2 --> Enrich
    Enrich --> Resp([200 OK UserSummaryDTO actualizado])
    Resp --> Invalidate[React Query invalida:<br/>publicProfile target, me,<br/>followers/following de ambos]
    Invalidate --> ReRender([UI re-pinta contador + botón])
```

### Redirección a la app móvil

El frontend web **no es responsive**: está pensado para escritorio. Para no
mostrar una experiencia rota en móviles, un **middleware de Next.js**
(`front/middleware.ts`) detecta el dispositivo a partir del `User-Agent` y
redirige cualquier acceso desde un móvil a `/mobile`, una pantalla simple que
invita a **descargar la app nativa** desde las
[releases oficiales de GitHub](https://github.com/conguchu/selfpotify/releases).

La única ruta exenta es `/playlist/share/*`: debe **cargarse** también en móvil
para hacer el handoff a la app (vía `intent:` en Android o `selfpotify://` en
iOS/otros) y, si la app no está instalada, decidir su fallback (ver "Apertura en
la app móvil"). Desde escritorio, `/mobile` redirige a `/home`. El middleware
ignora los assets estáticos y las rutas internas de Next.js mediante su `matcher`.

La pantalla `/mobile` adapta su texto según el parámetro `origin`: con
`?origin=playlist-share` —el `browser_fallback_url` que usa el `intent:` de Android
cuando la app **no** está instalada— muestra un copy de invitación ("Te han
invitado a colaborar en una playlist, ¿te lo vas a perder? Instálate la app y
regístrate en el servidor `<url del servidor>`", con la URL reconstruida en el
servidor desde las cabeceras de la petición) en vez del copy genérico de descarga,
**manteniendo el botón de descarga** en ambos casos.

```mermaid
flowchart TD
    Req([Petición a cualquier ruta]) --> UA{¿User-Agent móvil?}
    UA -- no --> Mobile1{¿Ruta == /mobile?}
    Mobile1 -- sí --> Home[Redirige a /home]
    Mobile1 -- no --> Pass1([Continúa normal])
    UA -- sí --> Share{¿Ruta == /playlist/share/*?}
    Share -- sí --> Pass2([Continúa normal<br/>la página hace el puente a la app:<br/>intent:// Android · selfpotify:// iOS])
    Share -- no --> Mob{¿Ruta == /mobile?}
    Mob -- sí --> Pass3([Muestra pantalla móvil])
    Mob -- no --> Redir[Redirige a /mobile]
    Redir --> Pass3
```

**Visión de conjunto: cómo se atiende a un cliente de teléfono.** El siguiente
diagrama resume todas las vías por las que un móvil llega a contenido de
Selfpotify y dónde acaba. Las flechas discontinuas marcan el **handoff a la app**
(ver "Apertura en la app móvil"), distinto según plataforma: en Android vía
`intent:` (el SO abre la app o cae al `browser_fallback_url` `/mobile`), y en
iOS/otros vía `selfpotify://` con fallback por temporizador al canje web.

```mermaid
flowchart TD
    subgraph Entradas
        A[Usuario abre la app nativa Android]
        B["Usuario abre una URL web<br/>(cualquier ruta del front)"]
        C["Usuario abre un enlace de share<br/>&lt;servidor&gt;/playlist/share/token"]
    end

    A --> AppNative([App Kotlin: consume la API :8080 directamente])

    B --> MW{Middleware Next.js<br/>¿UA móvil?}
    MW -- no, escritorio --> Web([App web normal])
    MW -- sí, móvil --> MobPage([Pantalla /mobile<br/>«Descarga la app» + releases GitHub])

    C --> MW2{Middleware:<br/>/playlist/share exento}
    MW2 --> SharePage["Página /playlist/share/token se CARGA<br/>aunque sea móvil · fuera del grupo protegido"]
    SharePage --> Plat{¿Qué dispositivo?}

    Plat -- escritorio --> Redeem[Canje web: POST /api/playlists/share/token]
    Redeem --> PL([Redirige a /playlist/id])

    Plat -- Android --> Intent[/"intent://…;scheme=selfpotify;<br/>browser_fallback_url=/mobile?origin=playlist-share"/]
    Intent -. app instalada .-> AppRedeem([MainActivity canjea y abre la playlist])
    Intent -. app NO instalada .-> MobInvite([/mobile?origin=playlist-share<br/>copy de invitación + descarga])

    Plat -- iOS/otros --> Deep[/"selfpotify://playlist/share/token"/]
    Deep -. app instalada .-> AppRedeem
    Deep -. app NO instalada timeout .-> Redeem
```

---

## Android

El cliente Android es una aplicación **nativa en Kotlin** que vive en el directorio `android/` del monorepo y consume la API de Spring **directamente** (`:8080`, sin pasar por Nginx; ver "Empaquetado y arranque con Docker"). Es el primero de los clientes "no web" previstos por la arquitectura de microservicios.

### Arquitectura

La app sigue **MVVM estricto**, con responsabilidades separadas en capas y sin saltos entre ellas:

- **`data/model`** — DTOs Kotlin puros que reflejan la forma real de la API (`PublicConfig`, `JwtResponse`, …), tomada de `API-doc.md` y de los controllers.
- **`data/network`** — interfaz Retrofit (`SelfpotifyApi`) y un `ApiProvider` que **reconstruye el cliente Retrofit cuando cambia el servidor**, ya que la URL base se decide en tiempo de ejecución y no está fijada en compilación.
- **`data/local`** — `SessionStore` sobre **DataStore Preferences**: persiste la dirección del servidor, el JWT, el servidor emisor del JWT, el nombre de usuario y la marca del servidor —paleta de colores y ruta del logo— (ver "Branding dinámico del servidor: colores y logo").
- **`data/repository`** — `AuthRepository` es la **única fuente de verdad**: combina red y persistencia y expone `Result<T>` para propagar errores sin lanzar excepciones a la UI.
- **`ui/<feature>`** — una carpeta por pantalla o flujo (`server/`, `auth/`, `main/`, `discover/`, `search/`, `library/`, `profile/`, `follow/`, `detail/`, `player/`, `offline/`), cada una con su `Screen` composable + `ViewModel`. Los ViewModels exponen el estado como `StateFlow` y los eventos de navegación como `SharedFlow`; **nunca** referencian la UI. Junto a las features hay dos carpetas de apoyo: `common/` (composables reutilizables como `ServerLogo`) y `theme/` (el `SelfpotifyTheme` y el `ThemeViewModel` del branding dinámico). La reproducción de audio vive aparte —fuera de `ui/`— en `playback/` (`PlaybackService` + `PlaybackConnection`).

El stack es **Jetpack Compose + Navigation Compose** (una sola `ComponentActivity` que aloja un `NavHost` con los destinos de la app), corrutinas y `StateFlow`. Para red se usa Retrofit + Gson sobre OkHttp.

El look & feel sigue la estética **Spotify (oscuro)**, pero **el branding es dinámico**: tanto la paleta de colores como el **logo** se obtienen del servidor vía `GET /api/config/public`. Lo que define el cliente son solo valores de **fallback de carga** —los colores neutros (fondo `#121212`, acento `#1DB954`, texto blanco) y el logo de Selfpotify empaquetado en `res/drawable`—, nunca el branding real de la instalación: en cuanto la app conoce un servidor adopta sus colores y muestra su logo en lugar del de Selfpotify.

### Branding dinámico del servidor: colores y logo

Cada instalación define su propio branding, así que la app **adopta tanto la paleta como el logo del servidor al que se conecta** en lugar de traer recursos fijos. El logo local de Selfpotify (`res/drawable/logo_selfpotify.png`) queda relegado a **fallback de carga**, igual que los colores neutros. El ciclo es:

1. **Origen.** El servidor expone su branding en `GET /api/config/public`:
   - **Colores:** `branding.colors`, un mapa de tokens CSS (`--color-bg`, `--color-bg-card`, `--color-bg-hover`, `--color-border`, `--color-text`, `--color-text-muted`, `--color-accent`, `--color-accent-hover`, `--color-danger`, …). El color de texto sobre el acento no lo envía el servidor: se calcula en el cliente (negro o blanco según la luminancia del acento, contraste WCAG).
   - **Logo:** `branding.logoUrl`, una ruta relativa al asset subido por el administrador (p. ej. `/assets/logo.png`, servido por `/assets/**`). Puede ser `null` si la instalación no ha subido logo.

2. **Captura.** El branding se obtiene de la misma llamada que ya valida el servidor en la pantalla 1, de modo que la app adopta colores y logo **antes incluso de iniciar sesión**. Además, al hacer login se **refresca** (best-effort) volviendo a leer la config pública, por si el branding cambió desde entonces.

3. **Almacenamiento.** Los tokens de color (serializados a JSON) y la ruta del logo se persisten en **DataStore** (`SessionStore`). El branding pertenece al servidor, no a la sesión: **sobrevive al cierre de sesión** y solo se borra al **cambiar de servidor** (junto con su URL y su JWT).

4. **Exposición.** El `ThemeViewModel` (compartido a nivel de `Activity`) lee el branding persistido y expone dos `StateFlow`: la paleta resuelta a un modelo `BrandingColors` (enteros ARGB, con cada token ausente cayendo a su fallback) y la **URL absoluta del logo** (combinando la dirección del servidor activo con `branding.logoUrl`; `null` mientras no haya logo). Mientras no haya branding guardado emite el fallback de carga.

5. **Aplicación.**
   - **Colores:** los tokens se proyectan sobre el `ColorScheme` de Material 3 dentro de `SelfpotifyTheme` (Jetpack Compose). Toda la jerarquía de composables hereda el branding vía `MaterialTheme.colorScheme`; los tokens extra (texto secundario, hover del acento…) están disponibles como `LocalBrandingColors.current`. La `MainActivity` también tiñe las barras del sistema con el color de fondo del servidor desde el primer frame, leyendo la paleta persistida.
   - **Logo:** la URL absoluta se publica vía `LocalServerLogoUrl` y la consume el composable común `ServerLogo`, que carga la imagen del servidor con **Coil** (`AsyncImage`). Todas las pantallas del flujo (configuración de servidor, login, home y sin-conexión) usan `ServerLogo` en lugar del recurso local; mientras la imagen llega, si la descarga falla o si el servidor no define logo, `ServerLogo` cae al logo de Selfpotify empaquetado.

### Estructura de navegación principal y reproductor

La app logueada es un `Scaffold` con un `NavHost` anidado para las cuatro pestañas y una `NavigationBar` inferior; sobre la barra vive un **mini-player** persistente (carátula, título/artista, play-pausa y acceso a "añadir a playlist") que solo aparece cuando hay algo cargado. Pulsarlo abre el **reproductor a pantalla completa** (carátula grande, barra de progreso con *seek*, anterior/siguiente, play-pausa y **loop**), que se desliza desde abajo como destino del NavHost externo.

**Reproducción con Media3 en un servicio en primer plano.** El audio lo gestiona un `MediaSessionService` (`PlaybackService`) que aloja un único `ExoPlayer`: la música **sobrevive en segundo plano**, con notificación multimedia y controles en la pantalla de bloqueo. La UI no habla con el servicio directamente, sino a través de un `MediaController` envuelto en `PlaybackConnection`, que expone el estado del player como `StateFlow` a los ViewModels (MVVM).

**Streaming con stream token.** Para reproducir, el cliente pide un stream token (`POST /api/listen/token`, con el JWT en cabecera) y construye las URLs de la cola como `/api/listen/{id}?st=<token>` (ver "Funcionamiento del streaming"). ExoPlayer hace las peticiones HTTP Range con esa URL, sin exponer el JWT.

**Añadir a playlist.** Desde el mini-player o el reproductor, un *bottom sheet* lista las playlists propias (`GET /api/playlists/my`) y añade la canción en curso a la elegida (`POST /api/playlists/{id}/songs/{songId}`).

### Deep link de invitación a playlist (`selfpotify://`)

La app es el destino nativo de los enlaces de invitación a playlist. El **lado web** (detección de dispositivo y puente a la app vía `intent:` en Android o `selfpotify://` en iOS) se explica en "Apertura en la app móvil"; aquí se documenta el **lado cliente Android**.

**Registro del esquema.** `MainActivity` declara un `intent-filter` (acción `VIEW` + categoría `BROWSABLE`) para el URI `selfpotify://playlist/share/{token}` (`scheme=selfpotify`, `host=playlist`, `pathPrefix=/share`). Se usa un **esquema propio** y no App Links verificadas porque el servidor es self-hosted y su dominio es arbitrario, lo que haría inviable publicar el `assetlinks.json` que exigen las App Links.

**Recepción del intent (`singleTask`).** La Activity es `launchMode="singleTask"`, así que el deep link puede llegar al **arrancar** (`onCreate`, vía `getIntent()`) o con la app **ya abierta** (`onNewIntent`). En ambos casos `extractShareToken` valida que el intent sea un `VIEW` con path `/share/{token}` y guarda el token en un `mutableStateOf` (`pendingShareToken`) que observa el árbol de Compose.

**Canje diferido hasta tener sesión.** El token pendiente se canjea contra el servidor configurado (`POST /api/playlists/share/{token}` → `PlaylistRepository.redeem`), se añade al usuario como colaborador y se **navega al detalle de la playlist**. Si todavía no hay sesión activa, el canje **queda pendiente** hasta completar el login y se ejecuta a continuación; una vez consumido, el token se limpia (`onShareTokenConsumed`). El token es server-relativo: la app lo canjea contra **su** servidor configurado, coherente con el modelo single-server de cada instalación.

### Pantalla Descubrir

*Descubrir* adopta la misma estructura que el home de la web: una **columna vertical de secciones**, donde cada sección es un **carrusel horizontal de carátulas** deslizable. A diferencia de la web —que aplica un efecto *coverflow* 3D (`rotateY`/`translateZ`)— el cliente Android lo mantiene **plano** (sin transformaciones 3D), priorizando rendimiento y simplicidad.

Las secciones, de arriba a abajo, son:

1. **Descubrimientos diarios** (`GET /api/feed/daily-discoveries`) — carrusel con **scroll infinito**: al acercarse a las dos últimas tarjetas se piden 10 canciones aleatorias (`GET /api/songs/random?count=10`) y se añaden al final, con un spinner mientras llega el lote.
2. **Artistas recomendados** (`GET /api/feed`) — carrusel de artistas con foto circular. Pulsar un artista abre su **pantalla de detalle** (ver «Pantallas de detalle»).
3. **Carruseles por género** — uno por cada género reciente del usuario (`GET /api/feed/genres`), cada uno con sus canciones top (`GET /api/songs/top?genre=`). Se omiten los géneros sin canciones.

Pulsar cualquier canción la reproduce usando la lista de su propio carrusel como cola. En el carrusel **diario** esa cola **se autoextiende**: cuando la reproducción llega a su última canción (el player ya no tiene "siguiente"), se piden más canciones aleatorias (`GET /api/songs/random`) y se añaden al final de la cola del player —sin cortar la reproducción— para que la música no se detenga al acabar el lote inicial. Esta extensión opera sobre la cola del reproductor, independiente del scroll infinito del carrusel visible, y deduplica por `id` para no repetir canciones ya encoladas. Los carruseles de género y artistas no se autoextienden (sus colas son finitas).

**Pull-to-refresh.** La pantalla soporta el gesto de **tirar hacia abajo para refrescar** (`PullToRefreshBox` de Material 3): vuelve a pedir todas las secciones —descubrimientos diarios, artistas y carruseles por género— desde el servidor. A diferencia de la carga inicial (que muestra el *loader* a pantalla completa cuando aún no hay nada), el refresco **mantiene el contenido actual visible** mientras llega el nuevo, mostrando solo el indicador de refresco; si el refresco falla se conserva el contenido previo y solo se marca error cuando no había nada que mostrar. El refresco reinicia el carrusel diario (descarta lo acumulado por el scroll infinito y vuelve a empezar desde el primer lote). Además, al refrescar **todas las listas vuelven a su primer elemento** —la columna vertical y cada carrusel horizontal— para que el contenido recargado no quede a media posición de scroll; esto se consigue recreando el subárbol de la lista con un `key` que cambia en cada refresco.

**Decisión de diseño: evitar el crash por scroll y acotar la caché del teléfono.** El scroll infinito pedía canciones aleatorias que podían repetir `id`s ya mostrados; como el carrusel usa el `id` como clave de `LazyRow`, esos duplicados provocaban un crash (`IllegalArgumentException` por clave repetida) al desplazarse lo suficiente. Ahora el `DiscoverViewModel` **deduplica por `id`** antes de añadir y **acota** el carrusel diario a un máximo de canciones acumuladas. Además, la app configura un `ImageLoader` de Coil global (`SelfpotifyApp`) con **cachés acotadas** —memoria ≤20 % del heap (LRU) y disco ≤50 MB— para que arrastrar carruseles largos de carátulas no agote la memoria ni el almacenamiento.

### Pantalla Búsqueda

*Búsqueda* monta una **barra de texto** sobre el endpoint transversal `GET /api/search` (ver «Búsqueda global»). Debajo de la barra se pinta una **vista previa multi-categoría**: una columna vertical de carruseles horizontales —canciones, artistas, álbumes, playlists, usuarios y géneros—, reutilizando los mismos carruseles planos de «Pantalla Descubrir».

**Decisión de diseño: búsqueda en vivo con *debounce*.** No hay botón de buscar: cada pulsación actualiza el campo al instante, pero la llamada a la API se dispara solo cuando el usuario deja de teclear (~300 ms). El `SearchViewModel` alimenta un `MutableStateFlow` con el texto y lo consume con `debounce` + `distinctUntilChanged` + `collectLatest`, de modo que una consulta nueva **cancela** la anterior en vuelo y solo se pinta el resultado de la última. Se usa el modo `all` del endpoint, que devuelve hasta **5 elementos por categoría** —tamaño pensado justamente para esta vista previa— sin paginación.

**Navegación desde los resultados.** Pulsar una canción la reproduce usando la lista de su carrusel como cola (igual que en Descubrir). **Artistas, álbumes, playlists y usuarios** abren su **pantalla de detalle** (ver «Pantallas de detalle»). Los **géneros** son la única categoría sin navegación: se muestran como chips informativos porque no hay pantalla de género. Cada categoría se omite si no trae resultados, y la pantalla cubre los cuatro estados habituales: indicación inicial (campo vacío), *loader*, error y "sin resultados" para la consulta tecleada.

### Pantalla Biblioteca

*Biblioteca* reúne las playlists del usuario al estilo de Spotify: una columna con una tarjeta de **«Nueva playlist»** arriba y, debajo, dos secciones —**tus playlists** (`GET /api/playlists/my`, públicas y privadas) y las **compartidas contigo** (`GET /api/playlists/shared`, donde eres colaborador)—, cada una en tarjetas con carátula, nombre y descripción que abren su detalle. Las privadas muestran un candado y las que tienen colaboradores, un icono de «compartida».

**Alta y edición de playlists con un formulario compartido.** La tarjeta de «Nueva playlist» y el botón de editar (en el detalle) abren la **misma hoja inferior**: nombre, descripción, interruptor **pública/privada** y selector de **carátula** mediante el *Photo Picker* del sistema (sin permisos de almacenamiento). Crear hace `POST /api/playlists` y, si se eligió imagen, `POST /api/playlists/{id}/cover` (multipart); editar hace `PUT /api/playlists/{id}` (+ carátula). En modo edición la hoja incluye además **borrar** la playlist (`DELETE /api/playlists/{id}`, con confirmación).

**Compartir por magic link, igual que la web.** El botón de compartir del detalle abre una hoja que genera un **enlace de un solo uso** (`POST /api/playlists/{id}/share`; ver «Playlists compartidas»), lo copia al portapapeles y permite regenerarlo, además de listar los **colaboradores** actuales (`GET /api/playlists/{id}/collaborators`) y quitarlos (`DELETE /api/playlists/{id}/collaborators/{userId}`). Editar metadatos, borrar y compartir son acciones **solo del propietario**; los colaboradores ven la playlist y pueden añadir/quitar canciones, pero no su «continente».

### Pantallas de detalle

Tanto desde Búsqueda como desde Descubrir se puede **abrir el detalle** de un artista, álbum, playlist o usuario. Hay cuatro pantallas:

- **Artista** — foto, nombre y sus **10 canciones más escuchadas** (`GET /api/artists/{id}` + `GET /api/artists/{id}/top-10-tracks`), al estilo de la vista web: **numeradas**, con su **número de escuchas** por canción y un botón **«+»** que abre una hoja para añadirla/quitarla de las playlists propias (`POST`/`DELETE /api/playlists/{id}/songs/{songId}`). Pulsar la fila reproduce.
- **Álbum** — carátula, nombre y sus canciones (`GET /api/albums/{id}`), reproducibles.
- **Playlist** — carátula, nombre/descripción y sus canciones (`GET /api/playlists/{id}`), reproducibles; cada canción muestra sus **escuchas** y un botón **«−»** para quitarla de la playlist (propietario o colaborador). Si la playlist es tuya, la cabecera ofrece **editar**, **compartir** y **borrar** (ver «Pantalla Biblioteca»); un icono de «compartida» aparece —para dueño y colaborador— cuando tiene colaboradores.
- **Usuario** — avatar, nombre y sus **playlists públicas** (`GET /api/users/{id}/public` + `GET /api/playlists/user/{userId}`); pulsar una playlist abre su detalle.

**Decisión de diseño: viven en el grafo de las pestañas, no en el externo.** Las pantallas de detalle son destinos del `NavHost` **anidado** de la app principal (el mismo que las pestañas), no del NavHost externo donde vive el reproductor. Así, al abrir un artista/álbum/playlist/usuario, la **barra de navegación inferior y el mini-player siguen visibles** y la flecha de retroceso vuelve a la pestaña de origen, igual que en Spotify.

**Decisión de diseño: álbum y playlist resuelven sus `songIds` en paralelo.** `AlbumDTO` y `PlaylistDTO` solo traen la **lista de ids** de canciones, no los `SongDTO` completos. La pantalla los resuelve con llamadas concurrentes a `GET /api/songs/{id}`, conservando el orden de la lista y **descartando** las que fallen (una canción borrada no debe tumbar toda la pantalla). Es una elección consciente para no añadir un endpoint nuevo en el backend; los catálogos personales hacen que el coste sea asumible.

**Decisión de diseño: borrado optimista con «deshacer» (canciones y colaboradores).** Quitar una canción de la playlist —o un colaborador desde la hoja de compartir— **no llama al backend de inmediato**. La fila desaparece de la UI al instante y aparece un *snackbar* «Se ha eliminado «X», pulsa para deshacer» con una **ventana de gracia de 3 s**. Si el usuario **no** pulsa deshacer, transcurrido ese tiempo se confirma el borrado en el backend (`DELETE /api/playlists/{id}/songs/{songId}` o `DELETE /api/playlists/{id}/collaborators/{userId}`); si **sí** lo pulsa, se cancela la petición y la fila se **restaura en su posición original**. Se eligió este esquema —borrar de la UI y diferir la confirmación— en lugar de borrar y volver a añadir, porque re-añadir **perdería el orden** de la lista ante un borrado accidental. El *ViewModel* es la fuente de verdad del temporizador y mantiene los borrados pendientes por id (admite varios a la vez); si la confirmación en el backend falla, la fila también se restaura. El *snackbar* de las canciones se ancla a la pantalla de detalle, mientras que el de colaboradores se ancla al propio *bottom sheet* de compartir (si no, quedaría oculto tras él).

### Flujo de acceso: servidor, login y sesión

Como cada usuario aloja su propio servidor, la app no tiene una URL fija: lo primero que hace es **preguntar a qué servidor conectarse**. El acceso son tres pantallas encadenadas:

1. **Configuración de servidor.** El usuario escribe la dirección (con un *helper* que muestra el formato esperado, p. ej. `http://192.168.1.10:8080`). Cuando deja de escribir (con un pequeño *debounce*), la app valida en segundo plano —mostrando un *loader*— que esa dirección es **realmente un servidor Selfpotify**, llamando a su `GET /api/config/public` (endpoint público, sin auth) y comprobando que devuelve un `branding` válido. El botón **Siguiente** permanece deshabilitado hasta que la validación tiene éxito. La dirección se normaliza a una forma canónica (con esquema, sin barra final) y se **guarda en el almacenamiento local del teléfono** para tenerla siempre disponible.

2. **Login / registro.** Misma lógica que la web: el usuario inicia sesión (`POST /api/auth/login`) o crea una cuenta (`POST /api/auth/signup`, que tras registrar inicia sesión automáticamente). El JWT recibido se **guarda asociado al servidor que lo emitió**: la sesión solo se considera válida si el servidor activo coincide con el servidor del token, de modo que **un JWT nunca se reutiliza en un servidor al que no pertenece**. Esta pantalla también ofrece un botón **Cambiar de servidor** que descarta el servidor y su token y vuelve al paso 1.

3. **App principal.** Tras el login se entra al contenedor principal: una **barra de navegación inferior** (estilo Spotify) con cuatro pestañas —**Descubrir, Búsqueda, Biblioteca y Perfil**— y, encima de ella, un **mini-player** persistente. *Descubrir* replica la estructura del home de la web (ver «Pantalla Descubrir»): una columna de carruseles horizontales con los descubrimientos diarios (`GET /api/feed/daily-discoveries`) y scroll infinito (`GET /api/songs/random`), artistas recomendados (`GET /api/feed`) y un carrusel por cada género reciente (`GET /api/feed/genres` + `GET /api/songs/top?genre=`), y permite reproducir. *Búsqueda* ofrece una barra que busca en vivo sobre `GET /api/search` y muestra una vista previa multi-categoría (ver «Pantalla Búsqueda»); *Biblioteca* reúne tus playlists y las compartidas contigo, y permite crearlas, editarlas, borrarlas y compartirlas (ver «Pantalla Biblioteca»). *Perfil* muestra el avatar y el nombre visible del usuario (`GET /api/me`), ambos **editables en línea**: tocar la foto abre una hoja con *Cambiar foto* (selector del sistema → `POST /api/me/profile/picture`) y *Eliminar foto* (`DELETE /api/me/profile/picture`), y un lápiz junto al nombre abre un diálogo que lo guarda (`PUT /api/me/profile`). Bajo el nombre, los contadores de **seguidores** y **seguidos** abren cada uno una **cuadrícula** de usuarios (`GET /api/users/{id}/followers` y `/following`); en mi propia lista de seguidos cada fila incluye un botón *dejar de seguir* (`DELETE /api/users/{id}/follow`), coherente con la decisión de «los botones por fila solo viven en mis propias listas». Tocar un usuario de una cuadrícula —o buscarlo— abre su **perfil** (`GET /api/users/{id}/public`), que reutiliza la misma cabecera **sin** iconos de edición, añade el botón **seguir / dejar de seguir** y lista sus playlists públicas o colaborativas conmigo (icono de personitas en las colaborativas). El perfil también aloja **Cerrar sesión** (borra el JWT, conserva el servidor, devolviendo al paso 2) y **Cambiar de servidor** (borra servidor + JWT + branding, devolviendo al paso 1). A diferencia del cliente web —donde la edición vive en una página aparte y la pantalla pública nunca contiene inputs—, en Android la edición es **inline sobre el propio perfil**; la vista de otros usuarios sí se mantiene sin inputs. La app solo expone features de usuario (sin admin). Al entrar al contenedor principal la app **verifica el JWT** contra el servidor (`GET /api/me`): si el servidor lo **rechaza** (token expirado o inválido) cierra sesión automáticamente y vuelve al login —conservando el servidor—; si el servidor **no responde**, se muestra la pantalla de sin-conexión (a diferencia del JWT inválido, aquí la sesión se conserva para reintentar).

Si, estando ya logueado, el servidor deja de responder, la app no se queda en un contenedor inerte: muestra una **pantalla de sin-conexión** ("No hay conexión al servidor actualmente"). La comprobación se dispara al entrar al contenedor principal, como parte de la misma verificación de sesión que valida el JWT (`GET /api/me`): si la petición falla por falta de red se asume el servidor caído. Esa pantalla ofrece dos acciones: **Reintentar conexión** (vuelve a comprobar el servidor con el endpoint público `GET /api/config/public`; si responde, regresa al contenedor principal) y **Desconectarse del servidor** (borra servidor + JWT + branding —paleta y logo— y vuelve al paso 1, para poder apuntar a otro servidor). No es un paso del flujo lineal de acceso, sino un estado al que se llega cuando la conexión cae.

Al arrancar, la app decide la pantalla inicial según el estado persistido: sin servidor → configuración de servidor; con servidor pero sin JWT válido → login; con servidor y JWT válido → home.

```mermaid
flowchart TD
    Launch([Arranque de la app]) --> Check{¿Estado<br/>persistido?}
    Check -- sin servidor --> S1
    Check -- servidor sin JWT válido --> S2
    Check -- servidor + JWT válido --> S3

    S1[Pantalla 1: dirección del servidor] --> Validate[GET /api/config/public<br/>loader mientras valida]
    Validate -- no es Selfpotify / no responde --> S1
    Validate -- branding válido --> Enable[Activar Siguiente<br/>+ guardar URL en local storage]
    Enable --> S2

    S2[Pantalla 2: login / registro] -->|Cambiar de servidor| Forget[Borrar servidor + JWT]
    Forget --> S1
    S2 --> Auth[POST /api/auth/login<br/>o /signup + login]
    Auth -- credenciales inválidas --> S2
    Auth -- 200 OK --> SaveJwt[Guardar JWT atado<br/>al servidor emisor]
    SaveJwt --> S3

    S3[Pantalla 3: contenedor principal<br/>Descubrir / Búsqueda / Biblioteca / Perfil] -->|Cerrar sesión / JWT rechazado| ClrJwt[Borrar JWT<br/>conservar servidor]
    ClrJwt --> S2
    S3 -->|Cambiar de servidor| Forget
    S3 -->|servidor no responde| S4[Pantalla sin conexión]
    S4 -->|Reintentar conexión| S3
    S4 -->|Desconectarse del servidor| Forget
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
        - int bpm
        - String songPath
        - boolean available
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
        - String picture_path
        - String mbid
        - List~Album~ albums
        - List~Song~ songs
        + copy(Artist)
    }

    class Playlist {
        - Long id
        - String name
        - String description
        - List~Song~ songs
        - int duration_ms
        - boolean isPublic
        - String pictureUrl
        - User creator
        + copy(Playlist)
    }

    class User {
        - Long id
        - Profile profile
        - String username
        - String password
        - UserFeed userFeed
        + copy(User)
    }

    class Admin {
    }

    class Profile {
        - Long id
        - String name
        - String pictureUrl
        + copy(Profile)
    }

    class UserFeed {
        - Long id
        - List~Artist~ recommendedArtists
        - List~String~ last20GenresListened
        + copy(UserFeed)
    }

    class UserSongListen {
        - Long id
        - User user
        - Song song
        - Instant listenedAt
    }

    class UserFollow {
        - Long id
        - User follower
        - User followed
        - Instant createdAt
    }

    class PlaylistCollaborator {
        - Long id
        - Playlist playlist
        - User user
        - Instant createdAt
    }

    class PlaylistShareToken {
        - Long id
        - String token
        - Playlist playlist
        - Instant createdAt
    }

    %% Herencia
    Admin --|> User : es un

    %% Relaciones User
    User "1" --> "1" Profile : tiene
    User "1" --> "1" UserFeed : tiene
    User "1" --> "N" Playlist : crea

    %% Relaciones UserFeed
    UserFeed "N" o--o "N" Artist : recomienda

    %% Tabla cruzada de escuchas (máx. 1000 por usuario, FIFO)
    UserSongListen "N" --> "1" User : la registra
    UserSongListen "N" --> "1" Song : referencia

    %% Tabla cruzada de seguimiento (grafo dirigido follower → followed)
    UserFollow "N" --> "1" User : follower
    UserFollow "N" --> "1" User : followed

    %% Colaboración en playlists (tabla cruzada + magic link de un solo uso)
    PlaylistCollaborator "N" --> "1" Playlist : colabora en
    PlaylistCollaborator "N" --> "1" User : colaborador
    PlaylistShareToken "N" --> "1" Playlist : invita a

    %% Relaciones música
    Album "N" o--o "N" Artist : es grabado por
    Album "1" --> "N" Song : contiene
    Song "N" o--o "N" Artist : es interpretada por
    Playlist "N" o--o "N" Song : agrupa
```

---

## Diagramas de casos de uso

### UC1 — Incorporar música a la biblioteca (carpeta o subida)

```mermaid
graph LR
    Admin["👤 Administrador"]

    subgraph Sistema Self-Potify
        UC1("Incorporar música a la biblioteca")
        UC1f("Añadir carpeta al path<br/>(POST /api/config/scan-paths)")
        UC1g("Subir audios drag & drop<br/>(POST /api/songs/upload → staging,<br/>POST /api/songs/commit → selfpotify_added)")
        UC1a("Leer etiquetas ID3")
        UC1b("Crear / actualizar Artista<br/>(canónico + MBID vía Last.fm)")
        UC1c("Crear / actualizar Álbum")
        UC1d("Persistir Canción")
        UC1e("Autocompletar género (Last.fm)<br/>y carátulas/foto de artista<br/>(Cover Art Archive, iTunes, Deezer)")
    end

    Admin --> UC1
    UC1 -.->|include| UC1f
    UC1 -.->|include| UC1g
    UC1f -.->|include| UC1a
    UC1g -.->|include| UC1a
    UC1a -.->|include| UC1b
    UC1a -.->|include| UC1c
    UC1a -.->|include| UC1d
    UC1a -.->|include| UC1e
```

### UC2 — Crear playlist, añadir canciones y compartir

```mermaid
graph LR
    Owner["👤 Propietario"]
    Collab["👤 Colaborador"]

    subgraph Sistema Self-Potify
        UC2("Crear playlist")
        UC2a("Buscar canción")
        UC2b("Añadir canción a playlist")
        UC2c("Quitar canción de playlist")
        UC2d("Generar magic link")
        UC2e("Canjear magic link<br/>(unirse como colaborador)")
    end

    Owner --> UC2
    Owner --> UC2b
    Owner --> UC2c
    Owner --> UC2d
    Collab --> UC2e
    Collab --> UC2b
    Collab --> UC2c
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
        UC5a("Hacer streaming de audio<br/>(HTTP Range)")
        UC5b("Registrar género escuchado<br/>en la pila del usuario")
        UC5c("Registrar evento en<br/>user_song_listen (FIFO 1000)")
    end

    User --> UC5
    UC5 -.->|include| UC5a
    UC5 -.->|include| UC5b
    UC5 -.->|include| UC5c
```

### UC6 — Setup inicial del servidor

```mermaid
graph LR
    Admin["👤 Operador (sin login,<br/>en modo setup)"]

    subgraph Sistema Self-Potify
        UC6("Completar setup inicial")
        UC6a("Definir branding<br/>(appName, colores, logo)")
        UC6b("Registrar rutas de escaneo")
        UC6c("Fijar intervalo de escaneo")
        UC6d("Persistir config en YAML<br/>y marcar setupComplete")
        UC6e("Lanzar escaneo inicial")
        UC6f("Crear usuarios iniciales")
    end

    Admin --> UC6
    UC6 -.->|include| UC6a
    UC6 -.->|include| UC6b
    UC6 -.->|include| UC6c
    UC6 -.->|include| UC6f
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
        UC8a("Validar appName / colores hex<br/>y derivar paleta con contraste WCAG")
        UC8b("Subir logo (PNG/JPG/SVG/WebP, ≤2MB)")
        UC8c("Persistir en YAML y servir vía /assets/**")
    end

    Admin --> UC8
    UC8 -.->|include| UC8a
    UC8 -.->|include| UC8b
    UC8b -.->|include| UC8c
```

### UC9 — Ver el feed de recomendaciones del home

```mermaid
graph LR
    User["👤 Usuario"]

    subgraph Sistema Self-Potify
        UC9("Abrir el home")
        UC9a("Regenerar feed del usuario")
        UC9b("Recomendar hasta 10 artistas<br/>por géneros recientes + 3 aleatorios<br/>(todos los artistas si no tiene escuchas)")
        UC9c("Mostrar artistas recomendados")
    end

    User --> UC9
    UC9 -.->|include| UC9a
    UC9a -.->|include| UC9b
    UC9 -.->|include| UC9c
```

### UC10 — Ver la página de un artista

```mermaid
graph LR
    User["👤 Usuario"]

    subgraph Sistema Self-Potify
        UC10("Abrir página de artista")
        UC10a("Consultar datos del artista")
        UC10b("Listar top 10 canciones<br/>del artista por oyentes")
    end

    User --> UC10
    UC10 -.->|include| UC10a
    UC10 -.->|include| UC10b
```

### UC11b — Ver tu propio perfil y editarlo

```mermaid
graph LR
    User["👤 Usuario"]

    subgraph Sistema Self-Potify
        UCP("Ver tu perfil (/profile)")
        UCPa("Cargar mi vista pública<br/>(GET /api/me + /api/users/{id}/public)")
        UCPb("Listar mis playlists públicas<br/>(GET /api/playlists/user/{userId})")
        UCE("Editar perfil (/profile/edit)")
        UCEa("Cambiar nombre visible<br/>(PUT /api/me/profile)")
        UCEb("Subir foto<br/>(POST /api/me/profile/picture)")
        UCEc("Quitar foto<br/>(DELETE /api/me/profile/picture)")
    end

    User --> UCP
    UCP -.->|include| UCPa
    UCP -.->|include| UCPb
    UCP -.->|extend| UCE
    UCE -.->|extend| UCEa
    UCE -.->|extend| UCEb
    UCE -.->|extend| UCEc
```

### UC11c — Ver el perfil público de otro usuario

```mermaid
graph LR
    User["👤 Usuario"]

    subgraph Sistema Self-Potify
        UCV("Abrir perfil de otro usuario (/user/[id])")
        UCVa("Buscar usuario<br/>(/api/search?type=users)")
        UCVb("Cargar vista pública<br/>(GET /api/users/{id}/public)")
        UCVc("Listar sus playlists públicas<br/>(GET /api/playlists/user/{userId})")
    end

    User --> UCV
    UCV -.->|include| UCVa
    UCV -.->|include| UCVb
    UCV -.->|include| UCVc
```

### UC11d — Seguir / dejar de seguir a otro usuario

```mermaid
graph LR
    User["👤 Usuario"]

    subgraph Sistema Self-Potify
        UCF("Seguir / dejar de seguir")
        UCFa("Resolver follower<br/>del SecurityContext")
        UCFb("Validar follower ≠ followed")
        UCFc("Crear o borrar arista<br/>UserFollow (idempotente)")
        UCFd("Recalcular counts +<br/>isFollowedByMe del target")
    end

    User --> UCF
    UCF -.->|include| UCFa
    UCF -.->|include| UCFb
    UCF -.->|include| UCFc
    UCF -.->|include| UCFd
```

### UC11e — Ver las listas de seguidores / siguiendo

```mermaid
graph LR
    User["👤 Usuario"]

    subgraph Sistema Self-Potify
        UCL("Abrir /user/[id]/followers o /following")
        UCLa("Obtener lista<br/>(GET /api/users/{id}/followers|following)")
        UCLb("Enriquecer DTOs en batch<br/>(counts + isFollowedByMe)")
        UCLc{"¿La lista es mía<br/>(me.id == [id])?"}
        UCLd("Render filas SIN botón")
        UCLe("Render filas CON botón<br/>Siguiendo / Seguir")
    end

    User --> UCL
    UCL -.->|include| UCLa
    UCLa -.->|include| UCLb
    UCLb --> UCLc
    UCLc -- no --> UCLd
    UCLc -- sí --> UCLe
```

### UC11 — Ver los descubrimientos diarios

```mermaid
graph LR
    User["👤 Usuario"]

    subgraph Sistema Self-Potify
        UC11("Abrir el home")
        UC11a("Calcular descubrimientos diarios<br/>(estables por día)")
        UC11b("Tomar 3 canciones aleatorias")
        UC11c("Tomar 3 no escuchadas<br/>del último género")
        UC11d("Tomar 3 de un género<br/>que no escucha")
        UC11e("Mostrar 9 canciones<br/>mezcladas en el deslizable")
    end

    User --> UC11
    UC11 -.->|include| UC11a
    UC11a -.->|include| UC11b
    UC11a -.->|include| UC11c
    UC11a -.->|include| UC11d
    UC11 -.->|include| UC11e
```

### UC12 — Gestionar el catálogo de canciones

```mermaid
graph LR
    Admin["👤 Administrador"]

    subgraph Sistema Self-Potify
        UC12("Gestionar catálogo de canciones")
        UC12a("Subir audios drag & drop<br/>(POST /api/songs/upload)")
        UC12e("Autocompletar en staging<br/>género/artista/carátula (Last.fm,<br/>Cover Art Archive/iTunes/Deezer)<br/>antes de mostrar la edición previa")
        UC12b("Editar metadatos<br/>(PUT /api/songs/{id}:<br/>title, género, BPM, duración, carátula)")
        UC12c("Eliminar canción<br/>(DELETE /api/songs/{id})")
        UC12d("Conservar songPath<br/>(la edición no toca la ruta física)")
    end

    Admin --> UC12
    UC12 -.->|include| UC12a
    UC12 -.->|include| UC12b
    UC12 -.->|include| UC12c
    UC12a -.->|include| UC12e
    UC12b -.->|include| UC12d
```

### UC13 — Cambiar el rol de un usuario

```mermaid
graph LR
    Admin["👤 Administrador"]

    subgraph Sistema Self-Potify
        UC13("Cambiar rol de usuario")
        UC13a("Reasignar discriminador users.type<br/>(PUT /api/users/{id}/role)")
        UC13b{"¿Es el último ADMIN<br/>y se intenta degradar?"}
        UC13c("Rechazar con 400<br/>(no degradar al último admin)")
        UC13d("Refrescar contexto<br/>y devolver usuario actualizado")
    end

    Admin --> UC13
    UC13 -.->|include| UC13a
    UC13a --> UC13b
    UC13b -- sí --> UC13c
    UC13b -- no --> UC13d
```

## Diagrama de arquitectura

![Diagrama sin título.drawio.png](img/Diagrama%20sin%20t%C3%ADtulo.drawio.png)
