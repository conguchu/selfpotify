import { apiFetch } from "./client";
import type { ArtistDTO, Top10ArtistTracks } from "@/lib/types";

export function listArtists() {
  return apiFetch<ArtistDTO[]>("/api/artists");
}

/** Crea un artista nuevo solo con su nombre (modal de búsqueda del panel). */
export function createArtist(name: string) {
  return apiFetch<ArtistDTO>("/api/artists", {
    method: "POST",
    body: { name },
  });
}

export function getArtist(id: number) {
  return apiFetch<ArtistDTO>(`/api/artists/${id}`);
}

/** Edita el nombre y/o la foto de un artista (solo admin). */
export function updateArtist(
  id: number,
  payload: { name: string; photoUrl: string | null },
) {
  return apiFetch<ArtistDTO>(`/api/artists/${id}`, {
    method: "PUT",
    body: payload,
  });
}

/**
 * Busca automáticamente una foto para el artista (Deezer) y devuelve su URL sin
 * persistirla. Lanza un `ApiError` 404 si no se encuentra (o si la resolución
 * online de carátulas está desactivada en config).
 */
export function fetchArtistPhoto(id: number) {
  return apiFetch<{ url: string }>(`/api/artists/${id}/fetch-photo`, {
    method: "POST",
  });
}

/** Borra un artista (sus canciones/álbumes dejan de atribuírsele, no se borran). */
export function deleteArtist(id: number) {
  return apiFetch<void>(`/api/artists/${id}`, {
    method: "DELETE",
    parse: "none",
  });
}

/**
 * Separa un artista en varios reales. Cada nombre se resuelve contra Last.fm;
 * todas las canciones y álbumes del original se atribuyen a todos los resultantes
 * y el original se borra. Devuelve los artistas resultantes.
 */
export function splitArtist(id: number, names: string[]) {
  return apiFetch<ArtistDTO[]>(`/api/artists/${id}/split`, {
    method: "POST",
    body: { names },
  });
}

/**
 * Une varios artistas en uno (el superviviente). El superviviente absorbe las
 * canciones/álbumes del resto, que se borran. Devuelve el superviviente.
 */
export function mergeArtists(payload: {
  ids: number[];
  survivorId: number;
  name?: string;
}) {
  return apiFetch<ArtistDTO>("/api/artists/merge", {
    method: "POST",
    body: payload,
  });
}

/** Top 10 canciones del artista, ordenadas por número de escuchas. */
export function getArtistTopTracks(id: number) {
  return apiFetch<Top10ArtistTracks>(`/api/artists/${id}/top-10-tracks`);
}
