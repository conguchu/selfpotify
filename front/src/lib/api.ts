import type {
  AlbumDTO,
  ArtistDTO,
  JwtResponse,
  PlaylistDTO,
  SignupRequest,
  SongDTO,
  User,
} from './types';

const STORAGE_KEY = 'selfpotify_auth';

export type StoredAuth = JwtResponse;

export function getStoredAuth(): StoredAuth | null {
  if (typeof window === 'undefined') return null;
  const raw = window.localStorage.getItem(STORAGE_KEY);
  return raw ? (JSON.parse(raw) as StoredAuth) : null;
}

export function setStoredAuth(auth: StoredAuth | null) {
  if (typeof window === 'undefined') return;
  if (auth) window.localStorage.setItem(STORAGE_KEY, JSON.stringify(auth));
  else window.localStorage.removeItem(STORAGE_KEY);
}

export class ApiError extends Error {
  constructor(public status: number, message: string) {
    super(message);
  }
}

async function apiFetch<T>(path: string, init: RequestInit = {}): Promise<T> {
  const auth = getStoredAuth();
  const headers = new Headers(init.headers);
  headers.set('Content-Type', 'application/json');
  if (auth?.token) headers.set('Authorization', `Bearer ${auth.token}`);

  const res = await fetch(path, { ...init, headers });
  if (!res.ok) {
    let msg = `${res.status} ${res.statusText}`;
    try {
      const text = await res.text();
      if (text) msg = text;
    } catch {}
    throw new ApiError(res.status, msg);
  }
  if (res.status === 204) return undefined as T;
  const text = await res.text();
  return (text ? JSON.parse(text) : undefined) as T;
}

// Auth
export const login = (username: string, password: string) =>
  apiFetch<JwtResponse>('/api/auth/login', {
    method: 'POST',
    body: JSON.stringify({ username, password }),
  });

export const signup = (username: string, password: string) =>
  apiFetch<string>('/api/auth/signup', {
    method: 'POST',
    body: JSON.stringify({ username, password }),
  });

// Playlists
export const getMyPlaylists = () => apiFetch<PlaylistDTO[]>('/api/playlists/my');
export const getUserPlaylists = (userId: number) =>
  apiFetch<PlaylistDTO[]>(`/api/playlists/user/${userId}`);
export const getPlaylist = (id: number) => apiFetch<PlaylistDTO>(`/api/playlists/${id}`);
export const createPlaylist = (p: PlaylistDTO) =>
  apiFetch<PlaylistDTO>('/api/playlists', { method: 'POST', body: JSON.stringify(p) });
export const updatePlaylist = (id: number, p: PlaylistDTO) =>
  apiFetch<PlaylistDTO>(`/api/playlists/${id}`, { method: 'PUT', body: JSON.stringify(p) });
export const deletePlaylist = (id: number) =>
  apiFetch<void>(`/api/playlists/${id}`, { method: 'DELETE' });

// Songs / Albums / Artists
export const getSongs = () => apiFetch<SongDTO[]>('/api/songs');
export const getAlbums = () => apiFetch<AlbumDTO[]>('/api/albums');
export const getArtists = () => apiFetch<ArtistDTO[]>('/api/artists');

// Users (admin)
export const getUsers = () => apiFetch<User[]>('/api/users');
export const createUser = (req: SignupRequest) =>
  apiFetch<string>('/api/users', { method: 'POST', body: JSON.stringify(req) });
export const updateUser = (id: number, data: Partial<User> & { password?: string }) =>
  apiFetch<User>(`/api/users/${id}`, { method: 'PUT', body: JSON.stringify(data) });
export const deleteUser = (id: number) =>
  apiFetch<void>(`/api/users/${id}`, { method: 'DELETE' });
