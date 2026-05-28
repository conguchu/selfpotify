"use client";

import * as React from "react";

/**
 * Devuelve `value` retrasado por `delayMs`. Cada vez que `value` cambia se
 * reinicia el temporizador, así que el valor expuesto solo se actualiza cuando
 * el de entrada lleva quieto al menos `delayMs` milisegundos.
 *
 * Pensado para acoplarlo a inputs de búsqueda: evita disparar una llamada al
 * backend por cada tecla manteniendo la UI reactiva (el input es controlado,
 * solo el "valor estable" se debouncea).
 */
export function useDebouncedValue<T>(value: T, delayMs: number): T {
  const [debounced, setDebounced] = React.useState(value);
  React.useEffect(() => {
    const id = setTimeout(() => setDebounced(value), delayMs);
    return () => clearTimeout(id);
  }, [value, delayMs]);
  return debounced;
}
