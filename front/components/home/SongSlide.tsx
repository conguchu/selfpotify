"use client";

import { Pause, Play } from "lucide-react";
import { CoverArt } from "@/components/music/CoverArt";
import { AddToPlaylistButton } from "@/components/music/AddToPlaylistButton";
import { usePlayerStore } from "@/lib/player/store";
import type { SongDTO } from "@/lib/types";
import { cn } from "@/lib/utils";

/**
 * Slide de canción para el coverflow del home. Presentacional: la reproducción
 * la dispara el `onActivateCenter` del Coverflow. Muestra el indicador de play
 * cuando es el slide central o cuando es la canción que suena ahora.
 *
 * `showPlayIndicator` se desactiva cuando el coverflow navega en vez de
 * reproducir (p. ej. el carrusel de un género), para no sugerir reproducción.
 */
export function SongSlide({
  song,
  isCenter,
  showPlayIndicator = true,
}: {
  song: SongDTO;
  isCenter: boolean;
  showPlayIndicator?: boolean;
}) {
  const current = usePlayerStore((s) => s.current);
  const isPlaying = usePlayerStore((s) => s.isPlaying);
  const isCurrent = current?.id === song.id;
  const showPause = isCurrent && isPlaying;
  const artistLabel = song.artistNames?.length
    ? song.artistNames.join(", ")
    : "Artista desconocido";

  return (
    <div className="flex flex-col items-center gap-4">
      <div className="relative w-full">
        <CoverArt
          src={song.picture_url}
          alt={song.title}
          size="xl"
          rounded="lg"
          className={cn(
            "aspect-square h-auto w-full shadow-2xl transition-shadow",
            isCenter && "ring-2 ring-accent",
          )}
        />
        {isCenter && (
          <AddToPlaylistButton
            songId={song.id}
            size="md"
            className="absolute left-3 top-3 [pointer-events:auto] bg-black/50 text-white backdrop-blur hover:bg-black/70 hover:text-white"
          />
        )}
        {showPlayIndicator && (isCenter || isCurrent) && (
          <span
            className={cn(
              "absolute bottom-3 right-3 flex h-14 w-14 items-center justify-center rounded-full",
              "bg-accent text-on-accent shadow-xl",
            )}
            aria-hidden="true"
          >
            {showPause ? (
              <Pause className="h-6 w-6" fill="currentColor" />
            ) : (
              <Play className="h-6 w-6" fill="currentColor" />
            )}
          </span>
        )}
      </div>
      <div className="w-full text-center">
        <p
          className={cn(
            "truncate text-base font-semibold",
            isCurrent
              ? "text-accent-hover"
              : isCenter
                ? "text-text"
                : "text-text-muted",
          )}
          title={song.title}
        >
          {song.title}
        </p>
        <p className="truncate text-xs text-text-muted" title={artistLabel}>
          {artistLabel}
        </p>
      </div>
    </div>
  );
}
