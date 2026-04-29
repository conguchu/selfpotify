"use client";

import { SongCard } from "./SongCard";
import { usePlayerStore } from "@/lib/player/store";
import type { SongDTO } from "@/lib/types";
import { cn } from "@/lib/utils";

export function SongGrid({
  songs,
  className,
}: {
  songs: SongDTO[];
  className?: string;
}) {
  const playSong = usePlayerStore((s) => s.playSong);
  return (
    <div
      className={cn(
        "grid grid-cols-2 gap-3 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-5 xl:grid-cols-6",
        className,
      )}
    >
      {songs.map((song) => (
        <SongCard
          key={song.id}
          song={song}
          onPlay={() => playSong(song, songs)}
        />
      ))}
    </div>
  );
}
