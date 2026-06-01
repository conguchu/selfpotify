import { apiFetch } from "./client";
import type { AdminUser, CreateUserPayload } from "@/lib/types";

export function listUsers() {
  return apiFetch<AdminUser[]>("/api/users");
}

export function createUser(payload: CreateUserPayload) {
  return apiFetch<string>("/api/users", {
    method: "POST",
    body: payload,
    parse: "text",
  });
}

export function updateUserPassword(id: number, password: string) {
  return apiFetch<AdminUser>(`/api/users/${id}`, {
    method: "PUT",
    body: { password },
  });
}

export function deleteUser(id: number) {
  return apiFetch<void>(`/api/users/${id}`, {
    method: "DELETE",
    parse: "none",
  });
}

/** Cambia el rol de un usuario (admin <-> usuario). El backend impide degradar al último admin. */
export function changeUserRole(id: number, isAdmin: boolean) {
  return apiFetch<AdminUser>(`/api/users/${id}/role`, {
    method: "PUT",
    body: { isAdmin },
  });
}
