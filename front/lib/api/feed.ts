import { apiFetch } from "./client";
import type { ArtistDTO } from "@/lib/types";

/**
 * Carga el feed del home (`GET /api/feed`).
 *
 * El backend regenera el feed del usuario autenticado en CADA llamada
 * (los 10 artistas más escuchados) y devuelve los artistas recomendados.
 * Por eso el hook que lo consume nunca cachea la respuesta.
 */
export function getHomeFeed() {
  return apiFetch<ArtistDTO[]>("/api/feed");
}
