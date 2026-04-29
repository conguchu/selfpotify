import { API_BASE } from "./client";

export function buildAudioUrl(songId: number, token: string | null): string {
  if (!token) return `${API_BASE}/api/listen/${songId}`;
  return `${API_BASE}/api/listen/${songId}?token=${encodeURIComponent(token)}`;
}
