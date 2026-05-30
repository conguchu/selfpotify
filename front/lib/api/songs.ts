import { apiFetch } from "./client";
import type {
  CreateSongPayload,
  ImportFolderPayload,
  SongDTO,
  Top10GenreSongs,
} from "@/lib/types";

export function listSongs() {
  return apiFetch<SongDTO[]>("/api/songs");
}

export function getSong(id: number) {
  return apiFetch<SongDTO>(`/api/songs/${id}`);
}

/**
 * Top 10 canciones del género, ordenadas por número de escuchas.
 * El género va como query param porque algunos contienen '/' (p.ej.
 * "Rap/Hip Hop"), que codificado en la ruta provoca un 400 en Tomcat.
 */
export function getGenreTopSongs(genre: string) {
  return apiFetch<Top10GenreSongs>(
    `/api/songs/top?genre=${encodeURIComponent(genre)}`,
  );
}

export function createSong(payload: CreateSongPayload) {
  return apiFetch<SongDTO>("/api/songs", {
    method: "POST",
    body: payload,
  });
}

export function deleteSong(id: number) {
  return apiFetch<void>(`/api/songs/${id}`, {
    method: "DELETE",
    parse: "none",
  });
}

export function importFolder(payload: ImportFolderPayload) {
  return apiFetch<SongDTO[]>("/api/songs/import", {
    method: "POST",
    body: payload,
  });
}

/** Devuelve `count` canciones completamente aleatorias del catálogo disponible. */
export function getRandomSongs(count = 10) {
  return apiFetch<SongDTO[]>(`/api/songs/random?count=${count}`);
}
