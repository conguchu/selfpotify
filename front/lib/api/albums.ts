import { apiFetch } from "./client";
import type { AlbumDTO } from "@/lib/types";

export function listAlbums() {
  return apiFetch<AlbumDTO[]>("/api/albums");
}

export function getAlbum(id: number) {
  return apiFetch<AlbumDTO>(`/api/albums/${id}`);
}

/** Edita el nombre y/o la portada de un álbum (solo admin). */
export function updateAlbum(
  id: number,
  payload: { name: string; photoUrl: string | null },
) {
  return apiFetch<AlbumDTO>(`/api/albums/${id}`, {
    method: "PUT",
    body: payload,
  });
}
