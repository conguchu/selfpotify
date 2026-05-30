"use client";

import Link from "next/link";
import { Pause, Play } from "lucide-react";
import { CoverArt } from "./CoverArt";
import { AddToPlaylistButton } from "./AddToPlaylistButton";
import { IconButton } from "@/components/ui/IconButton";
import { usePlayerStore } from "@/lib/player/store";
import type { SongDTO } from "@/lib/types";
import { cn, formatDuration } from "@/lib/utils";

export function SongRow({
  index,
  song,
  onPlay,
  className,
}: {
  index: number;
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
        "group grid grid-cols-[2.5rem_1fr_5rem_2.5rem_3rem] items-center gap-3 rounded-md px-3 py-2 transition-colors hover:bg-bg-hover",
        className,
      )}
    >
      <div className="relative flex h-10 w-10 items-center justify-center text-sm text-text-muted">
        <span className="group-hover:hidden">{index + 1}</span>
        <IconButton
          label={showPause ? "Pausar" : "Reproducir"}
          variant="ghost"
          size="sm"
          className="hidden group-hover:inline-flex"
          onClick={() => {
            if (isCurrent) togglePlay();
            else onPlay();
          }}
        >
          {showPause ? <Pause /> : <Play />}
        </IconButton>
      </div>
      <div className="flex min-w-0 items-center gap-3 text-left">
        <CoverArt src={song.picture_url} alt={song.title} size="sm" rounded="md" />
        <div className="min-w-0">
          <p
            className={cn(
              "truncate text-sm font-medium",
              isCurrent ? "text-accent-hover" : "text-text",
            )}
            title={song.title}
          >
            {song.title}
          </p>
          <div className="flex min-w-0 items-center gap-0.5 truncate text-xs text-text-muted">
            {song.artistNames?.map((name, i) => {
              const id = song.artistIds?.[i];
              return (
                <span key={id ?? name}>
                  {i > 0 && <span className="mr-0.5">,</span>}
                  {id != null ? (
                    <Link
                      href={`/artist/${id}`}
                      className="hover:text-text hover:underline"
                      onClick={(e) => e.stopPropagation()}
                    >
                      {name}
                    </Link>
                  ) : (
                    name
                  )}
                </span>
              );
            })}
            {song.genre && (song.artistNames?.length ?? 0) > 0 && (
              <span className="ml-0.5">- {song.genre}</span>
            )}
            {song.genre && (song.artistNames?.length ?? 0) === 0 && (
              <span>{song.genre}</span>
            )}
            {!song.genre && (song.artistNames?.length ?? 0) === 0 && "—"}
          </div>
        </div>
      </div>
      <div className="flex items-center justify-end gap-1.5 text-xs text-text-muted tabular-nums">
        <Play className="h-3.5 w-3.5" fill="currentColor" aria-hidden />
        {song.listeners ?? 0}
      </div>
      <div className="flex justify-center">
        <AddToPlaylistButton
          songId={song.id}
          className="opacity-0 transition-opacity focus-visible:opacity-100 group-hover:opacity-100"
        />
      </div>
      <div className="text-right text-xs text-text-muted tabular-nums">
        {formatDuration(song.duration_ms)}
      </div>
    </div>
  );
}
