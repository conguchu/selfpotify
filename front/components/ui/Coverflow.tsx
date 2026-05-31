"use client";

import * as React from "react";
// @splidejs/react-splide@0.7.12 no expone sus tipos en `exports` de package.json
// (incompatible con moduleResolution:bundler), así que suprimimos el error de
// resolución. El bundler (webpack/Next.js) resuelve el módulo desde node_modules
// con normalidad; solo el type-checker no lo encuentra.
// eslint-disable-next-line @typescript-eslint/ban-ts-comment
// @ts-expect-error
import { Splide, SplideSlide } from "@splidejs/react-splide";
import "@splidejs/react-splide/css/core";
import { cn } from "@/lib/utils";

/** Superficie mínima de la instancia de Splide que usamos en este componente. */
interface SplideInstance {
  root: HTMLElement;
  go: (control: number | string) => void;
}

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
  /** Se invoca cada vez que cambia el slide seleccionado. */
  onIndexChange?: (index: number) => void;
  /** Si es false, el carrusel no hace bucle al llegar al final. Por defecto true. */
  loop?: boolean;
  ariaLabel?: string;
  className?: string;
}

const clamp = (n: number, min: number, max: number) =>
  Math.min(max, Math.max(min, n));

/**
 * Carrusel horizontal estilo Cover Flow usando Splide como motor de
 * drag/snap y transforms 3D aplicados frame a frame vía getBoundingClientRect
 * sobre el wrapper 2D exterior (fuera del contexto preserve-3d).
 *
 * Splide soporta adición dinámica de slides mediante MutationObserver interno,
 * sin reInit ni salto de posición.
 *
 * Interacción: click en un lateral lo centra; click en el central dispara
 * `onActivateCenter`. Pasar el ratón sobre un lateral lo centra. Teclado con
 * flechas/Enter/Espacio. Respeta `prefers-reduced-motion`.
 */
export function Coverflow<T>({
  items,
  renderItem,
  getKey,
  onActivateCenter,
  onIndexChange,
  loop = true,
  ariaLabel,
  className,
}: CoverflowProps<T>) {
  const splideRef = React.useRef<SplideInstance | null>(null);
  const [selectedIndex, setSelectedIndex] = React.useState(0);
  const slideRefs = React.useRef<(HTMLDivElement | null)[]>([]);
  const reducedMotion = useReducedMotion();
  const pointerDownRef = React.useRef<{ x: number; y: number } | null>(null);
  // RAF para actualizar los transforms durante drag y animación.
  const isAnimatingRef = React.useRef(false);
  const rafIdRef = React.useRef<number | null>(null);

  // Calcula y aplica los transforms 3D a cada slide según su posición visual
  // actual. Se basa en getBoundingClientRect para ser preciso durante drag.
  const applyStyles = React.useCallback(() => {
    const splide = splideRef.current;
    if (!splide?.root) return;
    const containerRect = splide.root.getBoundingClientRect();
    const containerCenter = containerRect.left + containerRect.width / 2;

    slideRefs.current.forEach((node, i) => {
      if (!node) return;
      const rect = node.getBoundingClientRect();
      const slideCenter = rect.left + rect.width / 2;
      // Diferencia normalizada respecto al ancho del contenedor, en [-1.5, 1.5].
      const diff = clamp(
        (slideCenter - containerCenter) / containerRect.width,
        -1.5,
        1.5,
      );
      const abs = Math.min(Math.abs(diff), 1);

      // El <li> (abuelo del inner div, padre del wrapper de perspectiva) recibe
      // z-index para controlar el orden de apilamiento 2D: el central queda
      // delante sin provocar oclusión 3D que bloquee los eventos de puntero.
      // Los <li> son flex items: z-index funciona sin position:relative (CSS Flexbox).
      const li = node.parentElement?.parentElement;
      if (li) li.style.zIndex = String(Math.round((1 - abs) * 10));

      if (reducedMotion) {
        node.style.transform = "none";
        node.style.opacity = abs > 0.5 ? "0.55" : "1";
        return;
      }
      const scale = 1 - abs * 0.35;
      const rotateY = clamp(diff * -45, -55, 55);
      const translateZ = -abs * 120;
      const opacity = 1 - abs * 0.5;
      node.style.transform = `translateZ(${translateZ}px) rotateY(${rotateY}deg) scale(${scale})`;
      node.style.opacity = String(opacity);
    });
  }, [reducedMotion]);

  // Arranca el bucle RAF para actualizar frames durante drag/animación.
  const startRAF = React.useCallback(() => {
    isAnimatingRef.current = true;
    if (rafIdRef.current) return;
    const tick = () => {
      applyStyles();
      if (isAnimatingRef.current) {
        rafIdRef.current = requestAnimationFrame(tick);
      } else {
        rafIdRef.current = null;
        applyStyles(); // frame final cuando la animación se detiene
      }
    };
    rafIdRef.current = requestAnimationFrame(tick);
  }, [applyStyles]);

  const stopRAF = React.useCallback(() => {
    isAnimatingRef.current = false;
    // el bucle se auto-termina en el siguiente frame
  }, []);

  const handleSlideClick = (e: React.MouseEvent, index: number) => {
    const down = pointerDownRef.current;
    if (down && Math.hypot(e.clientX - down.x, e.clientY - down.y) > 8) return;
    if (index !== selectedIndex) splideRef.current?.go(index);
    onActivateCenter?.(items[index], index);
  };

  const handleKeyDown = (e: React.KeyboardEvent) => {
    const s = splideRef.current;
    if (!s) return;
    if (e.key === "ArrowRight") { e.preventDefault(); s.go(">"); }
    else if (e.key === "ArrowLeft") { e.preventDefault(); s.go("<"); }
    else if (e.key === "Enter" || e.key === " ") {
      e.preventDefault();
      onActivateCenter?.(items[selectedIndex], selectedIndex);
    }
  };

  return (
    <div
      className={cn("h-full w-full overflow-hidden", className)}
      style={{ perspective: "1200px" }}
      role="listbox"
      aria-label={ariaLabel}
      tabIndex={0}
      onKeyDown={handleKeyDown}
      onPointerDownCapture={(e) => {
        pointerDownRef.current = { x: e.clientX, y: e.clientY };
      }}
    >
      <Splide
        options={{
          type: loop ? "loop" : "slide",
          focus: "center",
          autoWidth: true,
          trimSpace: false,
          arrows: false,
          pagination: false,
        }}
        onMounted={(splide: SplideInstance) => {
          splideRef.current = splide;
          applyStyles();
        }}
        onMove={(_splide: SplideInstance, index: number) => {
          setSelectedIndex(index);
          onIndexChange?.(index);
          startRAF();
        }}
        onMoved={stopRAF}
        onDrag={startRAF}
        onDragged={stopRAF}
        className="h-full w-full"
      >
        {items.map((item, i) => (
          <SplideSlide
            key={getKey(item, i)}
            onClick={(e: React.MouseEvent<HTMLLIElement>) => handleSlideClick(e, i)}
            onPointerEnter={(e: React.PointerEvent<HTMLLIElement>) => {
              if (e.pointerType !== "mouse" || e.buttons !== 0) return;
              if (i !== selectedIndex) splideRef.current?.go(i);
            }}
            className="w-[78%] cursor-pointer select-none px-3 sm:w-[58%] md:w-[46%] lg:w-[36%] xl:w-[30%]"
          >
            {/* Wrapper con perspectiva local: cada slide tiene su propio punto de
                fuga. Así los transforms 3D del inner div funcionan con perspectiva
                sin necesitar preserve-3d en .splide__list (que causaba oclusión
                3D y bloqueaba clics en los slides del extremo). */}
            <div className="h-full w-full" style={{ perspective: "1200px" }}>
              <div
                ref={(el) => {
                  slideRefs.current[i] = el;
                }}
                className="w-full transform-gpu [backface-visibility:hidden] [pointer-events:none] [will-change:transform]"
              >
                {renderItem(item, { isCenter: i === selectedIndex, index: i })}
              </div>
            </div>
          </SplideSlide>
        ))}
      </Splide>
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
