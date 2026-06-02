import { API_BASE, apiFetch } from "./client";

export async function issueStreamToken(): Promise<string> {
  const data = await apiFetch<{ token: string }>("/api/listen/token", { method: "POST" });
  return data.token;
}

export function buildAudioUrl(songId: number, streamToken: string): string {
  return `${API_BASE}/api/listen/${songId}?st=${encodeURIComponent(streamToken)}`;
}
