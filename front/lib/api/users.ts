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
