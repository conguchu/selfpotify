"use client";

import * as React from "react";
import { cn } from "@/lib/utils";

const clamp = (n: number, min: number, max: number) => Math.min(max, Math.max(min, n));

interface CoverflowProps<T> {
  items: T[];
  renderItem: (item: T, state: { isCenter: boolean; index: number }) => React.ReactNode;
  getKey: (item: T, index: number) => React.Key;
  onActivateCenter?: (item: T, index: number) => void;
  onIndexChange?: (index: number) => void;
  /** Mantenida por compatibilidad; el scroll nativo no implementa bucle infinito. */
  loop?: boolean;
  ariaLabel?: string;
  className?: string;
}

/**
 * Carrusel estilo Cover Flow sin dependencias externas.
 *
 * Motor: scroll nativo con scroll-snap (CSS) y scrollIntoView para la
 * navegación programática. scrollIntoView actualiza el destino al vuelo
 * si se llama varias veces seguidas, por lo que el hover rápido siempre
 * alcanza el slide correcto sin cascadas ni llamadas ignoradas.
 *
 * 3D: transforms calculados frame a frame vía getBoundingClientRect + RAF.
 * Drag de ratón: pointer events con setPointerCapture sobre el track.
 * Adición dinámica de slides: solo requiere añadir el elemento al DOM.
 */
export function Coverflow<T>({
  items,
  renderItem,
  getKey,
  onActivateCenter,
  onIndexChange,
  ariaLabel,
  className,
}: CoverflowProps<T>) {
  const outerRef = React.useRef<HTMLDivElement>(null);
  const trackRef = React.useRef<HTMLDivElement>(null);
  const slideRefs = React.useRef<(HTMLDivElement | null)[]>([]);
  const innerRefs = React.useRef<(HTMLDivElement | null)[]>([]);

  const [selectedIndex, setSelectedIndex] = React.useState(0);
  const selectedIndexRef = React.useRef(0);

  // Refs estables para evitar re-crear callbacks cuando cambian las props.
  const onIndexChangeRef = React.useRef(onIndexChange);
  React.useEffect(() => { onIndexChangeRef.current = onIndexChange; }, [onIndexChange]);
  const onActivateCenterRef = React.useRef(onActivateCenter);
  React.useEffect(() => { onActivateCenterRef.current = onActivateCenter; }, [onActivateCenter]);

  const reducedMotion = useReducedMotion();
  const reducedMotionRef = React.useRef(reducedMotion);
  React.useEffect(() => { reducedMotionRef.current = reducedMotion; }, [reducedMotion]);

  const pointerDownRef = React.useRef<{ x: number; y: number } | null>(null);
  const dragRef = React.useRef({ active: false, startX: 0, scrollLeft: 0 });
  const rafIdRef = React.useRef<number | null>(null);
  const isScrollingRef = React.useRef(false);

  // ── 3D transforms ─────────────────────────────────────────────────────────

  const applyStyles = React.useCallback(() => {
    const outer = outerRef.current;
    if (!outer) return;
    const containerRect = outer.getBoundingClientRect();
    const centerX = containerRect.left + containerRect.width / 2;

    slideRefs.current.forEach((slide, i) => {
      if (!slide) return;
      const inner = innerRefs.current[i];
      if (!inner) return;
      const rect = slide.getBoundingClientRect();
      const slideCenter = rect.left + rect.width / 2;
      const diff = clamp((slideCenter - centerX) / containerRect.width, -1.5, 1.5);
      const abs = Math.min(Math.abs(diff), 1);

      // z-index 2D para que el slide central quede encima sin oclusión 3D.
      // Los slides son flex items: z-index funciona sin position:relative.
      slide.style.zIndex = String(Math.round((1 - abs) * 10));

      if (reducedMotionRef.current) {
        inner.style.transform = "none";
        inner.style.opacity = abs > 0.5 ? "0.55" : "1";
        return;
      }
      inner.style.transform = `translateZ(${-abs * 120}px) rotateY(${clamp(diff * -45, -55, 55)}deg) scale(${1 - abs * 0.35})`;
      inner.style.opacity = String(1 - abs * 0.5);
    });
  }, []);

  const startRAF = React.useCallback(() => {
    isScrollingRef.current = true;
    if (rafIdRef.current) return;
    const tick = () => {
      applyStyles();
      if (isScrollingRef.current) {
        rafIdRef.current = requestAnimationFrame(tick);
      } else {
        rafIdRef.current = null;
        applyStyles(); // frame final tras detener el scroll
      }
    };
    rafIdRef.current = requestAnimationFrame(tick);
  }, [applyStyles]);

  const stopRAF = React.useCallback(() => {
    isScrollingRef.current = false;
  }, []);

  // ── detección del slide central ───────────────────────────────────────────

  const detectCenter = React.useCallback(() => {
    const track = trackRef.current;
    if (!track) return;
    const trackCenter = track.scrollLeft + track.clientWidth / 2;
    let closest = 0;
    let minDist = Infinity;
    slideRefs.current.forEach((slide, i) => {
      if (!slide) return;
      const dist = Math.abs(slide.offsetLeft + slide.clientWidth / 2 - trackCenter);
      if (dist < minDist) { minDist = dist; closest = i; }
    });
    if (closest !== selectedIndexRef.current) {
      selectedIndexRef.current = closest;
      setSelectedIndex(closest);
      onIndexChangeRef.current?.(closest);
    }
  }, []);

  // ── inicialización y cambios en items ────────────────────────────────────

  React.useEffect(() => {
    const track = trackRef.current;
    if (!track) return;
    // Gestión imperativa para poder alternar durante el drag sin conflicto
    // con la prop style de React.
    track.style.scrollSnapType = "x mandatory";
    applyStyles();
  }, [applyStyles]);

  React.useEffect(() => {
    slideRefs.current = slideRefs.current.slice(0, items.length);
    innerRefs.current = innerRefs.current.slice(0, items.length);
    applyStyles();
  }, [items.length, applyStyles]);

  // ── eventos de scroll ─────────────────────────────────────────────────────

  React.useEffect(() => {
    const track = trackRef.current;
    if (!track) return;

    let fallbackTimer: ReturnType<typeof setTimeout>;

    const onScroll = () => {
      startRAF();
      detectCenter();
      // Fallback para navegadores sin soporte de scrollend (< Safari 17.4).
      clearTimeout(fallbackTimer);
      fallbackTimer = setTimeout(() => { stopRAF(); detectCenter(); }, 150);
    };
    const onScrollEnd = () => {
      clearTimeout(fallbackTimer);
      stopRAF();
      detectCenter();
    };

    track.addEventListener("scroll", onScroll, { passive: true });
    track.addEventListener("scrollend", onScrollEnd, { passive: true });
    return () => {
      track.removeEventListener("scroll", onScroll);
      track.removeEventListener("scrollend", onScrollEnd);
      clearTimeout(fallbackTimer);
    };
  }, [startRAF, stopRAF, detectCenter]);

  // ── navegación ────────────────────────────────────────────────────────────

  const goTo = React.useCallback((index: number) => {
    const slide = slideRefs.current[index];
    if (!slide) return;
    // block: nearest evita scroll vertical si el componente no está 100% visible.
    slide.scrollIntoView({ behavior: "smooth", inline: "center", block: "nearest" });
  }, []);

  // ── drag de ratón ─────────────────────────────────────────────────────────

  const handlePointerDown = (e: React.PointerEvent<HTMLDivElement>) => {
    pointerDownRef.current = { x: e.clientX, y: e.clientY };
    if (e.pointerType !== "mouse") return;
    const track = trackRef.current;
    if (!track) return;
    track.style.scrollSnapType = "none"; // evita snap durante el arrastre
    dragRef.current = { active: true, startX: e.clientX, scrollLeft: track.scrollLeft };
    track.setPointerCapture(e.pointerId);
  };

  const handlePointerMove = (e: React.PointerEvent<HTMLDivElement>) => {
    if (!dragRef.current.active) return;
    const track = trackRef.current;
    if (track) track.scrollLeft = dragRef.current.scrollLeft - (e.clientX - dragRef.current.startX);
  };

  const handlePointerUp = (e: React.PointerEvent<HTMLDivElement>) => {
    if (!dragRef.current.active) return;
    dragRef.current.active = false;
    const track = trackRef.current;
    if (!track) return;
    track.style.scrollSnapType = "x mandatory";
    detectCenter();
    goTo(selectedIndexRef.current);
  };

  // ── interacciones de slide ────────────────────────────────────────────────

  const handleSlideClick = (e: React.MouseEvent, i: number) => {
    const down = pointerDownRef.current;
    if (down && Math.hypot(e.clientX - down.x, e.clientY - down.y) > 8) return;
    if (i !== selectedIndexRef.current) {
      goTo(i);
    } else {
      onActivateCenterRef.current?.(items[i], i);
    }
  };

  const handleSlidePointerEnter = (e: React.PointerEvent, i: number) => {
    if (e.pointerType !== "mouse" || e.buttons !== 0) return;
    if (i !== selectedIndexRef.current) goTo(i);
  };

  // ── teclado ───────────────────────────────────────────────────────────────

  const handleKeyDown = (e: React.KeyboardEvent) => {
    if (e.key === "ArrowRight") {
      e.preventDefault();
      goTo(Math.min(selectedIndexRef.current + 1, items.length - 1));
    } else if (e.key === "ArrowLeft") {
      e.preventDefault();
      goTo(Math.max(selectedIndexRef.current - 1, 0));
    } else if (e.key === "Enter" || e.key === " ") {
      e.preventDefault();
      onActivateCenterRef.current?.(items[selectedIndexRef.current], selectedIndexRef.current);
    }
  };

  // ── render ────────────────────────────────────────────────────────────────

  return (
    <div
      ref={outerRef}
      className={cn("h-full w-full overflow-hidden", className)}
      style={{ perspective: "1200px" }}
      role="listbox"
      aria-label={ariaLabel}
      tabIndex={0}
      onKeyDown={handleKeyDown}
    >
      <div
        ref={trackRef}
        className="flex h-full w-full cursor-grab items-center overflow-x-auto active:cursor-grabbing [scrollbar-width:none] [&::-webkit-scrollbar]:hidden"
        onPointerDown={handlePointerDown}
        onPointerMove={handlePointerMove}
        onPointerUp={handlePointerUp}
        onPointerCancel={handlePointerUp}
      >
        {/*
          Espaciadores para que el primer y el último slide puedan centrarse.
          Ancho = (100% - ancho_del_slide) / 2, sincronizado con los breakpoints
          de los slides (78% → 58% → 46% → 36% → 30%).
        */}
        <div
          className="w-[11%] shrink-0 sm:w-[21%] md:w-[27%] lg:w-[32%] xl:w-[35%]"
          aria-hidden
        />

        {items.map((item, i) => (
          <div
            key={getKey(item, i)}
            ref={(el) => { slideRefs.current[i] = el; }}
            className="w-[78%] shrink-0 cursor-pointer select-none px-3 sm:w-[58%] md:w-[46%] lg:w-[36%] xl:w-[30%]"
            style={{ scrollSnapAlign: "center" }}
            role="option"
            aria-selected={i === selectedIndex}
            onClick={(e) => handleSlideClick(e, i)}
            onPointerEnter={(e) => handleSlidePointerEnter(e, i)}
          >
            {/* Perspectiva local por slide: cada carátula tiene su propio
                punto de fuga sin compartir contexto 3D con los vecinos,
                lo que evita la oclusión 3D en los hit-tests de puntero. */}
            <div style={{ perspective: "1200px" }}>
              <div
                ref={(el) => { innerRefs.current[i] = el; }}
                className="w-full transform-gpu [backface-visibility:hidden] [pointer-events:none] [will-change:transform]"
              >
                {renderItem(item, { isCenter: i === selectedIndex, index: i })}
              </div>
            </div>
          </div>
        ))}

        <div
          className="w-[11%] shrink-0 sm:w-[21%] md:w-[27%] lg:w-[32%] xl:w-[35%]"
          aria-hidden
        />
      </div>
    </div>
  );
}

function useReducedMotion() {
  const [reduced, setReduced] = React.useState(false);
  React.useEffect(() => {
    const mq = window.matchMedia("(prefers-reduced-motion: reduce)");
    setReduced(mq.matches);
    const onChange = () => setReduced(mq.matches);
    mq.addEventListener("change", onChange);
    return () => mq.removeEventListener("change", onChange);
  }, []);
  return reduced;
}
