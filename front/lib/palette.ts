import {
  TonalPalette,
  Hct,
  hexFromArgb,
  argbFromHex,
} from "@material/material-color-utilities";

/**
 * Genera la paleta completa de 14 colores del tema a partir de dos semillas,
 * usando el espacio HCT (Hue-Chroma-Tone) de Material — el mismo enfoque que el
 * generador de paletas de Google (Material Theme Builder). Trabajar en HCT
 * garantiza que las variaciones combinen y mantengan contraste: cada color se
 * obtiene fijando el *tono* (luminancia perceptual 0–100) sobre una paleta tonal
 * que conserva el matiz y el croma de la semilla.
 *
 * - `primary`   → familia acento (--color-accent y variantes). Se respeta el
 *   color elegido exactamente como `--color-accent`; hover/active/soft son tonos
 *   derivados de su misma paleta tonal.
 * - `secondary` → fondos (--color-bg y variantes) y textos por contraste.
 *
 * `--color-danger` / `--color-success` son semánticos: se toman de `base` si
 * existen, o de los defaults rojo/verde.
 *
 * Las claves devueltas coinciden exactamente con las que valida el backend
 * (ServerGlobalConfig.defaultColors()).
 */

const DEFAULT_DANGER = "#ef4444";
const DEFAULT_SUCCESS = "#16a34a";

function clampTone(t: number): number {
  return Math.min(100, Math.max(0, t));
}

/** Hex (#rrggbb) de una paleta tonal a un tono dado. */
function tone(palette: TonalPalette, t: number): string {
  return hexFromArgb(palette.tone(clampTone(t)));
}

export function derivePalette(
  primary: string,
  secondary: string,
  base?: Record<string, string>,
): Record<string, string> {
  const accentArgb = safeArgb(primary, 0xffb91c1c);
  const secArgb = safeArgb(secondary, 0xff0a0a0a);

  const accent = TonalPalette.fromInt(accentArgb);
  // Paleta neutra para fondos/textos: conserva el matiz del secundario con un
  // croma contenido para que los fondos no "vibren" (igual que Material).
  const secHct = Hct.fromInt(secArgb);
  const neutral = TonalPalette.fromHueAndChroma(
    secHct.hue,
    Math.min(secHct.chroma, 8),
  );

  const accentTone = Hct.fromInt(accentArgb).tone;
  const secTone = secHct.tone;
  // Tema oscuro si el fondo base es oscuro: los fondos suben de tono y el texto
  // es claro. En tema claro, todo se invierte.
  const dark = secTone < 50;

  return {
    // Fondos — el secundario se respeta como base; el resto son escalones
    // tonales hacia el "frente" (más claros en oscuro, más oscuros en claro).
    "--color-bg": hexFromArgb(secArgb),
    "--color-bg-elevated": tone(neutral, secTone + (dark ? 4 : -3)),
    "--color-bg-card": tone(neutral, secTone + (dark ? 7 : -5)),
    "--color-bg-hover": tone(neutral, secTone + (dark ? 11 : -8)),
    "--color-border": tone(neutral, secTone + (dark ? 16 : -14)),

    // Textos — tonos fijados para contraste legible sobre el fondo.
    "--color-text": tone(neutral, dark ? 96 : 12),
    "--color-text-muted": tone(neutral, dark ? 78 : 38),
    "--color-text-subtle": tone(neutral, dark ? 58 : 52),

    // Acentos — el primario se respeta tal cual; variantes en su paleta tonal.
    "--color-accent": hexFromArgb(accentArgb),
    "--color-accent-hover": tone(accent, accentTone + 8),
    "--color-accent-active": tone(accent, accentTone - 8),
    "--color-accent-soft": tone(accent, dark ? 25 : 90),

    // Semánticos — fijos (no se derivan), respetando lo que ya hubiera.
    "--color-danger": base?.["--color-danger"] ?? DEFAULT_DANGER,
    "--color-success": base?.["--color-success"] ?? DEFAULT_SUCCESS,
  };
}

/** Convierte un hex a ARGB; si el valor no es válido devuelve `fallback`. */
function safeArgb(hex: string, fallback: number): number {
  try {
    return argbFromHex(hex);
  } catch {
    return fallback;
  }
}
