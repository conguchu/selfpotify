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
│   │   ├── <Feature>Screen.kt     ← composable raíz de la pantalla
│   │   ├── <Feature>ViewModel.kt
│   │   └── <Feature>Components.kt ← composables auxiliares reutilizables dentro del feature
│   ├── theme/          ← SelfpotifyTheme, BrandingColors, ThemeViewModel, Spacing
│   └── common/         ← Composables reutilizables entre features
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
| `Screen/Composable` | `ViewModel` (vía `viewModel()`), `common/` | `repository`, `network` directamente |

- Los ViewModels exponen **`StateFlow`** y nunca referencias a vistas ni a contextos de composición.
- Los repositorios devuelven tipos `Result<T>` o `Flow<T>` para propagar errores sin lanzar excepciones a la UI.

---

## 3. UI: Jetpack Compose — OBLIGATORIO

**Toda la UI se implementa con Jetpack Compose. Queda prohibido usar el View system XML.**

- **No crees layouts XML** (ningún archivo en `res/layout/`).
- **No uses** `Fragment`, `ViewBinding`, `RecyclerView`, `ConstraintLayout`, `LinearLayout` ni ninguna vista del View system.
- **No uses** `AppCompatActivity` — usa `ComponentActivity` con `setContent { }`.
- Cada pantalla es un composable `@Composable fun <Feature>Screen(...)` en `ui/<feature>/<Feature>Screen.kt`.
- La navegación se gestiona con `Navigation Compose` (`NavHost` + `composable(route)` + `NavController`). No uses `nav_graph.xml`.
- El estado del ViewModel se colecta en los composables con `collectAsStateWithLifecycle()`.
- Los eventos de navegación (SharedFlow) se consumen con `LaunchedEffect(Unit) { flow.collect { ... } }`.

### 3.1 Colores dinámicos (branding del servidor)

- Al arrancar (antes del login), la app llama a `GET /api/config/public` — endpoint público, sin auth — y recibe `branding.colors`, un mapa de variables CSS con los tokens de la paleta.
- La paleta se proyecta sobre el `ColorScheme` de Material 3 en `SelfpotifyTheme` (fichero `ui/theme/Theme.kt`). **No hardcodees ningún color de marca en el cliente.**
- Los tokens que Material 3 no cubre (texto secundario, hover del acento, etc.) están disponibles vía `LocalBrandingColors.current`.
- Define en `res/values/colors.xml` únicamente los **valores de fallback** (por si el servidor no responde antes del primer render): fondo oscuro neutro `#121212`, acento neutro `#1DB954`, texto blanco `#FFFFFF`. Estos valores NO son el branding de la app — son un placeholder de carga.
- El `ThemeViewModel` expone los tokens de color como `StateFlow<BrandingColors>` y se instancia a nivel de Activity. `SelfpotifyTheme` se aplica una sola vez en `MainActivity` y todas las pantallas heredan el tema.

### 3.2 Estructura y navegación (inspirada en Spotify)

- **Tipografía:** `sans-serif` (`Roboto`). Jerarquía: título 20 sp bold, subtítulo 14 sp medium, cuerpo 14 sp regular, caption 12 sp.
- **Iconografía:** Material Symbols Rounded / `Icons.Rounded.*`. Sin iconos outline por defecto.
- **Espaciado:** múltiplos de 8 dp. Usa el objeto `Spacing` de `ui/theme/Theme.kt`. Padding lateral de página: `Spacing.page` (16 dp).
- **Esquinas:** `RoundedCornerShape(50)` botones principales (cápsula), `8.dp` tarjetas, `4.dp` chips.
- **Animaciones:** `AnimatedContent` / `slideIntoContainer` con eje X para transiciones entre pantallas; fade-in 200 ms en imágenes.
- **Bottom navigation:** máx. 4 ítems (Home, Buscar, Biblioteca, Perfil). `NavigationBar` de Material 3.
- **Mini-player persistente:** `Scaffold` a nivel del NavHost con un slot `bottomBar` para el mini-player activo.

---

## 4. Strings: i18n obligatorio

- **Nunca** pongas texto literal en composables ni en código Kotlin.
- Todo texto visible va en `res/values/strings.xml` (inglés base) y `res/values-es/strings.xml` (español).
- Accede con `stringResource(R.string.nombre_clave)` dentro de composables.
- Convención de nombres: `<pantalla>_<elemento>` → `login_title`, `player_play_button`, `home_greeting`.
- Para plurales usa `pluralStringResource(R.plurals.nombre, count, count)`.

---

## 5. Conexión con la API de Selfpotify

- La URL base se configura en tiempo de ejecución (el usuario introduce IP:puerto en la primera pantalla de configuración).
  Guárdala en `DataStore` y reconstruye el cliente Retrofit cuando cambie.
- Autenticación JWT: el token se obtiene en `POST /api/auth/login` y se adjunta como `Authorization: Bearer <token>` en todas las peticiones mediante un `OkHttp Interceptor`.
- Para el streaming de audio usa `Media3 ExoPlayer`; el endpoint acepta el token como query param `?token=<jwt>` (necesario porque ExoPlayer no manda headers en la petición inicial de datos).
- **Referencia de la API:** consulta siempre `API-doc.md` (raíz del monorepo) antes de hacer ninguna llamada o pregunta sobre endpoints. Si algo no cuadra o falta detalle, inspecciona directamente los controllers y DTOs en `src/main/java/` — nunca preguntes al usuario ni hagas suposiciones cuando la respuesta está en el código.

---

## 6. Dependencias recomendadas

Añade solo lo que necesites. Las siguientes están pre-aprobadas:

| Librería | Uso |
|----------|-----|
| `Jetpack Compose BOM` | UI declarativa (OBLIGATORIO) |
| `Navigation Compose` | Navegación entre pantallas |
| `Activity Compose` | `setContent {}` en ComponentActivity |
| `Lifecycle Runtime Compose` | `collectAsStateWithLifecycle()` |
| `ViewModel Compose` | `viewModel()` en composables |
| `Retrofit2` + `OkHttp3` | Llamadas REST a la API |
| `Moshi` o `Gson` | Serialización JSON |
| `Coil Compose` | Carga de imágenes async (usar `AsyncImage`) |
| `Media3 (ExoPlayer)` | Reproducción de streaming de audio |
| `ViewModel` + `StateFlow` | MVVM |
| `Hilt` | Inyección de dependencias (añadir cuando haya ≥ 3 repositorios) |
| `DataStore Preferences` | Persistencia de configuración (URL del servidor, token, prefs de usuario) |
| `Material 3 Compose` | Tema y componentes visuales |

Cualquier dependencia fuera de esta lista requiere justificación en el mensaje de commit.

---

## 7. Commits

- **Prefijo:** `android:` en todos los commits que toquen archivos dentro de `android/`.
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

- [ ] No hay strings literales en composables ni en Kotlin (todo en `strings.xml`).
- [ ] No hay layouts XML en `res/layout/` ni Fragments.
- [ ] El ViewModel no importa nada de `android.view.*` ni de `androidx.compose.*`.
- [ ] Los Screen composables no llaman directamente a ningún repositorio o servicio de red.
- [ ] Los colores usan `MaterialTheme.colorScheme.*` o `LocalBrandingColors.current`; nunca valores literales.
- [ ] Se ha añadido el commit `android:` correspondiente.
- [ ] Si cambió la superficie de la API, se actualizó o se preguntó sobre `API-doc.md`.
