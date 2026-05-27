"use client";

import { toast } from "sonner";
import { useCreatePlaylist, useUploadPlaylistCover } from "@/lib/query/hooks";
import { PlaylistFormModal } from "./PlaylistFormModal";

const EMPTY = { name: "", description: "", isPublic: false };

export function CreatePlaylistModal({
  open,
  onClose,
}: {
  open: boolean;
  onClose: () => void;
}) {
  const create = useCreatePlaylist();
  const uploadCover = useUploadPlaylistCover();

  return (
    <PlaylistFormModal
      open={open}
      onClose={onClose}
      title="Nueva playlist"
      description="Crea una colección vacía y añade canciones después."
      submitLabel="Crear"
      initial={EMPTY}
      loading={create.isPending || uploadCover.isPending}
      onSubmit={async ({ name, description, isPublic }, coverFile) => {
        try {
          const playlist = await create.mutateAsync({
            name,
            description: description || undefined,
            isPublic,
            songIds: [],
          });

          if (coverFile) {
            await uploadCover.mutateAsync({ id: playlist.id, file: coverFile });
          }

          toast.success("Playlist creada");
          onClose();
        } catch (err) {
          toast.error(
            err instanceof Error ? err.message : "Error al crear playlist",
          );
        }
      }}
    />
  );
}
