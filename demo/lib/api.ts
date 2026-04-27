export const API_BASE =
  process.env.NEXT_PUBLIC_API_BASE ?? "http://localhost:8080";

const TOKEN_KEY = "selfpotify_token";

export function getToken(): string | null {
  if (typeof window === "undefined") return null;
  return localStorage.getItem(TOKEN_KEY);
}

export function setToken(token: string) {
  localStorage.setItem(TOKEN_KEY, token);
}

export function clearToken() {
  localStorage.removeItem(TOKEN_KEY);
}

export async function login(username: string, password: string) {
  const r = await fetch(`${API_BASE}/api/auth/login`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ username, password }),
  });
  if (!r.ok) throw new Error(`Login failed: ${r.status}`);
  const data = await r.json();
  setToken(data.token);
  return data;
}

export type Song = {
  id: number;
  title: string;
  duration_ms: number;
  genre: string | null;
  bpm: number;
  picture_url: string | null;
  albumId: number | null;
  artistIds: number[];
};

export async function listSongs(): Promise<Song[]> {
  const token = getToken();
  if (!token) throw new Error("Not authenticated");
  const r = await fetch(`${API_BASE}/api/songs`, {
    headers: { Authorization: `Bearer ${token}` },
  });
  if (!r.ok) throw new Error(`Failed to list songs: ${r.status}`);
  return r.json();
}

export function streamUrl(songId: number): string {
  const token = getToken();
  return `${API_BASE}/api/listen/${songId}?token=${encodeURIComponent(token ?? "")}`;
}
