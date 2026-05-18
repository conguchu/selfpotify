import { clsx, type ClassValue } from "clsx";
import { twMerge } from "tailwind-merge";
import type { RawSong, SongDTO } from "@/lib/types";

export function cn(...inputs: ClassValue[]) {
  return twMerge(clsx(inputs));
}

/**
 * Convierte una `RawSong` (entidad `Song` del backend) en el `SongDTO`
 * que esperan el reproductor y los componentes de música.
 */
export function rawSongToDTO(song: RawSong): SongDTO {
  return {
    id: song.id,
    title: song.title,
    duration_ms: song.duration_ms,
    genre: song.genre,
    bpm: song.bpm,
    picture_url: song.picture_url,
    artistIds: (song.artists ?? []).map((a) => a.id),
    albumId: song.album?.id ?? null,
  };
}

export function formatDuration(ms: number | null | undefined): string {
  if (!ms || ms < 0) return "0:00";
  const totalSeconds = Math.floor(ms / 1000);
  const minutes = Math.floor(totalSeconds / 60);
  const seconds = totalSeconds % 60;
  return `${minutes}:${seconds.toString().padStart(2, "0")}`;
}

export function pluralize(n: number, singular: string, plural?: string) {
  return n === 1 ? singular : plural ?? singular + "s";
}
