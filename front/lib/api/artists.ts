import { apiFetch } from "./client";
import type { ArtistDTO } from "@/lib/types";

export function listArtists() {
  return apiFetch<ArtistDTO[]>("/api/artists");
}

export function getArtist(id: number) {
  return apiFetch<ArtistDTO>(`/api/artists/${id}`);
}
