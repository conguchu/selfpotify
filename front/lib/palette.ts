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

  // El acento se usa sobre todo como FONDO de botón, así que debe PARECERSE al
  // color elegido (un rojo vivo da un botón rojo, no pastel). Seguimos el tono
  // de la semilla pero acotado a [36, 60]: nunca tan claro que quede pastel ni
  // tan oscuro que se pierda. El contraste del texto encima lo garantiza aparte
  // `on-accent` (ver onAccentFor), que elige claro u oscuro según el acento.
  const accentSeedTone = Hct.fromInt(accentArgb).tone;
  const accentRoleTone = clampTone(Math.min(60, Math.max(36, accentSeedTone)));

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
    "--color-accent-hover": tone(accent, clampTone(accentRoleTone + 8)),
    "--color-accent-active": tone(accent, clampTone(accentRoleTone - 8)),
    "--color-accent-soft": tone(accent, dark ? 26 : 90),

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

/**
 * Color del acento usable como PRIMER PLANO (iconos, enlaces, estados activos)
 * SOBRE el fondo. El `--color-accent` se calibra para lucir como FONDO de botón
 * (vivo, parecido al color elegido), y por eso no garantiza contraste contra el
 * fondo de la página: un acento vivo sobre un fondo del mismo tono da iconos que
 * no se ven. Aquí conservamos matiz y croma del acento pero llevamos el TONO al
 * mínimo que alcanza AA (4.5:1) contra el fondo, de modo que el color siga
 * "siendo el acento" (un rojo sigue rojo) pero legible. Se calcula SIEMPRE al
 * aplicar (no se almacena), igual que {@link onAccentFor}, para seguir al acento
 * y al fondo aunque se editen a mano. Alimenta `--color-accent-text`.
 */
export function accentTextFor(accentHex: string, bgHex: string): string {
  const accentArgb = safeArgb(accentHex, 0xffb91c1c);
  const bgTone = Hct.fromInt(safeArgb(bgHex, 0xff0a0a0a)).tone;
  const accentHct = Hct.fromInt(accentArgb);
  const dark = bgTone < 50;
  const t = readableTone(bgTone, 4.5, dark);
  return tone(TonalPalette.fromHueAndChroma(accentHct.hue, accentHct.chroma), t);
}

// Contraste mínimo exigido a cada color de texto contra la superficie de
// contenido más exigente. `text` cumple AA (4.5:1); `muted`/`subtle` se dejan
// más tenues a propósito, pero nunca por debajo de 3:1 (siguen siendo visibles).
const TEXT_FLOORS: Record<string, number> = {
  "--color-text": 4.5,
  "--color-text-muted": 3,
  "--color-text-subtle": 3,
};

/**
 * Red de seguridad de accesibilidad: dado un mapa de colores cualquiera (incluido
 * uno editado a mano en "Avanzado" o heredado de una config antigua), devuelve
 * una copia donde cada color de texto que NO alcance su contraste mínimo contra
 * el fondo se reemplaza por el tono legible más cercano, conservando su matiz y
 * croma. Los colores que ya cumplen se dejan intactos, así que no "aplana" un
 * tema bien derivado; solo corrige los que romperían la legibilidad. Se aplica
 * tanto en el preview de los editores como al pintar la app real
 * ({@link applyTheme}-style en ThemeApplier), por lo que la app NUNCA renderiza
 * texto ilegible, da igual lo que haya guardado.
 */
export function enforceContrast(
  colors: Record<string, string>,
): Record<string, string> {
  const bg = colors["--color-bg"];
  if (!bg) return { ...colors };

  const bgTone = Hct.fromInt(safeArgb(bg, 0xff0a0a0a)).tone;
  const dark = bgTone < 50;

  // Superficie de contenido más exigente: en oscuro la más clara, en claro la
  // más oscura (la más próxima al texto en tono, que es la que peor contrasta).
  const surfaceTones = [
    "--color-bg",
    "--color-bg-elevated",
    "--color-bg-card",
    "--color-bg-hover",
  ]
    .map((k) => colors[k])
    .filter(Boolean)
    .map((hex) => Hct.fromInt(safeArgb(hex, 0xff0a0a0a)).tone);
  const refTone = surfaceTones.length
    ? dark
      ? Math.max(...surfaceTones)
      : Math.min(...surfaceTones)
    : bgTone;

  const out = { ...colors };
  for (const [key, ratio] of Object.entries(TEXT_FLOORS)) {
    const value = colors[key];
    if (!value) continue;
    const hct = Hct.fromInt(safeArgb(value, 0xfff5f5f5));
    if (Contrast.ratioOfTones(refTone, hct.tone) >= ratio) continue;
    const t = readableTone(refTone, ratio, dark);
    out[key] = tone(TonalPalette.fromHueAndChroma(hct.hue, hct.chroma), t);
  }
  return out;
}

/** Convierte un hex a ARGB; si el valor no es válido devuelve `fallback`. */
function safeArgb(hex: string, fallback: number): number {
  try {
    return argbFromHex(hex);
  } catch {
    return fallback;
  }
}
