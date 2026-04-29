import { apiFetch } from "./client";
import type { AlbumDTO } from "@/lib/types";

export function listAlbums() {
  return apiFetch<AlbumDTO[]>("/api/albums");
}

export function getAlbum(id: number) {
  return apiFetch<AlbumDTO>(`/api/albums/${id}`);
}
