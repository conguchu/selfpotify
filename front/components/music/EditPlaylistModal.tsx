"use client";

import { useMemo } from "react";
import { toast } from "sonner";
import { useUpdatePlaylist, useUploadPlaylistCover } from "@/lib/query/hooks";
import type { PlaylistDTO } from "@/lib/types";
import { PlaylistFormModal } from "./PlaylistFormModal";

export function EditPlaylistModal({
  open,
  onClose,
  playlist,
}: {
  open: boolean;
  onClose: () => void;
  playlist: PlaylistDTO;
}) {
  const update = useUpdatePlaylist();
  const uploadCover = useUploadPlaylistCover();

  const initial = useMemo(
    () => ({
      name: playlist.name,
      description: playlist.description ?? "",
      isPublic: playlist.isPublic,
    }),
    [playlist.name, playlist.description, playlist.isPublic],
  );

  return (
    <PlaylistFormModal
      open={open}
      onClose={onClose}
      title="Editar playlist"
      description="Cambia el nombre, la descripción, la visibilidad y la carátula de tu playlist."
      submitLabel="Guardar"
      initial={initial}
      currentCoverUrl={playlist.pictureUrl}
      loading={update.isPending || uploadCover.isPending}
      onSubmit={async ({ name, description, isPublic }, coverFile) => {
        try {
          await update.mutateAsync({
            id: playlist.id,
            payload: {
              name,
              description: description || undefined,
              isPublic,
              songIds: playlist.songIds,
            },
          });

          if (coverFile) {
            await uploadCover.mutateAsync({ id: playlist.id, file: coverFile });
          }

          toast.success("Playlist actualizada");
          onClose();
        } catch (err) {
          toast.error(
            err instanceof Error ? err.message : "Error al actualizar",
          );
        }
      }}
    />
  );
}
