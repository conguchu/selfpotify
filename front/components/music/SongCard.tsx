"use client";

import { Pause, Play } from "lucide-react";
import { CoverArt } from "./CoverArt";
import { IconButton } from "@/components/ui/IconButton";
import { usePlayerStore } from "@/lib/player/store";
import type { SongDTO } from "@/lib/types";
import { cn } from "@/lib/utils";

export function SongCard({
  song,
  onPlay,
  className,
}: {
  song: SongDTO;
  onPlay: () => void;
  className?: string;
}) {
  const current = usePlayerStore((s) => s.current);
  const isPlaying = usePlayerStore((s) => s.isPlaying);
  const togglePlay = usePlayerStore((s) => s.togglePlay);
  const isCurrent = current?.id === song.id;
  const showPause = isCurrent && isPlaying;

  return (
    <div
      className={cn(
        "group relative flex flex-col gap-3 rounded-lg bg-bg-card/40 p-4 transition-colors hover:bg-bg-hover",
        className,
      )}
    >
      <div className="relative">
        <CoverArt src={song.picture_url} alt={song.title} size="lg" rounded="md" className="w-full h-auto aspect-square" />
        <IconButton
          label={showPause ? "Pausar" : "Reproducir"}
          variant="accent"
          size="lg"
          className={cn(
            "absolute bottom-2 right-2 shadow-xl transition-all",
            showPause
              ? "translate-y-0 opacity-100"
              : "translate-y-2 opacity-0 group-hover:translate-y-0 group-hover:opacity-100",
          )}
          onClick={() => {
            if (isCurrent) togglePlay();
            else onPlay();
          }}
        >
          {showPause ? <Pause /> : <Play />}
        </IconButton>
      </div>
      <div className="min-w-0">
        <p
          className={cn(
            "truncate text-sm font-semibold",
            isCurrent ? "text-accent-hover" : "text-text",
          )}
          title={song.title}
        >
          {song.title}
        </p>
        <p className="truncate text-xs text-text-muted">
          {song.genre || "Sin género"}
        </p>
      </div>
    </div>
  );
}
