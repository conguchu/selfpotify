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
  listSharedPlaylists,
  listUserPublicPlaylists,
  updatePlaylist,
  getPlaylist,
  uploadPlaylistCover,
  createPlaylistShareLink,
  redeemPlaylistShareLink,
  addSongToPlaylist,
  removeSongFromPlaylist,
  listPlaylistCollaborators,
  removePlaylistCollaborator,
} from "@/lib/api/playlists";
import {
  deleteMyAvatar,
  getMe,
  getPublicProfile,
  updateMyProfile,
  uploadMyAvatar,
} from "@/lib/api/profile";
import {
  followUser,
  getFollowers,
  getFollowing,
  unfollowUser,
} from "@/lib/api/follow";
import {
  listSongs,
  getSong,
  importFolder,
  createSong,
  deleteSong,
  updateSong,
  uploadSongsToStaging,
  commitSongs,
  uploadSongCover,
  setSongArtists,
  getGenreTopSongs,
} from "@/lib/api/songs";
import {
  listArtists,
  getArtist,
  getArtistTopTracks,
  createArtist,
  updateArtist,
  deleteArtist,
  splitArtist,
  mergeArtists,
  fetchArtistPhoto,
} from "@/lib/api/artists";
import {
  getHomeFeed,
  getRecentGenres,
  getDailyDiscoveries,
} from "@/lib/api/feed";
import { getAlbum, listAlbums, updateAlbum } from "@/lib/api/albums";
import { search as searchApi } from "@/lib/api/search";
import {
  getPublicConfig,
  getServerConfig,
  rescanLibrary,
  updateServerConfig,
  addScanPath,
  removeScanPath,
  resetServer,
} from "@/lib/api/config";
import {
  createUser,
  deleteUser,
  listUsers,
  updateUserPassword,
  changeUserRole,
} from "@/lib/api/users";
import type {
  CreateSongPayload,
  CreateUserPayload,
  ImportFolderPayload,
  PlaylistInput,
  SearchType,
  SongCommitPayload,
  UpdateConfigPayload,
  UpdateSongPayload,
} from "@/lib/types";

export const queryKeys = {
  songs: ["songs"] as const,
  artists: ["artists"] as const,
  artist: (id: number) => ["artists", id] as const,
  artistTopTracks: (id: number) => ["artists", id, "top-tracks"] as const,
  albums: ["albums"] as const,
  album: (id: number) => ["albums", id] as const,
  homeFeed: ["feed", "home"] as const,
  recentGenres: ["feed", "genres"] as const,
  dailyDiscoveries: ["feed", "daily-discoveries"] as const,
  genreTopSongs: (genre: string) => ["songs", "genre", genre, "top"] as const,
  playlists: ["playlists", "my"] as const,
  sharedPlaylists: ["playlists", "shared"] as const,
  playlist: (id: number) => ["playlists", id] as const,
  playlistCollaborators: (id: number) =>
    ["playlists", id, "collaborators"] as const,
  users: ["users"] as const,
  song: (id: number) => ["songs", id] as const,
  serverConfig: ["config", "server"] as const,
  me: ["me"] as const,
  publicProfile: (id: number) => ["users", "public", id] as const,
  publicProfilePlaylists: (id: number) =>
    ["users", "public", id, "playlists"] as const,
  followers: (id: number) => ["users", id, "followers"] as const,
  following: (id: number) => ["users", id, "following"] as const,
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

export function useSong(id: number, enabled = true) {
  return useQuery({
    queryKey: queryKeys.song(id),
    queryFn: () => getSong(id),
    enabled: enabled && Number.isFinite(id),
  });
}

/** Config completa del servidor (solo admin): branding, scan paths, features, contexto Docker. */
export function useServerConfig(enabled = true) {
  return useQuery({
    queryKey: queryKeys.serverConfig,
    queryFn: getServerConfig,
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

export function useAlbum(id: number, enabled = true) {
  return useQuery({
    queryKey: queryKeys.album(id),
    queryFn: () => getAlbum(id),
    enabled: enabled && Number.isFinite(id),
  });
}

/** Edita nombre/portada de un álbum. Invalida la lista y el álbum concreto. */
export function useUpdateAlbum() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({
      id,
      payload,
    }: {
      id: number;
      payload: { name: string; photoUrl: string | null };
    }) => updateAlbum(id, payload),
    onSuccess: (_data, vars) => {
      qc.invalidateQueries({ queryKey: queryKeys.albums });
      qc.invalidateQueries({ queryKey: queryKeys.album(vars.id) });
    },
  });
}

export function useMyPlaylists(enabled = true) {
  return useQuery({
    queryKey: queryKeys.playlists,
    queryFn: listMyPlaylists,
    enabled,
  });
}

export function useSharedPlaylists(enabled = true) {
  return useQuery({
    queryKey: queryKeys.sharedPlaylists,
    queryFn: listSharedPlaylists,
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

export function useCreatePlaylistShareLink() {
  return useMutation({
    mutationFn: (id: number) => createPlaylistShareLink(id),
  });
}

export function usePlaylistCollaborators(id: number, enabled = true) {
  return useQuery({
    queryKey: queryKeys.playlistCollaborators(id),
    queryFn: () => listPlaylistCollaborators(id),
    enabled: enabled && Number.isFinite(id),
  });
}

export function useRemovePlaylistCollaborator() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ playlistId, userId }: { playlistId: number; userId: number }) =>
      removePlaylistCollaborator(playlistId, userId),
    onSuccess: (_data, vars) => {
      qc.invalidateQueries({
        queryKey: queryKeys.playlistCollaborators(vars.playlistId),
      });
      qc.invalidateQueries({ queryKey: queryKeys.playlist(vars.playlistId) });
      qc.invalidateQueries({ queryKey: queryKeys.playlists });
      qc.invalidateQueries({ queryKey: queryKeys.sharedPlaylists });
    },
  });
}

export function useRedeemPlaylistShareLink() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (token: string) => redeemPlaylistShareLink(token),
    onSuccess: (data) => {
      qc.invalidateQueries({ queryKey: queryKeys.sharedPlaylists });
      qc.invalidateQueries({ queryKey: queryKeys.playlist(data.id) });
    },
  });
}

/** Añade o quita una canción de una playlist (dueño o colaborador). */
export function useTogglePlaylistSong() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({
      playlistId,
      songId,
      add,
    }: {
      playlistId: number;
      songId: number;
      add: boolean;
    }) =>
      add
        ? addSongToPlaylist(playlistId, songId)
        : removeSongFromPlaylist(playlistId, songId),
    onSuccess: (_data, vars) => {
      qc.invalidateQueries({ queryKey: queryKeys.playlists });
      qc.invalidateQueries({ queryKey: queryKeys.sharedPlaylists });
      qc.invalidateQueries({ queryKey: queryKeys.playlist(vars.playlistId) });
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

export function useUpdateSong() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ id, payload }: { id: number; payload: UpdateSongPayload }) =>
      updateSong(id, payload),
    onSuccess: (_data, vars) => {
      qc.invalidateQueries({ queryKey: queryKeys.songs });
      qc.invalidateQueries({ queryKey: queryKeys.song(vars.id) });
    },
  });
}

/** Fase 1: sube audios a staging y devuelve borradores editables. No persiste nada. */
export function useUploadSongsToStaging() {
  return useMutation({
    mutationFn: (files: File[]) => uploadSongsToStaging(files),
  });
}

/** Fase 2: confirma los borradores y persiste las canciones. Invalida biblioteca. */
export function useCommitSongs() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (payload: SongCommitPayload) => commitSongs(payload),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: queryKeys.songs });
      qc.invalidateQueries({ queryKey: queryKeys.artists });
      qc.invalidateQueries({ queryKey: queryKeys.albums });
      qc.invalidateQueries({ queryKey: queryKeys.serverConfig });
    },
  });
}

/** Sube una carátula (imagen) y devuelve su URL en /assets/covers. */
export function useUploadSongCover() {
  return useMutation({
    mutationFn: (file: File) => uploadSongCover(file),
  });
}

/** Crea un artista nuevo por nombre. Invalida la lista de artistas. */
export function useCreateArtist() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (name: string) => createArtist(name),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: queryKeys.artists });
    },
  });
}

/** Edita nombre/foto de un artista. Invalida la lista y el artista concreto. */
export function useUpdateArtist() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({
      id,
      payload,
    }: {
      id: number;
      payload: { name: string; photoUrl: string | null };
    }) => updateArtist(id, payload),
    onSuccess: (_data, vars) => {
      qc.invalidateQueries({ queryKey: queryKeys.artists });
      qc.invalidateQueries({ queryKey: queryKeys.artist(vars.id) });
    },
  });
}

/**
 * Busca automáticamente una foto para el artista (Deezer). No invalida nada: el
 * formulario de edición fija la URL devuelta y la guarda al confirmar.
 */
export function useFetchArtistPhoto() {
  return useMutation({
    mutationFn: (id: number) => fetchArtistPhoto(id),
  });
}

/** Borra un artista. Invalida artistas, canciones y álbumes (cambian atribuciones). */
export function useDeleteArtist() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (id: number) => deleteArtist(id),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: queryKeys.artists });
      qc.invalidateQueries({ queryKey: queryKeys.songs });
      qc.invalidateQueries({ queryKey: queryKeys.albums });
    },
  });
}

/** Separa un artista en varios. Invalida artistas, canciones y álbumes. */
export function useSplitArtist() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ id, names }: { id: number; names: string[] }) =>
      splitArtist(id, names),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: queryKeys.artists });
      qc.invalidateQueries({ queryKey: queryKeys.songs });
      qc.invalidateQueries({ queryKey: queryKeys.albums });
    },
  });
}

/** Une varios artistas en uno. Invalida artistas, canciones y álbumes. */
export function useMergeArtists() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (payload: {
      ids: number[];
      survivorId: number;
      name?: string;
    }) => mergeArtists(payload),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: queryKeys.artists });
      qc.invalidateQueries({ queryKey: queryKeys.songs });
      qc.invalidateQueries({ queryKey: queryKeys.albums });
    },
  });
}

/** Reasigna los artistas de una canción. Invalida la canción y la biblioteca. */
export function useSetSongArtists() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ id, artistIds }: { id: number; artistIds: number[] }) =>
      setSongArtists(id, artistIds),
    onSuccess: (_data, vars) => {
      qc.invalidateQueries({ queryKey: queryKeys.songs });
      qc.invalidateQueries({ queryKey: queryKeys.song(vars.id) });
      qc.invalidateQueries({ queryKey: queryKeys.artists });
    },
  });
}

/** Actualiza branding/features/intervalo. Invalida la config pública (retematiza) y la de admin. */
export function useUpdateServerConfig() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (payload: UpdateConfigPayload) => updateServerConfig(payload),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: queryKeys.serverConfig });
      qc.invalidateQueries({ queryKey: queryKeys.publicConfig });
    },
  });
}

export function useAddScanPath() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (path: string) => addScanPath(path),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: queryKeys.serverConfig });
      qc.invalidateQueries({ queryKey: queryKeys.songs });
    },
  });
}

export function useRemoveScanPath() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (path: string) => removeScanPath(path),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: queryKeys.serverConfig });
    },
  });
}

/** Reset total del servidor. No invalida caché: el llamante desloguea tras éxito. */
export function useResetServer() {
  return useMutation({
    mutationFn: () => resetServer(),
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

export function useChangeUserRole() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ id, isAdmin }: { id: number; isAdmin: boolean }) =>
      changeUserRole(id, isAdmin),
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

/**
 * Vista del usuario autenticado (`GET /api/me`). Se usa como fuente de verdad
 * del nombre visible y la foto en la página de editar perfil; el resto de la
 * app sigue leyendo el username del auth store local.
 */
export function useMe(enabled = true) {
  return useQuery({
    queryKey: queryKeys.me,
    queryFn: getMe,
    enabled,
    staleTime: 5 * 60_000,
  });
}

/** Vista pública mínima de un usuario por id, para la página /user/[id]. */
export function usePublicProfile(id: number, enabled = true) {
  return useQuery({
    queryKey: queryKeys.publicProfile(id),
    queryFn: () => getPublicProfile(id),
    enabled: enabled && Number.isFinite(id),
    staleTime: 60_000,
  });
}

/** Playlists públicas de un usuario por id, para la página /user/[id]. */
export function useUserPublicPlaylists(id: number, enabled = true) {
  return useQuery({
    queryKey: queryKeys.publicProfilePlaylists(id),
    queryFn: () => listUserPublicPlaylists(id),
    enabled: enabled && Number.isFinite(id),
  });
}

export function useUpdateMyProfile() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (payload: { name: string | null }) => updateMyProfile(payload),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: queryKeys.me });
    },
  });
}

export function useUploadMyAvatar() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (file: File) => uploadMyAvatar(file),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: queryKeys.me });
    },
  });
}

export function useDeleteMyAvatar() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: () => deleteMyAvatar(),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: queryKeys.me });
    },
  });
}

/** Lista de usuarios que siguen al id dado, ordenada de más reciente a más antiguo. */
export function useFollowers(id: number, enabled = true) {
  return useQuery({
    queryKey: queryKeys.followers(id),
    queryFn: () => getFollowers(id),
    enabled: enabled && Number.isFinite(id),
  });
}

/** Lista de usuarios a los que sigue el id dado, ordenada de más reciente a más antiguo. */
export function useFollowing(id: number, enabled = true) {
  return useQuery({
    queryKey: queryKeys.following(id),
    queryFn: () => getFollowing(id),
    enabled: enabled && Number.isFinite(id),
  });
}

/**
 * Toggle de follow / unfollow. Invalida todas las queries que dependen del
 * grafo afectado: el perfil del target (counts cambian) y el perfil del
 * propio usuario (su {@code followingCount} cambia), más las listas de
 * followers/following de ambos. La respuesta de la mutación ya trae el
 * {@code UserSummaryDTO} del target actualizado, así que tras una invalidación
 * el render queda consistente sin hacer pedir nada extra.
 */
export function useFollowUser() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (targetId: number) => followUser(targetId),
    onSuccess: (updated, targetId) => {
      invalidateFollowGraph(qc, targetId);
    },
  });
}

export function useUnfollowUser() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (targetId: number) => unfollowUser(targetId),
    onSuccess: (updated, targetId) => {
      invalidateFollowGraph(qc, targetId);
    },
  });
}

/**
 * Tras un follow/unfollow cambian: el perfil del target (counts +
 * isFollowedByMe), el perfil propio (followingCount), y las listas de
 * followers/following de ambos lados. Lo invalidamos todo de golpe — el coste
 * es bajo porque la mayoría de esas queries no estarán en el árbol.
 */
function invalidateFollowGraph(
  qc: ReturnType<typeof useQueryClient>,
  targetId: number,
) {
  qc.invalidateQueries({ queryKey: queryKeys.publicProfile(targetId) });
  qc.invalidateQueries({ queryKey: queryKeys.followers(targetId) });
  qc.invalidateQueries({ queryKey: queryKeys.following(targetId) });
  qc.invalidateQueries({ queryKey: queryKeys.me });
  // Y la del propio user en `users/public/<myId>`/`users/<myId>/following`,
  // pero como no conocemos el id del usuario en sesión aquí, invalidamos la
  // raíz "users" — Tanstack Query 5 invalida por prefijo de array.
  qc.invalidateQueries({ queryKey: ["users"] });
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
