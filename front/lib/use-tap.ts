"use client";

import * as React from "react";

const TAP_THRESHOLD = 8;

/**
 * Props de puntero para un control (play, +) que vive dentro de un carrusel
 * arrastrable (Coverflow / Embla).
 *
 * Embla registra un listener de `click` en fase de captura sobre la raíz del
 * carrusel: tras un arrastre que supera su umbral marca `preventClick` y se
 * "come" (stopPropagation + preventDefault) el SIGUIENTE click en cualquier
 * punto del carrusel. Con ratón un arrastre no genera click, así que la bandera
 * persiste y acaba devorando el primer click real del usuario sobre un botón,
 * dejándolo aparentemente muerto tras desplazar el carrusel.
 *
 * Para esquivarlo disparamos la acción en `pointerup` (que Embla no intercepta)
 * siempre que el puntero no se haya arrastrado, absorbemos el `click` para que
 * no llegue a la columna del Coverflow (que centraría/activaría el slide) y
 * conservamos la activación por teclado (Enter/Espacio genera un click sin
 * puntero, con `detail === 0`).
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
