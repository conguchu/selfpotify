"use client";

import * as React from "react";
import useEmblaCarousel from "embla-carousel-react";
import { cn } from "@/lib/utils";

interface CoverflowProps<T> {
  items: T[];
  /** Render del contenido del slide. `isCenter` marca el slide activo (central). */
  renderItem: (
    item: T,
    state: { isCenter: boolean; index: number },
  ) => React.ReactNode;
  getKey: (item: T, index: number) => React.Key;
  /** Se invoca al hacer click/Enter sobre el slide central ya seleccionado. */
  onActivateCenter?: (item: T, index: number) => void;
  ariaLabel?: string;
  className?: string;
}

const clamp = (n: number, min: number, max: number) =>
  Math.min(max, Math.max(min, n));

/**
 * Carrusel horizontal estilo Cover Flow (macOS / Virtual DJ): el slide central
 * se muestra grande y nítido, mientras los laterales se encogen, se atenúan y
 * rotan en 3D hacia atrás. El efecto 3D se aplica a un wrapper externo a cada
 * slide para no romper el `position: relative` que necesita `<Image fill>`.
 *
 * Interacción: click en un lateral lo centra; click (o Enter/Espacio) en el
 * central dispara `onActivateCenter`. Respeta `prefers-reduced-motion`.
 */
export function Coverflow<T>({
  items,
  renderItem,
  getKey,
  onActivateCenter,
  ariaLabel,
  className,
}: CoverflowProps<T>) {
  const [emblaRef, emblaApi] = useEmblaCarousel({
    align: "center",
    containScroll: false,
    loop: false,
    skipSnaps: false,
  });
  const [selectedIndex, setSelectedIndex] = React.useState(0);
  const reducedMotion = useReducedMotion();
  const slideRefs = React.useRef<(HTMLDivElement | null)[]>([]);

  // Aplica las transformaciones 3D a cada slide según su distancia al snap activo.
  const applyStyles = React.useCallback(() => {
    if (!emblaApi) return;
    const progress = emblaApi.scrollProgress();
    const snaps = emblaApi.scrollSnapList();
    snaps.forEach((snap, i) => {
      const node = slideRefs.current[i];
      if (!node) return;
      const diff = snap - progress;
      const abs = Math.min(Math.abs(diff), 1);
      if (reducedMotion) {
        node.style.transform = "none";
        node.style.opacity = abs > 0.5 ? "0.55" : "1";
        node.style.zIndex = String(Math.round((1 - abs) * 100));
        return;
      }
      const scale = 1 - abs * 0.35;
      const rotateY = clamp(diff * -45, -55, 55);
      const translateZ = -abs * 120;
      const opacity = 1 - abs * 0.5;
      node.style.transform = `translateZ(${translateZ}px) rotateY(${rotateY}deg) scale(${scale})`;
      node.style.opacity = String(opacity);
      node.style.zIndex = String(Math.round((1 - abs) * 100));
    });
  }, [emblaApi, reducedMotion]);

  React.useEffect(() => {
    if (!emblaApi) return;
    const onSelect = () => setSelectedIndex(emblaApi.selectedScrollSnap());
    onSelect();
    applyStyles();
    emblaApi.on("select", onSelect);
    emblaApi.on("scroll", applyStyles);
    emblaApi.on("reInit", applyStyles);
    emblaApi.on("reInit", onSelect);
    return () => {
      emblaApi.off("select", onSelect);
      emblaApi.off("scroll", applyStyles);
      emblaApi.off("reInit", applyStyles);
      emblaApi.off("reInit", onSelect);
    };
  }, [emblaApi, applyStyles]);

  // Re-inicializa cuando cambian los datos (llegan de forma asíncrona).
  React.useEffect(() => {
    if (emblaApi) emblaApi.reInit();
  }, [emblaApi, items.length]);

  const handleSlideClick = (index: number) => {
    if (!emblaApi) return;
    if (index === selectedIndex) onActivateCenter?.(items[index], index);
    else emblaApi.scrollTo(index, reducedMotion);
  };

  const handleKeyDown = (e: React.KeyboardEvent) => {
    if (!emblaApi) return;
    if (e.key === "ArrowRight") {
      e.preventDefault();
      emblaApi.scrollNext();
    } else if (e.key === "ArrowLeft") {
      e.preventDefault();
      emblaApi.scrollPrev();
    } else if (e.key === "Enter" || e.key === " ") {
      e.preventDefault();
      onActivateCenter?.(items[selectedIndex], selectedIndex);
    }
  };

  return (
    <div
      ref={emblaRef}
      className={cn("h-full w-full overflow-hidden", className)}
      style={{ perspective: "1200px" }}
      role="listbox"
      aria-label={ariaLabel}
      tabIndex={0}
      onKeyDown={handleKeyDown}
    >
      <div
        className="flex h-full items-center"
        style={{ transformStyle: "preserve-3d" }}
      >
        {items.map((item, i) => {
          const isCenter = i === selectedIndex;
          return (
            <div
              key={getKey(item, i)}
              role="option"
              aria-selected={isCenter}
              className="relative flex min-w-0 shrink-0 grow-0 basis-[78%] items-center justify-center px-3 sm:basis-[58%] md:basis-[46%] lg:basis-[36%] xl:basis-[30%]"
            >
              <div
                ref={(el) => {
                  slideRefs.current[i] = el;
                }}
                onClick={() => handleSlideClick(i)}
                className="transform-gpu cursor-pointer select-none [backface-visibility:hidden] [will-change:transform]"
              >
                {renderItem(item, { isCenter, index: i })}
              </div>
            </div>
          );
        })}
      </div>
    </div>
  );
}

/** `true` si el usuario ha pedido reducir el movimiento en su sistema. */
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
