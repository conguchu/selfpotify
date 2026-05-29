import { apiFetch } from "./client";
import type { UserSummaryDTO } from "@/lib/types";

/**
 * Cliente del API de perfil. Cubre el perfil del usuario en sesión
 * ({@code /api/me/*}) y la vista pública mínima de cualquier otro
 * ({@code /api/users/{id}/public}).
 */

export function getMe() {
  return apiFetch<UserSummaryDTO>("/api/me");
}

export function updateMyProfile(payload: { name: string | null }) {
  return apiFetch<UserSummaryDTO>("/api/me/profile", {
    method: "PUT",
    // Mandar `name: null` borra el nombre, dejando la identidad visible
    // limitada al username (mismo trato que el backend hace con el blanco).
    body: { name: payload.name ?? null },
  });
}

export function uploadMyAvatar(file: File) {
  const form = new FormData();
  form.append("file", file);
  return apiFetch<UserSummaryDTO>("/api/me/profile/picture", {
    method: "POST",
    body: form,
  });
}

export function deleteMyAvatar() {
  return apiFetch<UserSummaryDTO>("/api/me/profile/picture", {
    method: "DELETE",
  });
}

export function getPublicProfile(id: number) {
  return apiFetch<UserSummaryDTO>(`/api/users/${id}/public`);
}
