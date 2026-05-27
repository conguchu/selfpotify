"use client";

import { Pause, Play } from "lucide-react";
import { CoverArt } from "@/components/music/CoverArt";
import { AddToPlaylistButton } from "@/components/music/AddToPlaylistButton";
import { IconButton } from "@/components/ui/IconButton";
import { usePlayerStore } from "@/lib/player/store";
import type { SongDTO } from "@/lib/types";
import { cn } from "@/lib/utils";

/**
 * Slide de canción para el coverflow del home. La reproducción se dispara
 * SOLO con el botón de play de la carátula (no al pulsar la carátula). Para la
 * canción que ya suena, ese botón muestra y actúa como pausa.
 *
 * `showPlayIndicator` se desactiva cuando el coverflow navega en vez de
 * reproducir (p. ej. el carrusel de un género): en ese caso no hay botón ni
 * `onPlay`.
 */
export function SongSlide({
  song,
  isCenter,
  showPlayIndicator = true,
  onPlay,
}: {
  song: SongDTO;
  isCenter: boolean;
  showPlayIndicator?: boolean;
  /** Inicia la reproducción de esta canción (con su cola). Lo aporta el padre. */
  onPlay?: () => void;
}) {
  const current = usePlayerStore((s) => s.current);
  const isPlaying = usePlayerStore((s) => s.isPlaying);
  const togglePlay = usePlayerStore((s) => s.togglePlay);
  const isCurrent = current?.id === song.id;
  const showPause = isCurrent && isPlaying;
  const artistLabel = song.artistNames?.length
    ? song.artistNames.join(", ")
    : "Artista desconocido";

  // El botón de play/pause aparece en la carátula central y en la que suena.
  const showPlayButton = showPlayIndicator && (isCenter || isCurrent);

  const handlePlay = (e: React.MouseEvent) => {
    // No dejamos que el click llegue a la columna del Coverflow (que centra).
    e.stopPropagation();
    if (isCurrent) togglePlay();
    else onPlay?.();
  };

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
        {showPlayButton && (
          <IconButton
            label={showPause ? "Pausar" : "Reproducir"}
            variant="accent"
            size="lg"
            onClick={handlePlay}
            className="absolute bottom-3 right-3 h-14 w-14 [pointer-events:auto] shadow-xl [&>svg]:h-6 [&>svg]:w-6"
          >
            {showPause ? (
              <Pause fill="currentColor" />
            ) : (
              <Play fill="currentColor" />
            )}
          </IconButton>
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
