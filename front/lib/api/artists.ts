import { apiFetch } from "./client";
import type { ArtistDTO, Top10ArtistTracks } from "@/lib/types";

export function listArtists() {
  return apiFetch<ArtistDTO[]>("/api/artists");
}

export function getArtist(id: number) {
  return apiFetch<ArtistDTO>(`/api/artists/${id}`);
}

/** Top 10 canciones del artista, ordenadas por número de escuchas. */
export function getArtistTopTracks(id: number) {
  return apiFetch<Top10ArtistTracks>(`/api/artists/${id}/top-10-tracks`);
}
