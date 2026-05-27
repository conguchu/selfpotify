"use client";

import * as React from "react";

// Clave de la sección activa guardada por contenedor, a nivel de módulo:
// sobrevive al desmontaje del componente (p. ej. al navegar a /artist/[id] y
// volver) pero no a una recarga completa de la página.
const active = new Map<string, string>();

/** Desplazamiento de scroll de `child` dentro del contenedor con scroll. */
function offsetWithin(container: HTMLElement, child: HTMLElement) {
  return (
    child.getBoundingClientRect().top -
    container.getBoundingClientRect().top +
    container.scrollTop
  );
}

/**
 * Devuelve un `ref` para un contenedor con scroll propio (`overflow-y-auto`)
 * cuya sección visible se conserva entre montajes bajo `key`. Pensado para la
 * home, que se desmonta al navegar a un artista o género: al volver, el usuario
 * retoma la sección donde estaba en vez de saltar al principio.
 *
 * No guardamos el scroll en píxeles porque la home REORDENA sus secciones de
 * género en cada visita (al escuchar un género, este sube de posición), así que
 * un mismo offset acabaría cayendo sobre otra sección. En su lugar anclamos a la
 * IDENTIDAD de la sección: cada hija con `[data-scroll-key]` se identifica por
 * su clave, guardamos la que está visible y al volver desplazamos hasta esa
 * misma clave, esté donde esté ahora.
 *
 * El contenido llega de forma asíncrona (las queries del feed) y las secciones
 * aparecen tras el primer render, así que reintentamos restaurar a cada cambio
 * de tamaño hasta encontrar la sección. En cuanto el usuario hace scroll de
 * forma deliberada dejamos de restaurar y pasamos a registrar su sección.
 */
export function useScrollRestoration<T extends HTMLElement>(key: string) {
  const ref = React.useRef<T | null>(null);

  React.useLayoutEffect(() => {
    const el = ref.current;
    if (!el) return;

    const sections = () =>
      Array.from(el.querySelectorAll<HTMLElement>("[data-scroll-key]"));

    const target = active.get(key);
    let done = !target;
    let userMoved = false;

    // El contenedor usa `scroll-behavior: smooth`; al restaurar lo desactivamos
    // para saltar a la sección sin animación visible.
    const apply = () => {
      if (done || userMoved) return;
      const match = sections().find((s) => s.dataset.scrollKey === target);
      if (!match) return; // la sección aún no se ha renderizado: reintentaremos
      const prev = el.style.scrollBehavior;
      el.style.scrollBehavior = "auto";
      el.scrollTop = offsetWithin(el, match);
      el.style.scrollBehavior = prev;
      done = true;
    };
    apply();

    const ro = new ResizeObserver(() => apply());
    ro.observe(el);
    for (const child of Array.from(el.children)) ro.observe(child);

    // Guarda la sección cuyo borde superior está más cerca del scroll actual.
    const save = () => {
      let best: { k: string; d: number } | null = null;
      for (const s of sections()) {
        const k = s.dataset.scrollKey;
        if (!k) continue;
        const d = Math.abs(offsetWithin(el, s) - el.scrollTop);
        if (!best || d < best.d) best = { k, d };
      }
      if (best) active.set(key, best.k);
    };

    // Cualquier intención de scroll del usuario corta la restauración para no
    // pelear con él, y a partir de ahí guardamos su sección.
    const onUser = () => {
      userMoved = true;
      done = true;
    };
    const onScroll = () => {
      if (userMoved) save();
    };
    el.addEventListener("wheel", onUser, { passive: true });
    el.addEventListener("touchmove", onUser, { passive: true });
    el.addEventListener("keydown", onUser);
    el.addEventListener("scroll", onScroll, { passive: true });

    return () => {
      ro.disconnect();
      el.removeEventListener("wheel", onUser);
      el.removeEventListener("touchmove", onUser);
      el.removeEventListener("keydown", onUser);
      el.removeEventListener("scroll", onScroll);
      save();
    };
  }, [key]);

  return ref;
}
