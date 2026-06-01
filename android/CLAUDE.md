# CLAUDE.md — Android Selfpotify

Instrucciones específicas para trabajar en el cliente Android de Selfpotify.
Este archivo complementa `CLAUDE.local.md` (raíz del monorepo), que sigue vigente en todo lo que no contradiga estas reglas.

---

## 1. Scope de trabajo

- **Tu dominio:** todo lo que esté dentro de `android/`.
- **El backend Spring es sagrado.** No modifiques nada fuera de `android/` sin autorización explícita del usuario.
  Si un cambio en el cliente requiere un cambio de API, detente y pregunta primero.

---

## 2. Arquitectura: MVVM estricto

Toda pantalla o feature sigue esta estructura de capas. No mezcles responsabilidades entre ellas.

```
android/app/src/main/java/davila/anton/selfpotify/
├── data/
│   ├── model/          ← DTOs / entidades de dominio (data class Kotlin puras)
│   ├── network/        ← Retrofit interfaces + interceptors (SelfpotifyApi.kt, AuthInterceptor.kt…)
│   └── repository/     ← Repositorios (única fuente de verdad; combinan red + caché si procede)
├── ui/
│   ├── <feature>/      ← Una carpeta por pantalla o flujo (home/, player/, library/, login/…)
│   │   ├── <Feature>Fragment.kt   (o Activity si es entry-point)
│   │   ├── <Feature>ViewModel.kt
│   │   └── <Feature>Adapter.kt   (si la pantalla tiene RecyclerView / lista)
│   └── common/         ← Componentes de UI reutilizables (custom views, base fragments, etc.)
├── util/               ← Helpers sin estado (extensiones, formatters, constantes de navegación)
└── di/                 ← Módulos Hilt (si se añade inyección de dependencias en el futuro)
```

### Reglas por capa

| Capa | Puede acceder a | No puede acceder a |
|------|-----------------|--------------------|
| `model` | Nada externo | Todo lo demás |
| `network` | `model` | `repository`, `ui` |
| `repository` | `network`, `model` | `ui` |
| `ViewModel` | `repository` | Views, Context (salvo `Application`) |
| `Fragment/Activity` | `ViewModel` (vía `by viewModels()`), `common/` | `repository`, `network` directamente |

- Los ViewModels exponen **`StateFlow`** o **`LiveData`** y nunca referencias a vistas.
- Los repositorios devuelven tipos `Result<T>` o `Flow<T>` para propagar errores sin lanzar excepciones a la UI.

---

## 3. UI: estilo Spotify con tema dinámico del servidor

El look & feel de referencia en **estructura y navegación** es la app oficial de Spotify para Android. Sin embargo, **la paleta de colores es dinámica y viene del servidor**: Selfpotify es personalizable por instalación.

### 3.1 Colores dinámicos (branding del servidor)

- Al arrancar (antes del login), la app llama a `GET /api/config/public` — endpoint público, sin auth — y recibe `branding.colors`, un mapa de variables CSS con los 14 tokens de la paleta (`--color-bg`, `--color-accent`, `--color-text-primary`, `--color-on-accent`, etc.).
- La paleta se deriva en el servidor a partir de dos semillas (acento y fondo) en el espacio HCT de Material con contraste WCAG garantizado. **No hardcodees ningún color de marca en el cliente.**
- Guarda los tokens recibidos en `DataStore` y aplícalos en tiempo de ejecución usando `MaterialColors` / atributos de tema dinámicos o `ColorStateList` programáticos.
- Define en `res/values/colors.xml` únicamente los **valores de fallback** (por si el servidor no responde antes del primer render): fondo oscuro neutro `#121212`, acento neutro `#1DB954`, texto blanco `#FFFFFF`. Estos valores NO son el branding de la app — son un placeholder de carga.
- El `ThemeViewModel` (o `ConfigViewModel`) es el responsable de exponer los tokens de color como `StateFlow<BrandingColors>` al resto de la UI.

### 3.2 Estructura y navegación (inspirada en Spotify)

- **Tipografía:** `sans-serif` (`Roboto`). Jerarquía: título 20 sp bold, subtítulo 14 sp medium, cuerpo 14 sp regular, caption 12 sp.
- **Iconografía:** Material Symbols Rounded. Sin iconos outline por defecto.
- **Espaciado:** múltiplos de 8 dp. Padding lateral de página: 16 dp.
- **Esquinas:** `8 dp` tarjetas de álbum/playlist, `4 dp` chips, `50 %` avatares y botón play principal.
- **Animaciones:** `MaterialSharedAxis` (eje Z para profundidad, eje X para tabs). Fade-in 200 ms en imágenes.
- **Bottom navigation:** máx. 4 ítems (Home, Buscar, Biblioteca, Perfil). `BottomNavigationView` con tema M3.
- **Mini-player persistente:** barra fija sobre la bottom navigation cuando hay reproducción activa. Se implementa en el fragmento contenedor principal, no en cada pantalla.

---

## 4. Strings: i18n obligatorio

- **Nunca** pongas texto literal en layouts XML ni en código Kotlin.
- Todo texto visible va en `res/values/strings.xml`.
- Convención de nombres: `<pantalla>_<elemento>` → `login_title`, `player_play_button`, `home_greeting`.
- Para plurales usa `<plurals>` en vez de lógica en Kotlin.
- Para traducciones futuras, crea `res/values-es/strings.xml` (español) como lengua base y `res/values/strings.xml` en inglés.

---

## 5. Conexión con la API de Selfpotify

- La URL base se configura en tiempo de ejecución (el usuario introduce IP:puerto en la primera pantalla de configuración).
  Guárdala en `SharedPreferences` o `DataStore` y reconstruye el cliente Retrofit cuando cambie.
- Autenticación JWT: el token se obtiene en `POST /api/auth/login` y se adjunta como `Authorization: Bearer <token>` en todas las peticiones mediante un `OkHttp Interceptor`.
- Para el streaming de audio usa `Media3 ExoPlayer`; el endpoint acepta el token como query param `?token=<jwt>` (necesario porque `<audio>` / ExoPlayer no manda headers en la petición inicial de datos).
- Referencia de la API: `API-doc.md` en la raíz del monorepo.

---

## 6. Dependencias recomendadas

Añade solo lo que necesites. Las siguientes están pre-aprobadas:

| Librería | Uso |
|----------|-----|
| `Retrofit2` + `OkHttp3` | Llamadas REST a la API |
| `Moshi` o `Gson` | Serialización JSON |
| `Coil` | Carga de imágenes (async, compatible con Compose y View system) |
| `Media3 (ExoPlayer)` | Reproducción de streaming de audio |
| `Navigation Component` | Navegación entre fragmentos |
| `ViewModel` + `LiveData` / `StateFlow` | MVVM |
| `Hilt` | Inyección de dependencias (añadir cuando haya ≥ 3 repositorios) |
| `DataStore Preferences` | Persistencia de configuración (URL del servidor, token, prefs de usuario) |
| `Material Components 3` | Tema y componentes visuales |

Cualquier dependencia fuera de esta lista requiere justificación en el mensaje de commit.

---

## 7. Commits

- **Prefijo:** `android:` en todos los commits que toquen archivos dentro de `android/`.
- Un commit por archivo modificado (salvo cambios atómicos inseparables).
- Sin firma de Claude (`Co-Authored-By`, `🤖`, etc.).
- Estilo: imperativo en minúsculas, máx. 72 caracteres, sin punto final.
  Ejemplo: `android: agregar SongRepository con llamada a GET /api/songs`

---

## 8. README y documentación

**Esta regla tiene prioridad sobre cualquier otra instrucción técnica.**

- **Antes de escribir o modificar cualquier código**, lee las secciones relevantes de `README.md` (raíz) y comprueba si el cambio que vas a hacer es coherente con las decisiones de diseño documentadas.
- Si detectas **cualquier divergencia o tensión** entre lo que el código va a quedar y lo que dice el `README.md`, **detente inmediatamente y pregunta al usuario** cuál de las dos opciones prefiere:
  1. Adaptar el código al diseño (mantener el README y cambiar el código para cumplirlo).
  2. Adaptar el diseño al código (modificar el README para reflejar la nueva realidad).
- **Nunca modifiques `README.md` por iniciativa propia**, ni aunque el cambio parezca trivial (typo, reformulación, reordenar secciones). Cualquier cambio en README requiere confirmación explícita del usuario.
- Esta regla aplica incluso cuando el usuario pide un cambio de código que choca con el README: primero pregunta, luego ejecuta.

---

## 9. Checklist antes de dar un cambio por terminado

- [ ] No hay strings literales en XML ni Kotlin (todo en `strings.xml`).
- [ ] El ViewModel no importa nada de `android.view.*`.
- [ ] El Fragment/Activity no llama directamente a ningún repositorio o servicio de red.
- [ ] Los colores y dimensiones usan referencias a recursos (`@color/`, `@dimen/`), no valores literales.
- [ ] Se ha añadido el commit `android:` correspondiente.
- [ ] Si cambió la superficie de la API, se actualizó o se preguntó sobre `API-doc.md`.
