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
 * - `primary`   → familia acento (--color-accent y variantes) por ROLES: tonos
 *   calibrados que conservan matiz/croma de la semilla y garantizan contraste
 *   como primer plano (iconos/enlaces sobre el fondo) y como fondo.
 * - `secondary` → fondos (--color-bg y variantes) y textos por contraste.
 *
 * `--color-danger` / `--color-success` son semánticos: se toman de `base` si
 * existen, o de los defaults rojo/verde.
 *
 * El color del texto SOBRE el acento (`--color-on-accent`) NO se devuelve aquí:
 * se calcula siempre a partir del acento con {@link onAccentFor} al aplicar el
 * tema, para que sea legible aunque el acento se edite a mano.
 *
 * Las claves devueltas coinciden con `ServerGlobalConfig.defaultColors()`.
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

/**
 * Tono de primer plano (texto/icono) legible SOBRE un fondo de tono `bgTone`.
 * Prefiere casi-blanco o casi-negro según cuál alcance >=4.5:1; si ninguno llega,
 * elige el de mayor contraste. Para el color "on-accent" de los botones.
 */
function onTone(bgTone: number): number {
  const light = 98;
  const dark = 12;
  const cl = Contrast.ratioOfTones(bgTone, light);
  const cd = Contrast.ratioOfTones(bgTone, dark);
  if (cl >= 4.5) return light;
  if (cd >= 4.5) return dark;
  return cl >= cd ? light : dark;
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

  const secTone = secHct.tone;
  // Tema oscuro si el fondo base es oscuro: los fondos suben de tono y el texto
  // es claro. En tema claro, todo se invierte.
  const dark = secTone < 50;

  // Acento por ROLES (como Material): en vez de usar el tono crudo de la semilla
  // —que no garantiza contraste ni como fondo ni como primer plano— fijamos
  // tonos calibrados que conservan matiz/croma. En tema oscuro el acento es claro
  // (resalta sobre fondos oscuros como icono/enlace) y `on-accent` queda oscuro;
  // en tema claro, al revés. Así iconos, enlaces, logo y botones combinan.
  const accentRoleTone = dark ? 80 : 45;

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

    // Acentos — tonos por rol que mantienen contraste contra el fondo (uso como
    // icono/enlace) y, con `on-accent`, contra el propio acento (texto sobre
    // botones). `accent-soft` es el contenedor tenue que se empareja con texto
    // de acento (badges, tiles).
    "--color-accent": tone(accent, accentRoleTone),
    "--color-accent-hover": tone(accent, dark ? 86 : 38),
    "--color-accent-active": tone(accent, dark ? 72 : 54),
    "--color-accent-soft": tone(accent, dark ? 30 : 90),

    // Semánticos — fijos (no se derivan), respetando lo que ya hubiera.
    "--color-danger": base?.["--color-danger"] ?? DEFAULT_DANGER,
    "--color-success": base?.["--color-success"] ?? DEFAULT_SUCCESS,
  };
}

/**
 * Color de texto/icono legible SOBRE un fondo de acento dado. Se calcula SIEMPRE
 * a partir del acento real (no se almacena), de modo que `--color-on-accent`
 * siga al acento aunque se edite a mano o provenga de una paleta antigua.
 */
export function onAccentFor(accentHex: string): string {
  const argb = safeArgb(accentHex, 0xffb91c1c);
  const hct = Hct.fromInt(argb);
  return tone(TonalPalette.fromInt(argb), onTone(hct.tone));
}

/** Convierte un hex a ARGB; si el valor no es válido devuelve `fallback`. */
function safeArgb(hex: string, fallback: number): number {
  try {
    return argbFromHex(hex);
  } catch {
    return fallback;
  }
}
