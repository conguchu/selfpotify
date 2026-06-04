# Selfpotify: Memoria de Decisiones de Diseño

## Decisiones de Diseño

### Confirmación y Reversión en Operaciones Destructivas

#### Web: Modales de Confirmación

Las operaciones destructivas en la interfaz web (eliminación de playlists, canciones, etc.) **requieren una ventana modal de confirmación** antes de ejecutarse. Esta decisión:

- **Previene errores accidentales**: El usuario debe confirmar explícitamente su intención de borrar.
- **Proporciona claridad**: El modal muestra qué se va a eliminar exactamente (nombre, cantidad de elementos, etc.).
- **Mantiene la consistencia**: Es el patrón estándar en aplicaciones web modernas.

**Implementación**: Modal bloqueante con dos acciones: "Cancelar" y "Eliminar". El botón de eliminar tiene un color distintivo (rojo/destructivo) para indicar irreversibilidad.

#### Android: Toasts con Opción de Deshacer

Las operaciones destructivas en Android **se confirman mediante toasts con acción de "Deshacer"** en lugar de modales previos. Esta decisión:

- **Respeta las convenciones de UX móvil**: Los toasts son el patrón nativo de Android para confirmaciones no bloqueantes.
- **Reduce fricción**: El usuario no debe confirmar antes de actuar; la acción se ejecuta inmediatamente.
- **Proporciona reversibilidad**: Un botón "Deshacer" en el toast permite revertir la operación en ventana temporal (típicamente 5 segundos).
- **Mejora la experiencia**: Es menos intrusivo que un modal bloqueante en pantallas pequeñas.

**Implementación**: Al eliminar un elemento, la acción se ejecuta inmediatamente, se elimina la entrada de la UI, y se muestra un toast con el mensaje "X eliminado" + botón "Deshacer". Si el usuario presiona "Deshacer" antes de que el toast desaparezca, la acción se revierte.

#### Justificación de la Divergencia

La diferencia entre web y Android refleja:
- **Patrones nativos de cada plataforma**: Modales son estándar en web; toasts reversibles son estándar en Android.
- **Contexto de uso**: Usuarios web interactúan desde escritorio (pantalla grande, interacción deliberada); usuarios Android interactúan desde móvil (pantalla pequeña, interacciones más rápidas).
- **Mejor UX para cada medio**: No es usar lo mismo en todas partes, sino usar lo mejor para cada contexto.

### Scroll Infinito para Descubrimiento Continuo de Tracks

La feed de tracks implementa **scroll infinito** en lugar de paginación tradicional o necesidad de regenerar toda la feed manualmente. Esta decisión:

- **Reduce fricción en el descubrimiento**: El usuario puede desplazarse continuamente y recibir nuevos tracks aleatorios sin interrupciones ni acciones explícitas.
- **Evita regeneración innecesaria**: Sin scroll infinito, el usuario tendría que presionar un botón "Generar nueva feed" cada vez que agota el contenido visible, fragmentando la experiencia.
- **Mejora la retención**: El feed infinito favorece el consumo prolongado y continuo de contenido, manteniendo al usuario enganchado.
- **Patrón estándar en redes sociales**: Es el comportamiento esperado en plataformas modernas (Instagram, Twitter, TikTok, etc.).

**Implementación**: Al detectar que el usuario se acerca al final de la lista, se cargan automáticamente nuevos tracks aleatorios sin recargar la página ni la app. La transición es transparente, sin indicadores disruptivos.

**Beneficio clave**: El usuario explora continuamente música nueva sin cambios de contexto ni acciones deliberadas, haciendo la experiencia más fluida y descubridora.
