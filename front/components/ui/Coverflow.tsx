"use client";

import * as React from "react";
import { Swiper, SwiperSlide } from "swiper/react";
import { EffectCoverflow, Keyboard } from "swiper/modules";
import type { Swiper as SwiperType } from "swiper";
import { cn } from "@/lib/utils";
import "swiper/css";
import "swiper/css/effect-coverflow";

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

/**
 * Carrusel horizontal estilo Cover Flow usando Swiper con EffectCoverflow.
 * Soporta adición dinámica de slides sin reinicializar (ideal para paginación).
 *
 * Interacción: click en un lateral lo centra; click (o Enter/Espacio) en el
 * central dispara `onActivateCenter`. Pasar el ratón sobre un lateral lo
 * centra suavemente.
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
  const [activeIndex, setActiveIndex] = React.useState(0);
  const swiperRef = React.useRef<SwiperType | null>(null);

  const handleSlideChange = (swiper: SwiperType) => {
    const idx = swiper.realIndex;
    setActiveIndex(idx);
    onIndexChange?.(idx);
  };

  const handleClick = (swiper: SwiperType) => {
    const clickedIdx = swiper.clickedIndex;
    if (clickedIdx == null || clickedIdx < 0) return;
    if (clickedIdx !== swiper.activeIndex) {
      swiper.slideTo(clickedIdx);
      return;
    }
    const realIdx = swiper.realIndex;
    if (realIdx >= 0 && realIdx < items.length) {
      onActivateCenter?.(items[realIdx], realIdx);
    }
  };

  return (
    <div
      className={cn("h-full w-full", className)}
      role="listbox"
      aria-label={ariaLabel}
    >
      <Swiper
        modules={[EffectCoverflow, Keyboard]}
        effect="coverflow"
        centeredSlides
        slidesPerView="auto"
        loop={loop}
        coverflowEffect={{
          rotate: 45,
          stretch: 0,
          depth: 120,
          modifier: 1,
          slideShadows: false,
        }}
        keyboard={{ enabled: true, onlyInViewport: true }}
        onSwiper={(s) => {
          swiperRef.current = s;
        }}
        onSlideChange={handleSlideChange}
        onClick={handleClick}
        className="h-full w-full"
      >
        {items.map((item, i) => (
          <SwiperSlide
            key={getKey(item, i)}
            className="!w-[78%] cursor-pointer sm:!w-[58%] md:!w-[46%] lg:!w-[36%] xl:!w-[30%]"
            onMouseEnter={() => {
              const s = swiperRef.current;
              if (!s || i === activeIndex) return;
              s.slideToLoop(i);
            }}
          >
            {renderItem(item, { isCenter: i === activeIndex, index: i })}
          </SwiperSlide>
        ))}
      </Swiper>
    </div>
  );
}
