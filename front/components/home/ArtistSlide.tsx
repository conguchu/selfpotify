"use client";

import { Avatar } from "@/components/ui/Avatar";
import type { ArtistDTO } from "@/lib/types";
import { cn } from "@/lib/utils";

/**
 * Slide de artista para el coverflow del home. Presentacional: la navegación
 * a `/artist/{id}` la gestiona el `onActivateCenter` del Coverflow.
 */
export function ArtistSlide({
  artist,
  isCenter,
}: {
  artist: ArtistDTO;
  isCenter: boolean;
}) {
  return (
    <div className="flex flex-col items-center gap-4">
      <Avatar
        src={artist.photoUrl}
        alt={artist.name}
        size="lg"
        className={cn(
          "aspect-square h-auto w-full shadow-2xl ring-1 ring-border transition-shadow",
          isCenter && "ring-2 ring-accent",
        )}
      />
      <div className="w-full text-center">
        <p
          className={cn(
            "truncate text-base font-semibold",
            isCenter ? "text-text" : "text-text-muted",
          )}
          title={artist.name}
        >
          {artist.name}
        </p>
        <p className="text-xs text-text-muted">Artista</p>
      </div>
    </div>
  );
}
