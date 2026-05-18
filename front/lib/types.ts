export type Role = "ROLE_USER" | "ROLE_ADMIN";

export interface JwtResponse {
  token: string;
  type: "Bearer";
  username: string;
  roles: Role[];
}

export interface SongDTO {
  id: number;
  title: string;
  duration_ms: number;
  genre: string | null;
  bpm: number;
  listeners: number;
  picture_url: string | null;
  artistIds: number[];
  artistNames: string[];
  albumId: number | null;
}

export interface ArtistDTO {
  id: number;
  name: string;
  biography: string | null;
  photoUrl: string | null;
  albumIds: number[];
  songIds: number[];
}

/**
 * Canción tal cual la serializa el backend (entidad `Song`).
 * Es la forma que devuelve `GET /api/artists/{id}/top-10-tracks`,
 * distinta de `SongDTO` (que aplana artistas y álbum a ids).
 */
export interface RawSong {
  id: number;
  title: string;
  duration_ms: number;
  genre: string | null;
  listeners: number;
  bpm: number;
  available: boolean;
  picture_url: string | null;
  artists: { id: number; name: string }[] | null;
  album: { id: number } | null;
}

/** Respuesta de `GET /api/artists/{id}/top-10-tracks`. */
export interface Top10ArtistTracks {
  tracks: RawSong[];
}

/** Respuesta de `GET /api/songs/{genre}/top`. */
export interface Top10GenreSongs {
  genre: string;
  top: SongDTO[];
}

export interface AlbumDTO {
  id: number;
  name: string;
  releaseDate: string | null;
  pictureUrl: string | null;
  artistId: number | null;
  songIds: number[];
}

export interface PlaylistDTO {
  id: number;
  name: string;
  description: string | null;
  isPublic: boolean;
  creatorId: number;
  songIds: number[];
}

export interface AdminUser {
  id: number;
  username: string;
  profile: unknown | null;
  type: "USER" | "ADMIN";
}

export interface LoginPayload {
  username: string;
  password: string;
}

export interface SignupPayload {
  username: string;
  password: string;
}

export interface AdminSignupPayload extends SignupPayload {
  signupKey: string;
}

export interface CreateUserPayload {
  username: string;
  password: string;
  isAdmin: boolean;
}

export interface ImportFolderPayload {
  path: string;
}

export interface CreateSongPayload {
  title: string;
  songPath: string;
  genre?: string;
  bpm?: number;
  duration_ms?: number;
  picture_url?: string;
  available?: boolean;
}

export interface PlaylistInput {
  name: string;
  description?: string;
  isPublic: boolean;
  songIds: number[];
}

export interface RescanResult {
  added: number;
  recovered: number;
  skipped: number;
  failed: number;
}

export interface BrandingDTO {
  appName: string;
  logoUrl: string | null;
  colors: Record<string, string>;
}

export interface PublicConfig {
  branding: BrandingDTO;
  setupComplete: boolean;
  lastfmEnabled: boolean;
  musicLibraryPath: string | null;
  /** Tamaño máximo en bytes del logo que admite el backend. */
  logoMaxBytes: number;
}

export interface SetupPayload {
  appName?: string;
  scanPaths?: string[];
  autoCompleteMetadata?: boolean;
  scanIntervalSeconds?: number;
}
