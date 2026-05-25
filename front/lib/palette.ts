import {
  TonalPalette,
  Hct,
  Blend,
  Contrast,
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

/**
 * Tono mínimo (más claro u oscuro que `bgTone`) que alcanza al menos `ratio` de
 * contraste WCAG contra el fondo. Si es inalcanzable, satura al extremo (texto
 * blanco o negro). Así la legibilidad queda garantizada por construcción.
 */
function readableTone(bgTone: number, ratio: number, lighter: boolean): number {
  const t = lighter
    ? Contrast.lighter(bgTone, ratio)
    : Contrast.darker(bgTone, ratio);
  return t < 0 ? (lighter ? 100 : 0) : t;
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

  // Texto/iconos: tienen en cuenta AMBAS semillas. El matiz es una mezcla del
  // secundario armonizado hacia el primario (Blend.hctHue), con croma muy bajo
  // para que el tinte se note sin sacrificar legibilidad. El tono se calcula por
  // contraste — la legibilidad es lo primero.
  const textHue = Hct.fromInt(Blend.hctHue(secArgb, accentArgb, 0.4)).hue;
  const textPalette = TonalPalette.fromHueAndChroma(textHue, 5);
  // Fondo de referencia para el contraste: la superficie de contenido más
  // exigente (la más clara en oscuro / más oscura en claro), para que el texto
  // siga siendo legible también sobre tarjetas y estados hover.
  const refBgTone = clampTone(secTone + (dark ? 11 : -8));
  const readable = (ratio: number) => readableTone(refBgTone, ratio, dark);

  return {
    // Fondos — el secundario se respeta como base; el resto son escalones
    // tonales hacia el "frente" (más claros en oscuro, más oscuros en claro).
    "--color-bg": hexFromArgb(secArgb),
    "--color-bg-elevated": tone(neutral, secTone + (dark ? 4 : -3)),
    "--color-bg-card": tone(neutral, secTone + (dark ? 7 : -5)),
    "--color-bg-hover": tone(neutral, secTone + (dark ? 11 : -8)),
    "--color-border": tone(neutral, secTone + (dark ? 16 : -14)),

    // Textos/iconos — tinte mezcla de ambas semillas; tono por contraste WCAG:
    // AAA (>=7:1) para texto principal, AA (>=4.5:1) para secundario, y >=3:1
    // para sutil/iconos decorativos.
    "--color-text": tone(textPalette, readable(7)),
    "--color-text-muted": tone(textPalette, readable(4.5)),
    "--color-text-subtle": tone(textPalette, readable(3)),

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
