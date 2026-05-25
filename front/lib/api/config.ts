import { apiFetch } from "./client";
import type {
  BrandingDTO,
  PublicConfig,
  RescanResult,
  SetupPayload,
} from "@/lib/types";

export function rescanLibrary() {
  return apiFetch<RescanResult>("/api/config/scan/rescan", {
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
