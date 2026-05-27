import { apiFetch } from "./client";
import type { ArtistDTO, SongDTO } from "@/lib/types";

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

/**
 * Carga los 10 géneros escuchados más recientemente por el usuario
 * (`GET /api/feed/genres`). El índice 0 es el más reciente.
 *
 * Refleja el estado de escucha del usuario, así que el hook que lo
 * consume tampoco cachea la respuesta.
 */
export function getRecentGenres() {
  return apiFetch<string[]>("/api/feed/genres");
}

/**
 * Descubrimientos diarios del usuario (`GET /api/feed/daily-discoveries`):
 * 9 canciones (3 aleatorias + 3 no escuchadas de su último género + 3 de un
 * género que no escucha). La lista es estable durante el día y cambia a
 * medianoche, así que se puede cachear durante la sesión.
 */
export function getDailyDiscoveries() {
  return apiFetch<SongDTO[]>("/api/feed/daily-discoveries");
}
