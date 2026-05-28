"use client";

import {
  useMutation,
  useQuery,
  useQueryClient,
} from "@tanstack/react-query";
import {
  createPlaylist,
  deletePlaylist,
  listMyPlaylists,
  updatePlaylist,
  getPlaylist,
  uploadPlaylistCover,
} from "@/lib/api/playlists";
import {
  listSongs,
  importFolder,
  createSong,
  deleteSong,
  getGenreTopSongs,
} from "@/lib/api/songs";
import { listArtists, getArtist, getArtistTopTracks } from "@/lib/api/artists";
import {
  getHomeFeed,
  getRecentGenres,
  getDailyDiscoveries,
} from "@/lib/api/feed";
import { listAlbums } from "@/lib/api/albums";
import { search as searchApi } from "@/lib/api/search";
import { getPublicConfig, rescanLibrary } from "@/lib/api/config";
import {
  createUser,
  deleteUser,
  listUsers,
  updateUserPassword,
} from "@/lib/api/users";
import type {
  CreateSongPayload,
  CreateUserPayload,
  ImportFolderPayload,
  PlaylistInput,
  SearchType,
} from "@/lib/types";

export const queryKeys = {
  songs: ["songs"] as const,
  artists: ["artists"] as const,
  artist: (id: number) => ["artists", id] as const,
  artistTopTracks: (id: number) => ["artists", id, "top-tracks"] as const,
  albums: ["albums"] as const,
  homeFeed: ["feed", "home"] as const,
  recentGenres: ["feed", "genres"] as const,
  dailyDiscoveries: ["feed", "daily-discoveries"] as const,
  genreTopSongs: (genre: string) => ["songs", "genre", genre, "top"] as const,
  playlists: ["playlists", "my"] as const,
  playlist: (id: number) => ["playlists", id] as const,
  users: ["users"] as const,
  publicConfig: ["config", "public"] as const,
  search: (q: string, type: SearchType, page: number, size: number) =>
    ["search", q, type, page, size] as const,
};

export function usePublicConfig() {
  return useQuery({
    queryKey: queryKeys.publicConfig,
    queryFn: getPublicConfig,
    staleTime: 60_000,
    retry: 1,
  });
}

/** Nombre de la app del branding, con fallback a "selfpotify". */
export function useAppName(): string {
  const { data } = usePublicConfig();
  return data?.branding.appName?.trim() || "selfpotify";
}

/**
 * Feed del home. El backend regenera el feed en cada petición, así que
 * forzamos un refetch en cada montaje y no conservamos la respuesta en caché.
 */
export function useHomeFeed(enabled = true) {
  return useQuery({
    queryKey: queryKeys.homeFeed,
    queryFn: getHomeFeed,
    enabled,
    staleTime: 0,
    gcTime: 0,
    refetchOnMount: "always",
  });
}

/**
 * Géneros escuchados recientemente. Como el feed, refleja el estado de
 * escucha del usuario, así que se refresca en cada montaje y no se cachea.
 */
export function useRecentGenres(enabled = true) {
  return useQuery({
    queryKey: queryKeys.recentGenres,
    queryFn: getRecentGenres,
    enabled,
    staleTime: 0,
    gcTime: 0,
    refetchOnMount: "always",
  });
}

/**
 * Descubrimientos diarios. La respuesta es estable durante el día (el backend
 * la recalcula de forma determinista por usuario+fecha), así que la cacheamos
 * y la refrescamos al volver a montar por si cambió el día.
 */
export function useDailyDiscoveries(enabled = true) {
  return useQuery({
    queryKey: queryKeys.dailyDiscoveries,
    queryFn: getDailyDiscoveries,
    enabled,
    staleTime: 30 * 60 * 1000,
    refetchOnMount: "always",
  });
}

export function useGenreTopSongs(genre: string, enabled = true) {
  return useQuery({
    queryKey: queryKeys.genreTopSongs(genre),
    queryFn: () => getGenreTopSongs(genre),
    enabled: enabled && genre.length > 0,
  });
}

export function useArtist(id: number, enabled = true) {
  return useQuery({
    queryKey: queryKeys.artist(id),
    queryFn: () => getArtist(id),
    enabled: enabled && Number.isFinite(id),
  });
}

export function useArtistTopTracks(id: number, enabled = true) {
  return useQuery({
    queryKey: queryKeys.artistTopTracks(id),
    queryFn: () => getArtistTopTracks(id),
    enabled: enabled && Number.isFinite(id),
  });
}

export function useSongs(enabled = true) {
  return useQuery({
    queryKey: queryKeys.songs,
    queryFn: listSongs,
    enabled,
  });
}

export function useArtists(enabled = true) {
  return useQuery({
    queryKey: queryKeys.artists,
    queryFn: listArtists,
    enabled,
  });
}

export function useAlbums(enabled = true) {
  return useQuery({
    queryKey: queryKeys.albums,
    queryFn: listAlbums,
    enabled,
  });
}

export function useMyPlaylists(enabled = true) {
  return useQuery({
    queryKey: queryKeys.playlists,
    queryFn: listMyPlaylists,
    enabled,
  });
}

export function usePlaylist(id: number, enabled = true) {
  return useQuery({
    queryKey: queryKeys.playlist(id),
    queryFn: () => getPlaylist(id),
    enabled: enabled && Number.isFinite(id),
  });
}

export function useUsers(enabled = true) {
  return useQuery({
    queryKey: queryKeys.users,
    queryFn: listUsers,
    enabled,
  });
}

export function useCreatePlaylist() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (payload: PlaylistInput) => createPlaylist(payload),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: queryKeys.playlists });
    },
  });
}

export function useUpdatePlaylist() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ id, payload }: { id: number; payload: PlaylistInput }) =>
      updatePlaylist(id, payload),
    onSuccess: (_data, vars) => {
      qc.invalidateQueries({ queryKey: queryKeys.playlists });
      qc.invalidateQueries({ queryKey: queryKeys.playlist(vars.id) });
    },
  });
}

export function useDeletePlaylist() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (id: number) => deletePlaylist(id),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: queryKeys.playlists });
    },
  });
}

export function useUploadPlaylistCover() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ id, file }: { id: number; file: File }) =>
      uploadPlaylistCover(id, file),
    onSuccess: (_data, vars) => {
      qc.invalidateQueries({ queryKey: queryKeys.playlist(vars.id) });
      qc.invalidateQueries({ queryKey: queryKeys.playlists });
    },
  });
}

export function useImportFolder() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (payload: ImportFolderPayload) => importFolder(payload),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: queryKeys.songs });
      qc.invalidateQueries({ queryKey: queryKeys.artists });
      qc.invalidateQueries({ queryKey: queryKeys.albums });
    },
  });
}

export function useRescanLibrary() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: rescanLibrary,
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: queryKeys.songs });
      qc.invalidateQueries({ queryKey: queryKeys.artists });
      qc.invalidateQueries({ queryKey: queryKeys.albums });
    },
  });
}

export function useCreateSong() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (payload: CreateSongPayload) => createSong(payload),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: queryKeys.songs });
    },
  });
}

export function useDeleteSong() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (id: number) => deleteSong(id),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: queryKeys.songs });
    },
  });
}

export function useCreateUser() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (payload: CreateUserPayload) => createUser(payload),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: queryKeys.users });
    },
  });
}

export function useDeleteUser() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (id: number) => deleteUser(id),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: queryKeys.users });
    },
  });
}

/**
 * Búsqueda global (`GET /api/search`).
 *
 * Una sola firma para los dos modos del backend:
 * - `type="all"` → preview multi-categoría para el dropdown.
 * - `type="songs"|"artists"|...` → categoría única paginada para la página
 *   dedicada de resultados.
 *
 * La query se considera vacía si tras hacer `trim()` no queda nada y, en ese
 * caso, no se dispara la llamada (la búsqueda está deshabilitada). Se cachea
 * 30s por (q, type, page, size) para que volver atrás desde un resultado no
 * vuelva a pegar al servidor.
 */
export function useSearch(
  q: string,
  type: SearchType = "all",
  page = 0,
  size = 20,
) {
  const trimmed = q.trim();
  return useQuery({
    queryKey: queryKeys.search(trimmed, type, page, size),
    queryFn: () => searchApi({ q: trimmed, type, page, size }),
    enabled: trimmed.length > 0,
    staleTime: 30_000,
  });
}

export function useUpdateUserPassword() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ id, password }: { id: number; password: string }) =>
      updateUserPassword(id, password),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: queryKeys.users });
    },
  });
}
