/**
 * Presets de tema: pares (primario, secundario) curados. Son SEMILLAS — al
 * aplicarse pasan por `derivePalette`, que deriva los 14 colores garantizando
 * contraste WCAG, así que cualquier preset es accesible por construcción. Sirven
 * de punto de partida seguro para quien no quiere ajustar colores a mano; desde
 * uno se puede seguir afinando con los selectores primario/secundario.
 */
export interface ThemePreset {
  name: string;
  primary: string;
  secondary: string;
}

export const THEME_PRESETS: ThemePreset[] = [
  { name: "Selfpotify", primary: "#b91c1c", secondary: "#0a0a0a" },
  { name: "Esmeralda", primary: "#10b981", secondary: "#0b1410" },
  { name: "Océano", primary: "#3b82f6", secondary: "#0a0f1a" },
  { name: "Violeta", primary: "#8b5cf6", secondary: "#100a1a" },
  { name: "Ámbar", primary: "#f59e0b", secondary: "#14100a" },
  { name: "Rosa", primary: "#ec4899", secondary: "#160710" },
  { name: "Claro", primary: "#2563eb", secondary: "#f6f7f9" },
  { name: "Sepia", primary: "#b45309", secondary: "#faf6ef" },
];
