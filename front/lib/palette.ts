import { converter, formatHex, type Oklch } from "culori";

/**
 * Genera la paleta completa de 14 colores del tema a partir de dos semillas:
 * - `primary`   → familia acento (--color-accent y variantes).
 * - `secondary` → fondos (--color-bg y variantes) y, por contraste, los textos.
 *
 * Trabaja en OKLCH para que las rampas de luminosidad sean perceptualmente
 * uniformes. `--color-danger` / `--color-success` son semánticos: se toman de
 * `base` si existen, o de los defaults rojo/verde.
 *
 * Las claves devueltas coinciden exactamente con las que valida el backend
 * (ServerGlobalConfig.defaultColors()).
 */

const toOklch = converter("oklch");

const DEFAULT_DANGER = "#ef4444";
const DEFAULT_SUCCESS = "#16a34a";

function clamp(n: number, min: number, max: number): number {
  return Math.min(max, Math.max(min, n));
}

/** Construye un hex de 6 dígitos desde componentes OKLCH (clampeados a rango válido). */
function hex(l: number, c: number, h: number | undefined): string {
  const color: Oklch = {
    mode: "oklch",
    l: clamp(l, 0, 1),
    c: clamp(c, 0, 0.4),
    h,
  };
  // formatHex recorta a la gama sRGB y devuelve "#rrggbb".
  return formatHex(color);
}

export function derivePalette(
  primary: string,
  secondary: string,
  base?: Record<string, string>,
): Record<string, string> {
  const sec = toOklch(secondary) ?? { mode: "oklch", l: 0.04, c: 0, h: undefined };
  const acc = toOklch(primary) ?? { mode: "oklch", l: 0.45, c: 0.18, h: 25 };

  // Tema oscuro si el fondo base es oscuro: los escalones de fondo suben en L
  // y el texto es claro. En tema claro el signo se invierte.
  const dark = (sec.l ?? 0) < 0.5;
  const dir = dark ? 1 : -1;

  const sL = sec.l ?? 0.04;
  const sC = sec.c ?? 0;
  const sH = sec.h;

  // Croma muy bajo para neutros (fondos/textos teñidos con el hue del secundario).
  const neutralC = Math.min(sC, 0.02);

  const aL = acc.l ?? 0.45;
  const aC = acc.c ?? 0.18;
  const aH = acc.h;

  return {
    // Fondos — desde el secundario, subiendo (o bajando) la luminosidad.
    "--color-bg": hex(sL, neutralC, sH),
    "--color-bg-elevated": hex(sL + dir * 0.025, neutralC, sH),
    "--color-bg-card": hex(sL + dir * 0.04, neutralC, sH),
    "--color-bg-hover": hex(sL + dir * 0.065, neutralC, sH),
    "--color-border": hex(sL + dir * 0.09, neutralC, sH),

    // Textos — por contraste sobre el fondo (claros en tema oscuro y viceversa).
    "--color-text": dark ? hex(0.96, neutralC, sH) : hex(0.18, neutralC, sH),
    "--color-text-muted": dark ? hex(0.7, neutralC, sH) : hex(0.42, neutralC, sH),
    "--color-text-subtle": dark ? hex(0.48, neutralC, sH) : hex(0.6, neutralC, sH),

    // Acentos — desde el primario.
    "--color-accent": hex(aL, aC, aH),
    "--color-accent-hover": hex(aL + 0.06, aC, aH),
    "--color-accent-active": hex(aL - 0.06, aC, aH),
    "--color-accent-soft": hex(0.18, Math.min(aC, 0.08), aH),

    // Semánticos — fijos (no se derivan), respetando lo que ya hubiera.
    "--color-danger": base?.["--color-danger"] ?? DEFAULT_DANGER,
    "--color-success": base?.["--color-success"] ?? DEFAULT_SUCCESS,
  };
}
