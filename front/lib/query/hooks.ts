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
} from "@/lib/api/playlists";
import { listSongs, importFolder, createSong, deleteSong } from "@/lib/api/songs";
import { listArtists } from "@/lib/api/artists";
import { listAlbums } from "@/lib/api/albums";
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
} from "@/lib/types";

export const queryKeys = {
  songs: ["songs"] as const,
  artists: ["artists"] as const,
  albums: ["albums"] as const,
  playlists: ["playlists", "my"] as const,
  playlist: (id: number) => ["playlists", id] as const,
  users: ["users"] as const,
  publicConfig: ["config", "public"] as const,
};

export function usePublicConfig() {
  return useQuery({
    queryKey: queryKeys.publicConfig,
    queryFn: getPublicConfig,
    staleTime: 60_000,
    retry: 1,
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
