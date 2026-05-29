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
  // Popularidad derivada: número de escuchas contado por el backend sobre la
  // tabla de eventos user_song_listen (ya no es un campo almacenado).
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

/** Respuesta de `GET /api/artists/{id}/top-10-tracks`. */
export interface Top10ArtistTracks {
  tracks: SongDTO[];
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
  pictureUrl: string | null;
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
  coverArtEnabled: boolean;
  musicLibraryPath: string | null;
  /** Tamaño máximo en bytes del logo que admite el backend. */
  logoMaxBytes: number;
}

export interface SetupPayload {
  appName?: string;
  scanPaths?: string[];
  autoCompleteMetadata?: boolean;
  autoCompleteCoverArt?: boolean;
  scanIntervalSeconds?: number;
}

// =====================================
// ----- Búsqueda (`GET /api/search`)
// =====================================

/**
 * Vista pública mínima de un usuario, devuelta por la búsqueda y por todos
 * los endpoints de perfil/follow.
 *
 * Los counts y `isFollowedByMe` viajan siempre en el JSON con la misma forma,
 * pero solo los endpoints de perfil (/api/me, /api/users/{id}/public,
 * /api/users/{id}/followers|following, /follow) los rellenan; en los
 * resultados de búsqueda los counts vienen a 0 y `isFollowedByMe` a `null`
 * (decisión consciente para no introducir N+1 en SearchService).
 */
export interface UserSummaryDTO {
  id: number;
  username: string;
  displayName: string | null;
  avatarUrl: string | null;
  type: "USER" | "ADMIN";
  followersCount: number;
  followingCount: number;
  /** `null` cuando no hay viewer (admin listings) o cuando soy yo mismo. */
  isFollowedByMe: boolean | null;
}

/** Entrada de género en la búsqueda: nombre + canciones del catálogo. */
export interface GenreResultDTO {
  name: string;
  songCount: number;
}

/** Categoría paginada de la respuesta de búsqueda. */
export interface SearchCategoryPage<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
}

/** Modos soportados por el endpoint `GET /api/search`. */
export type SearchType =
  | "all"
  | "songs"
  | "artists"
  | "albums"
  | "playlists"
  | "users"
  | "genres";

/**
 * Respuesta del endpoint `GET /api/search`. En modo `all` se rellenan todas
 * las categorías; en modo específico solo se rellena la pedida (el resto
 * llega como `undefined` porque el backend las omite del JSON).
 */
export interface SearchResponseDTO {
  query: string;
  type: SearchType;
  page: number;
  size: number;
  songs?: SearchCategoryPage<SongDTO>;
  artists?: SearchCategoryPage<ArtistDTO>;
  albums?: SearchCategoryPage<AlbumDTO>;
  playlists?: SearchCategoryPage<PlaylistDTO>;
  users?: SearchCategoryPage<UserSummaryDTO>;
  genres?: SearchCategoryPage<GenreResultDTO>;
}
