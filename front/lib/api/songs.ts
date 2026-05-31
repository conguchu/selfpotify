import { apiFetch } from "./client";
import type {
  CreateSongPayload,
  ImportFolderPayload,
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
 * Sube audios (drag&drop). Se guardan en la carpeta selfpotify_added: en el
 * volumen de datos (Docker) o en `targetPath`/selfpotify_added (local, debe ser
 * una de las rutas de música configuradas).
 */
export function uploadSongs(files: File[], targetPath?: string) {
  const fd = new FormData();
  for (const file of files) fd.append("files", file);
  if (targetPath) fd.append("targetPath", targetPath);
  return apiFetch<SongDTO[]>("/api/songs/upload", {
    method: "POST",
    body: fd,
  });
}
