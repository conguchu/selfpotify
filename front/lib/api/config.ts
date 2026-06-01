import { apiFetch } from "./client";
import type {
  BrandingDTO,
  PublicConfig,
  RescanResult,
  ServerConfig,
  SetupPayload,
  UpdateConfigPayload,
} from "@/lib/types";

export function rescanLibrary() {
  return apiFetch<RescanResult>("/api/config/scan/rescan", {
    method: "POST",
  });
}

/** Config completa del servidor (solo admin). */
export function getServerConfig() {
  return apiFetch<ServerConfig>("/api/config");
}

/** Actualiza branding, features y/o intervalo de escaneo (solo admin). */
export function updateServerConfig(payload: UpdateConfigPayload) {
  return apiFetch<ServerConfig>("/api/config", {
    method: "PUT",
    body: payload,
  });
}

/** Añade una ruta de música a escanear (lanza un escaneo inicial de esa ruta). */
export function addScanPath(path: string) {
  return apiFetch<ServerConfig>("/api/config/scan-paths", {
    method: "POST",
    body: { path },
  });
}

/** Quita una ruta de música del escaneo. */
export function removeScanPath(path: string) {
  return apiFetch<ServerConfig>(
    `/api/config/scan-paths?path=${encodeURIComponent(path)}`,
    { method: "DELETE" },
  );
}

/** Resetea el servidor: borra BBDD y config, y reseedea el admin del .env. */
export function resetServer() {
  return apiFetch<{ status: string; message: string }>("/api/config/reset", {
    method: "POST",
  });
}

/** Config pública (sin auth): branding + flag setupComplete. */
export function getPublicConfig() {
  return apiFetch<PublicConfig>("/api/config/public");
}

/** Actualiza branding (nombre y/o colores). Abierto sin login en modo setup. */
export function updateBranding(branding: {
  appName?: string;
  colors?: Record<string, string>;
}) {
  return apiFetch<unknown>("/api/config", {
    method: "PUT",
    body: { branding },
  });
}

/** Sube el logo de la app. Abierto sin login en modo setup. */
export function uploadLogo(file: File) {
  const fd = new FormData();
  fd.append("file", file);
  return apiFetch<BrandingDTO>("/api/config/logo", {
    method: "POST",
    body: fd,
  });
}

/** Commit final del wizard: persiste config y marca setupComplete=true. */
export function setupServer(payload: SetupPayload) {
  return apiFetch<unknown>("/api/config/setup", {
    method: "POST",
    body: payload,
  });
}
