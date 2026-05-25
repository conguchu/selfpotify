"use client";

import { useEffect } from "react";
import { usePublicConfig } from "@/lib/query/hooks";
import { onAccentFor } from "@/lib/palette";

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
    for (const [key, value] of Object.entries(colors)) {
      if (key.startsWith("--color-") && value) {
        root.style.setProperty(key, value);
      }
    }
    // on-accent se calcula siempre desde el acento aplicado (no se almacena),
    // para que el texto sobre botones sea legible con cualquier paleta.
    const accent = colors["--color-accent"];
    if (accent) root.style.setProperty("--color-on-accent", onAccentFor(accent));
  }, [colors]);

  useEffect(() => {
    if (appName) document.title = appName;
  }, [appName]);

  return null;
}
