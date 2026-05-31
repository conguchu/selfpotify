"use client";

import Link from "next/link";
import { Pause, Play } from "lucide-react";
import { CoverArt } from "@/components/music/CoverArt";
import { AddToPlaylistButton } from "@/components/music/AddToPlaylistButton";
import { IconButton } from "@/components/ui/IconButton";
import { usePlayerStore } from "@/lib/player/store";
import { useTapAction } from "@/lib/use-tap";
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
  // Empareja cada nombre con su id para poder enlazar a la página del artista
  // (sus canciones más escuchadas). Si faltan ids o nombres, no hay enlaces.
  const artists = (song.artistNames ?? []).map((name, i) => ({
    name,
    id: song.artistIds?.[i],
  }));

  // El botón de play/pause aparece en la carátula central y en la que suena.
  const showPlayButton = showPlayIndicator && (isCenter || isCurrent);

  // El toque se resuelve en `pointerup` —no en `click`— porque Embla devora el
  // primer click tras arrastrar el carrusel. Ver useTapAction.
  const playTap = useTapAction(() => {
    if (isCurrent) togglePlay();
    else onPlay?.();
  });

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
            {...playTap}
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
              ? "text-accent-text"
              : isCenter
                ? "text-text"
                : "text-text-muted",
          )}
          title={song.title}
        >
          {song.title}
        </p>
        <p
          className="truncate text-xs text-text-muted"
          title={artists.map((a) => a.name).join(", ") || "Artista desconocido"}
        >
          {artists.length > 0
            ? artists.map((a, i) => (
                <span key={`${a.id}-${i}`}>
                  {i > 0 && ", "}
                  {a.id != null ? (
                    <Link
                      href={`/artist/${a.id}`}
                      // pointer-events:auto vence al carrusel; stopPropagation
                      // evita que el tap de la carátula dispare reproducción.
                      onClick={(e) => e.stopPropagation()}
                      onPointerUp={(e) => e.stopPropagation()}
                      className="[pointer-events:auto] hover:text-text hover:underline"
                    >
                      {a.name}
                    </Link>
                  ) : (
                    a.name
                  )}
                </span>
              ))
            : "Artista desconocido"}
        </p>
      </div>
    </div>
  );
}
