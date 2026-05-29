import { apiFetch } from "./client";
import type {
  PlaylistDTO,
  PlaylistInput,
  ShareLinkResponse,
  UserSummaryDTO,
} from "@/lib/types";

export function listMyPlaylists() {
  return apiFetch<PlaylistDTO[]>("/api/playlists/my");
}

/** Playlists en las que soy colaborador (no creador). */
export function listSharedPlaylists() {
  return apiFetch<PlaylistDTO[]>("/api/playlists/shared");
}

/** Playlists públicas de otro usuario por id. */
export function listUserPublicPlaylists(userId: number) {
  return apiFetch<PlaylistDTO[]>(`/api/playlists/user/${userId}`);
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

export function uploadPlaylistCover(id: number, file: File) {
  const form = new FormData();
  form.append("file", file);
  return apiFetch<PlaylistDTO>(`/api/playlists/${id}/cover`, {
    method: "POST",
    body: form,
  });
}

/** Genera un magic link de un solo uso para invitar a colaborar (solo el dueño). */
export function createPlaylistShareLink(id: number) {
  return apiFetch<ShareLinkResponse>(`/api/playlists/${id}/share`, {
    method: "POST",
  });
}

/** Canjea un magic link: une al usuario autenticado como colaborador. */
export function redeemPlaylistShareLink(token: string) {
  return apiFetch<PlaylistDTO>(
    `/api/playlists/share/${encodeURIComponent(token)}`,
    { method: "POST" },
  );
}

/** Lista los colaboradores de una playlist. */
export function listPlaylistCollaborators(id: number) {
  return apiFetch<UserSummaryDTO[]>(`/api/playlists/${id}/collaborators`);
}

/** Quita un colaborador de la playlist (solo el dueño). */
export function removePlaylistCollaborator(id: number, userId: number) {
  return apiFetch<void>(`/api/playlists/${id}/collaborators/${userId}`, {
    method: "DELETE",
    parse: "none",
  });
}

/** Añade una canción (dueño o colaborador). */
export function addSongToPlaylist(id: number, songId: number) {
  return apiFetch<PlaylistDTO>(`/api/playlists/${id}/songs/${songId}`, {
    method: "POST",
  });
}

/** Quita una canción (dueño o colaborador). */
export function removeSongFromPlaylist(id: number, songId: number) {
  return apiFetch<PlaylistDTO>(`/api/playlists/${id}/songs/${songId}`, {
    method: "DELETE",
  });
}
