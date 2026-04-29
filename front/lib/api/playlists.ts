import { apiFetch } from "./client";
import type { PlaylistDTO, PlaylistInput } from "@/lib/types";

export function listMyPlaylists() {
  return apiFetch<PlaylistDTO[]>("/api/playlists/my");
}

export function getPlaylist(id: number) {
  return apiFetch<PlaylistDTO>(`/api/playlists/${id}`);
}

export function createPlaylist(payload: PlaylistInput) {
  return apiFetch<PlaylistDTO>("/api/playlists", {
    method: "POST",
    body: payload,
  });
}

export function updatePlaylist(id: number, payload: PlaylistInput) {
  return apiFetch<PlaylistDTO>(`/api/playlists/${id}`, {
    method: "PUT",
    body: payload,
  });
}

export function deletePlaylist(id: number) {
  return apiFetch<void>(`/api/playlists/${id}`, {
    method: "DELETE",
    parse: "none",
  });
}
