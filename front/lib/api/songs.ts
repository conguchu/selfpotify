import { apiFetch } from "./client";
import type {
  CreateSongPayload,
  ImportFolderPayload,
  SongDTO,
} from "@/lib/types";

export function listSongs() {
  return apiFetch<SongDTO[]>("/api/songs");
}

export function getSong(id: number) {
  return apiFetch<SongDTO>(`/api/songs/${id}`);
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
