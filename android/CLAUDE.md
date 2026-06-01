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

## 3. UI: estilo Spotify

El look & feel de referencia es la app oficial de Spotify para Android. Sigue estos principios:

- **Paleta:** fondo `#121212`, superficie `#1E1E1E`, acento verde `#1DB954`, texto primario `#FFFFFF`, texto secundario `#B3B3B3`.
  Define estos colores en `res/values/colors.xml` con los nombres `colorBackground`, `colorSurface`, `colorAccent`, `colorTextPrimary`, `colorTextSecondary`.
- **Tipografía:** usa la familia `Circular` si está disponible; si no, `sans-serif` (`Roboto`). Jerarquía: título grande 20 sp bold, subtítulo 14 sp medium, cuerpo 14 sp regular, caption 12 sp.
- **Iconografía:** Material Symbols Rounded (`@style/Widget.MaterialComponents.*`). Sin iconos planos ni outline por defecto.
- **Espaciado:** múltiplos de 8 dp. Padding lateral estándar de página: 16 dp.
- **Esquinas:** `8 dp` para tarjetas de álbum/playlist, `4 dp` para chips, `50 %` (circular) para avatares y botón play principal.
- **Animaciones:** transiciones entre pantallas con `MaterialSharedAxis` (eje Z para navegación en profundidad, eje X para tabs). Fade-in de 200 ms en cargas de imagen.
- **Bottom navigation:** cuatro ítems máximo (Home, Buscar, Biblioteca, Perfil). Usa `BottomNavigationView` con el tema M3.
- **Mini-player persistente:** barra fija sobre la bottom navigation cuando hay reproducción activa. Se implementa en el fragmento contenedor principal (no en cada pantalla).

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

- Antes de cualquier cambio, comprueba si colisiona con las decisiones documentadas en `README.md` (raíz).
- Si detectas divergencia o quieres actualizar la documentación, **pregunta primero** al usuario.
- Nunca modifiques `README.md` por iniciativa propia.

---

## 9. Checklist antes de dar un cambio por terminado

- [ ] No hay strings literales en XML ni Kotlin (todo en `strings.xml`).
- [ ] El ViewModel no importa nada de `android.view.*`.
- [ ] El Fragment/Activity no llama directamente a ningún repositorio o servicio de red.
- [ ] Los colores y dimensiones usan referencias a recursos (`@color/`, `@dimen/`), no valores literales.
- [ ] Se ha añadido el commit `android:` correspondiente.
- [ ] Si cambió la superficie de la API, se actualizó o se preguntó sobre `API-doc.md`.
