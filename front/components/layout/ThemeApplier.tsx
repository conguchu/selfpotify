"use client";

import { useEffect } from "react";
import { usePublicConfig } from "@/lib/query/hooks";
import { onAccentFor, accentTextFor, enforceContrast } from "@/lib/palette";

/**
 * Aplica el branding guardado en el servidor: sobrescribe las variables
 * `--color-*` en `document.documentElement` y fija el título del documento con
 * el nombre de la app. Como `globals.css` define los colores con `@theme` (sin
 * `inline`), las utilidades Tailwind referencian `var(--color-*)`, así que estas
 * sobrescrituras retematizan toda la app sin recompilar CSS. Hasta que llega la
 * config se usan los defaults de globals.css.
 */
export function ThemeApplier() {
  const { data } = usePublicConfig();
  const colors = data?.branding.colors;
  const appName = data?.branding.appName?.trim();

  useEffect(() => {
    if (!colors) return;
    const root = document.documentElement;
    // Red de seguridad: cualquier color de texto ilegible (p. ej. editado a mano
    // en "Avanzado") se corrige al tono legible más cercano antes de pintar.
    const safe = enforceContrast(colors);
    for (const [key, value] of Object.entries(safe)) {
      if (key.startsWith("--color-") && value) {
        root.style.setProperty(key, value);
      }
    }
    // on-accent y accent-text se calculan siempre desde el acento/fondo aplicados
    // (no se almacenan): el primero hace legible el texto SOBRE botones, el
    // segundo el acento usado como texto/icono SOBRE el fondo.
    const accent = safe["--color-accent"];
    const bg = safe["--color-bg"];
    if (accent) root.style.setProperty("--color-on-accent", onAccentFor(accent));
    if (accent && bg)
      root.style.setProperty("--color-accent-text", accentTextFor(accent, bg));
  }, [colors]);

  useEffect(() => {
    if (appName) document.title = appName;
  }, [appName]);

  return null;
}
