"use client";

import { use, useMemo, useState } from "react";
import { ListMusic, Lock, Pencil, Trash2 } from "lucide-react";
import { toast } from "sonner";
import { Button } from "@/components/ui/Button";
import { Spinner } from "@/components/ui/Spinner";
import { Badge } from "@/components/ui/Badge";
import { EmptyState } from "@/components/ui/EmptyState";
import { Modal } from "@/components/ui/Modal";
import { SongRow } from "@/components/music/SongRow";
import { EditPlaylistModal } from "@/components/music/EditPlaylistModal";
import { useMe, usePlaylist, useSongs, useDeletePlaylist } from "@/lib/query/hooks";
import { usePlayerStore } from "@/lib/player/store";
import { resolveImageUrl } from "@/lib/image";
import { useRouter } from "next/navigation";

export default function PlaylistPage({
  params,
}: {
  params: Promise<{ id: string }>;
}) {
  const { id } = use(params);
  const router = useRouter();
  const playlistId = Number.parseInt(id, 10);
  const playlistQuery = usePlaylist(playlistId);
  const songsQuery = useSongs();
  const playSong = usePlayerStore((s) => s.playSong);
  // Necesitamos el id del usuario autenticado, no solo el username, para
  // compararlo contra creatorId. La playlist no expone el username del creador.
  const meQuery = useMe();
  const deletePlaylist = useDeletePlaylist();
  const [confirmOpen, setConfirmOpen] = useState(false);
  const [editOpen, setEditOpen] = useState(false);

  const songsInPlaylist = useMemo(() => {
    if (!playlistQuery.data || !songsQuery.data) return [];
    const map = new Map(songsQuery.data.map((s) => [s.id, s]));
    return playlistQuery.data.songIds
      .map((sid) => map.get(sid))
      .filter((s): s is NonNullable<typeof s> => Boolean(s));
  }, [playlistQuery.data, songsQuery.data]);

  if (playlistQuery.isLoading) {
    return (
      <div className="flex items-center justify-center py-20">
        <Spinner size="lg" />
      </div>
    );
  }

  if (playlistQuery.isError || !playlistQuery.data) {
    return (
      <EmptyState
        title="Playlist no encontrada"
        description="Puede que no exista o que no tengas permiso para verla."
      />
    );
  }

  const playlist = playlistQuery.data;
  // Edit/Eliminar solo se ofrecen al creador. Los admins pueden borrar via
  // API (lo gestiona PlaylistController) pero no exponemos el botón aquí
  // para no confundir el caso normal: si quieres administrar, vas al panel.
  const isOwner = !!meQuery.data && meQuery.data.id === playlist.creatorId;

  const onDelete = async () => {
    try {
      await deletePlaylist.mutateAsync(playlist.id);
      toast.success("Playlist eliminada");
      router.replace("/home");
    } catch (err) {
      toast.error(err instanceof Error ? err.message : "Error al eliminar");
    }
  };

  return (
    <div className="flex flex-col gap-6">
      <header className="flex items-end gap-6">
        {playlist.pictureUrl && (
          <img
            src={resolveImageUrl(playlist.pictureUrl)!}
            alt={playlist.name}
            className="h-40 w-40 shrink-0 rounded-lg object-cover shadow-2xl"
          />
        )}
        {!playlist.pictureUrl && (
          <div className="flex h-40 w-40 shrink-0 items-center justify-center rounded-lg bg-gradient-to-br from-accent to-accent-active text-on-accent shadow-2xl">
            <ListMusic className="h-16 w-16" />
          </div>
        )}
        <div className="flex flex-1 flex-col gap-2">
          <span className="text-xs uppercase tracking-wide text-text-muted">
            Playlist
          </span>
          <h1 className="text-4xl font-bold tracking-tight">{playlist.name}</h1>
          {playlist.description ? (
            <p className="text-sm text-text-muted">{playlist.description}</p>
          ) : null}
          <div className="flex items-center gap-2 text-xs text-text-subtle">
            <Badge variant={playlist.isPublic ? "accent" : "neutral"}>
              {playlist.isPublic ? "Pública" : (
                <span className="inline-flex items-center gap-1">
                  <Lock className="h-3 w-3" /> Privada
                </span>
              )}
            </Badge>
            <span>·</span>
            <span>
              {songsInPlaylist.length} canci
              {songsInPlaylist.length === 1 ? "ón" : "ones"}
            </span>
          </div>
        </div>
        {isOwner ? (
          <div className="flex items-center gap-2">
            <Button
              variant="outline"
              leftIcon={<Pencil className="h-4 w-4" />}
              onClick={() => setEditOpen(true)}
            >
              Editar
            </Button>
            <Button
              variant="outline"
              leftIcon={<Trash2 className="h-4 w-4" />}
              onClick={() => setConfirmOpen(true)}
            >
              Eliminar
            </Button>
          </div>
        ) : null}
      </header>

      {songsInPlaylist.length > 0 ? (
        <div className="flex flex-col">
          <div className="grid grid-cols-[2.5rem_1fr_8rem_3rem] gap-3 px-3 py-2 text-xs uppercase tracking-wide text-text-subtle">
            <span>#</span>
            <span>Título</span>
            <span>BPM</span>
            <span className="text-right">Dur.</span>
          </div>
          {songsInPlaylist.map((song, i) => (
            <SongRow
              key={song.id}
              index={i}
              song={song}
              onPlay={() => playSong(song, songsInPlaylist)}
            />
          ))}
        </div>
      ) : (
        <EmptyState
          icon={<ListMusic />}
          title="Playlist vacía"
          description="Esta playlist no tiene canciones todavía."
        />
      )}

      <EditPlaylistModal
        open={editOpen}
        onClose={() => setEditOpen(false)}
        playlist={playlist}
      />

      <Modal
        open={confirmOpen}
        onClose={() => setConfirmOpen(false)}
        title="Eliminar playlist"
        description={`¿Seguro que quieres eliminar "${playlist.name}"?`}
        footer={
          <>
            <Button variant="ghost" onClick={() => setConfirmOpen(false)}>
              Cancelar
            </Button>
            <Button
              variant="danger"
              loading={deletePlaylist.isPending}
              onClick={onDelete}
            >
              Eliminar
            </Button>
          </>
        }
      >
        <p className="text-sm text-text-muted">
          Esta acción es irreversible.
        </p>
      </Modal>
    </div>
  );
}
