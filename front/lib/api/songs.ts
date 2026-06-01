import { apiFetch } from "./client";
import type {
  CreateSongPayload,
  ImportFolderPayload,
  SongCommitPayload,
  SongDraft,
  SongDTO,
  Top10GenreSongs,
  UpdateSongPayload,
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

/** Edita los metadatos de una canción (solo admin). */
export function updateSong(id: number, payload: UpdateSongPayload) {
  return apiFetch<SongDTO>(`/api/songs/${id}`, {
    method: "PUT",
    body: payload,
  });
}

/**
 * Fase 1 de la subida drag&drop: sube audios a staging y devuelve borradores
 * editables (metadatos extraídos), aún sin incorporarlos a la biblioteca.
 */
export function uploadSongsToStaging(files: File[]) {
  const fd = new FormData();
  for (const file of files) fd.append("files", file);
  return apiFetch<SongDraft[]>("/api/songs/upload", {
    method: "POST",
    body: fd,
  });
}

/** Fase 2: confirma los borradores ya ajustados y persiste las canciones. */
export function commitSongs(payload: SongCommitPayload) {
  return apiFetch<SongDTO[]>("/api/songs/commit", {
    method: "POST",
    body: payload,
  });
}

/** Sube una carátula y la guarda donde las carátulas normales. Devuelve su URL. */
export function uploadSongCover(file: File) {
  const fd = new FormData();
  fd.append("file", file);
  return apiFetch<{ url: string }>("/api/songs/cover", {
    method: "POST",
    body: fd,
  });
}

/** Reasigna los artistas de una canción por sus ids. */
export function setSongArtists(id: number, artistIds: number[]) {
  return apiFetch<SongDTO>(`/api/songs/${id}/artists`, {
    method: "PUT",
    body: { artistIds },
  });
}

/** Devuelve `count` canciones completamente aleatorias del catálogo disponible. */
export function getRandomSongs(count = 10) {
  return apiFetch<SongDTO[]>(`/api/songs/random?count=${count}`);
}
