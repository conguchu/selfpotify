"use client";

import * as React from "react";

const TAP_THRESHOLD = 8;

/**
 * Props de puntero para un control (play, +) que vive dentro de un carrusel
 * arrastrable (Coverflow).
 *
 * Disparamos la acción en `pointerup` siempre que el puntero no se haya
 * arrastrado y absorbemos el `click` para que no llegue al contenedor del
 * carrusel (que lo interpretaría como activación del slide). La activación
 * por teclado (Enter/Espacio genera un click con `detail === 0`) se conserva.
 */
export function useTapAction(action: () => void) {
  const downRef = React.useRef<{ x: number; y: number } | null>(null);
  return {
    onPointerDown: (e: React.PointerEvent) => {
      downRef.current = { x: e.clientX, y: e.clientY };
    },
    onPointerUp: (e: React.PointerEvent) => {
      const down = downRef.current;
      downRef.current = null;
      if (
        down &&
        Math.hypot(e.clientX - down.x, e.clientY - down.y) > TAP_THRESHOLD
      ) {
        return; // fue un arrastre del carrusel, no un toque del botón
      }
      e.stopPropagation();
      action();
    },
    onClick: (e: React.MouseEvent) => {
      e.stopPropagation();
      if (e.detail === 0) action(); // activación por teclado
    },
  };
}
