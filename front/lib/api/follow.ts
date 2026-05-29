import { apiFetch } from "./client";
import type { UserSummaryDTO } from "@/lib/types";

/**
 * Cliente del API de follow. Los endpoints son simétricos: tanto seguir como
 * dejar de seguir devuelven el {@link UserSummaryDTO} actualizado del usuario
 * <em>target</em> (con sus counts y la flag {@code isFollowedByMe} ya
 * recalculadas), lo que permite refrescar la UI con una sola respuesta sin
 * tener que volver a pedir el perfil.
 */

export function followUser(id: number) {
  return apiFetch<UserSummaryDTO>(`/api/users/${id}/follow`, {
    method: "POST",
  });
}

export function unfollowUser(id: number) {
  return apiFetch<UserSummaryDTO>(`/api/users/${id}/follow`, {
    method: "DELETE",
  });
}

export function getFollowers(id: number) {
  return apiFetch<UserSummaryDTO[]>(`/api/users/${id}/followers`);
}

export function getFollowing(id: number) {
  return apiFetch<UserSummaryDTO[]>(`/api/users/${id}/following`);
}
