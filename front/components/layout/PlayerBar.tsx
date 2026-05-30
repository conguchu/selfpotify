"use client";

import { useEffect, useRef } from "react";
import { Pause, Play, Repeat1, SkipBack, SkipForward, Volume2, VolumeX } from "lucide-react";
import { CoverArt } from "@/components/music/CoverArt";
import { AddToPlaylistButton } from "@/components/music/AddToPlaylistButton";
import { IconButton } from "@/components/ui/IconButton";
import { Slider } from "@/components/ui/Slider";
import { usePlayerStore } from "@/lib/player/store";
import { useAuthStore } from "@/lib/auth/store";
import { buildAudioUrl } from "@/lib/api/streaming";
import { formatDuration, cn } from "@/lib/utils";

export function PlayerBar() {
  const audioRef = useRef<HTMLAudioElement | null>(null);
  // Mientras hay un seek en curso ignoramos los timeupdate, que aún reportan
  // la posición antigua y harían saltar la barra hacia atrás.
  const seekingRef = useRef(false);
  const token = useAuthStore((s) => s.token);
  const current = usePlayerStore((s) => s.current);
  const isPlaying = usePlayerStore((s) => s.isPlaying);
  const loop = usePlayerStore((s) => s.loop);
  const togglePlay = usePlayerStore((s) => s.togglePlay);
  const toggleLoop = usePlayerStore((s) => s.toggleLoop);
  const pause = usePlayerStore((s) => s.pause);
  const next = usePlayerStore((s) => s.next);
  const prev = usePlayerStore((s) => s.prev);
  const volume = usePlayerStore((s) => s.volume);
  const setVolume = usePlayerStore((s) => s.setVolume);
  const positionMs = usePlayerStore((s) => s.positionMs);
  const setPosition = usePlayerStore((s) => s.setPosition);
  const durationMs = usePlayerStore((s) => s.durationMs);
  const setDuration = usePlayerStore((s) => s.setDuration);

  // Al montar (primera carga de la app), garantizar que no hay reproducción automática.
  useEffect(() => {
    pause();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  // Cargar nueva canción cuando cambia
  useEffect(() => {
    const audio = audioRef.current;
    if (!audio) return;
    if (!current) {
      audio.removeAttribute("src");
      audio.load();
      return;
    }
    audio.src = buildAudioUrl(current.id, token);
    audio.load();
    if (isPlaying) {
      audio.play().catch(() => {
        /* navegador puede bloquear autoplay */
      });
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [current?.id, token]);

  // Play / pause sincronizado
  useEffect(() => {
    const audio = audioRef.current;
    if (!audio || !current) return;
    if (isPlaying) {
      audio.play().catch(() => {});
    } else {
      audio.pause();
    }
  }, [isPlaying, current]);

  // Volumen
  useEffect(() => {
    if (audioRef.current) {
      audioRef.current.volume = volume;
    }
  }, [volume]);

  const seek = (ms: number) => {
    const audio = audioRef.current;
    if (!audio) return;
    seekingRef.current = true;
    audio.currentTime = ms / 1000;
    setPosition(ms);
  };

  return (
    <footer
      className={cn(
        "z-30 flex h-20 shrink-0 items-center gap-4 border-t border-border bg-bg-elevated px-4",
      )}
    >
      <div className="flex w-72 min-w-0 items-center gap-3">
        {current ? (
          <>
            <CoverArt src={current.picture_url} alt={current.title} size="md" />
            <div className="min-w-0">
              <p className="truncate text-sm font-medium text-text">
                {current.title}
              </p>
              <p className="truncate text-xs text-text-muted">
                {[
                  current.artistNames?.length
                    ? current.artistNames.join(", ")
                    : null,
                  current.genre,
                ]
                  .filter(Boolean)
                  .join(" - ") || "—"}
              </p>
            </div>
            <AddToPlaylistButton songId={current.id} size="sm" className="shrink-0" />
          </>
        ) : (
          <p className="text-sm text-text-subtle">Sin reproducción</p>
        )}
      </div>

      <div className="flex flex-1 flex-col items-center gap-1.5">
        <div className="flex items-center gap-2">
          <IconButton
            label={loop ? "Desactivar bucle" : "Activar bucle"}
            variant="ghost"
            size="sm"
            disabled={!current}
            onClick={toggleLoop}
            className={
              loop
                ? "rounded-full bg-white text-black hover:bg-white/90"
                : "text-text-muted hover:text-text"
            }
          >
            <Repeat1 />
          </IconButton>
          <IconButton
            label="Anterior"
            variant="ghost"
            size="sm"
            disabled={!current}
            onClick={prev}
          >
            <SkipBack />
          </IconButton>
          <IconButton
            label={isPlaying ? "Pausar" : "Reproducir"}
            variant="solid"
            size="md"
            disabled={!current}
            onClick={togglePlay}
            className="bg-white !text-black hover:bg-white/90 [&>svg]:fill-black [&>svg]:text-black"
          >
            {isPlaying ? (
              <Pause fill="black" stroke="black" />
            ) : (
              <Play fill="black" stroke="black" />
            )}
          </IconButton>
          <IconButton
            label="Siguiente"
            variant="ghost"
            size="sm"
            disabled={!current}
            onClick={next}
          >
            <SkipForward />
          </IconButton>
        </div>
        <div className="flex w-full max-w-2xl items-center gap-2">
          <span className="w-10 text-right text-[11px] tabular-nums text-text-muted">
            {formatDuration(positionMs)}
          </span>
          <Slider
            ariaLabel="Posición"
            value={positionMs}
            max={durationMs > 0 ? durationMs : (current?.duration_ms ?? 1)}
            onChange={(v) => setPosition(v)}
            onCommit={seek}
            className="flex-1"
          />
          <span className="w-10 text-[11px] tabular-nums text-text-muted">
            {formatDuration(durationMs > 0 ? durationMs : current?.duration_ms ?? 0)}
          </span>
        </div>
      </div>

      <div className="flex w-44 items-center gap-2">
        <IconButton
          label={volume === 0 ? "Activar volumen" : "Silenciar"}
          variant="ghost"
          size="sm"
          onClick={() => setVolume(volume === 0 ? 0.8 : 0)}
        >
          {volume === 0 ? <VolumeX /> : <Volume2 />}
        </IconButton>
        <Slider
          ariaLabel="Volumen"
          value={Math.round(volume * 100)}
          max={100}
          onChange={(v) => setVolume(v / 100)}
        />
      </div>

      <audio
        ref={audioRef}
        preload="metadata"
        onTimeUpdate={(e) => {
          if (seekingRef.current) return;
          setPosition((e.currentTarget.currentTime || 0) * 1000);
        }}
        onSeeked={(e) => {
          seekingRef.current = false;
          setPosition((e.currentTarget.currentTime || 0) * 1000);
        }}
        onLoadedMetadata={(e) => {
          const d = e.currentTarget.duration;
          if (Number.isFinite(d) && d > 0) {
            setDuration(d * 1000);
          } else if (current) {
            setDuration(current.duration_ms);
          }
        }}
        onEnded={() => {
          if (loop) {
            const audio = audioRef.current;
            if (audio) {
              audio.currentTime = 0;
              audio.play().catch(() => {});
            }
            setPosition(0);
          } else {
            next();
          }
        }}
      />
    </footer>
  );
}
