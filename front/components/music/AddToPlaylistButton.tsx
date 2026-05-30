"use client";

import * as React from "react";
import { Check, ListMusic, Plus } from "lucide-react";
import { toast } from "sonner";
import { Modal } from "@/components/ui/Modal";
import { IconButton } from "@/components/ui/IconButton";
import { Spinner } from "@/components/ui/Spinner";
import { EmptyState } from "@/components/ui/EmptyState";
import {
  useMyPlaylists,
  useSharedPlaylists,
  useTogglePlaylistSong,
} from "@/lib/query/hooks";
import { useTapAction } from "@/lib/use-tap";
import type { PlaylistDTO } from "@/lib/types";
import { cn } from "@/lib/utils";

/**
 * Botón "+" que abre un diálogo para elegir en qué playlists está la canción,
 * con una lista de checkboxes al estilo de Spotify. Marcar/desmarcar añade o
 * quita la canción de la playlist al instante.
 */
export function AddToPlaylistButton({
  songId,
  size = "sm",
  className,
}: {
  songId: number;
  size?: "sm" | "md" | "lg";
  className?: string;
}) {
  const [open, setOpen] = React.useState(false);
  const tap = useTapAction(() => setOpen(true));

  return (
    <>
      <IconButton
        label="Añadir a playlist"
        variant="ghost"
        size={size}
        className={className}
        {...tap}
      >
        <Plus />
      </IconButton>
      {open && (
        <AddToPlaylistModal
          songId={songId}
          onClose={() => setOpen(false)}
        />
      )}
    </>
  );
}

function AddToPlaylistModal({
  songId,
  onClose,
}: {
  songId: number;
  onClose: () => void;
}) {
  // Mostramos tanto mis playlists como aquellas en las que soy colaborador:
  // ambos roles pueden añadir/quitar canciones. Los conjuntos son disjuntos
  // (mías = creadas por mí; compartidas = donde soy colaborador, no creador).
  const myQuery = useMyPlaylists();
  const sharedQuery = useSharedPlaylists();
  const toggleSong = useTogglePlaylistSong();
  const [pendingId, setPendingId] = React.useState<number | null>(null);

  const playlists = React.useMemo(
    () => [...(myQuery.data ?? []), ...(sharedQuery.data ?? [])],
    [myQuery.data, sharedQuery.data],
  );
  const isLoading = myQuery.isLoading || sharedQuery.isLoading;
  const isError = myQuery.isError;

  const toggle = async (pl: PlaylistDTO) => {
    const has = pl.songIds.includes(songId);
    setPendingId(pl.id);
    try {
      // Endpoints granulares: funcionan para dueño y colaborador (el PUT
      // completo solo lo admite el dueño).
      await toggleSong.mutateAsync({
        playlistId: pl.id,
        songId,
        add: !has,
      });
      toast.success(has ? `Quitada de «${pl.name}»` : `Añadida a «${pl.name}»`);
    } catch (err) {
      toast.error(err instanceof Error ? err.message : "Error al actualizar la playlist");
    } finally {
      setPendingId(null);
    }
  };

  return (
    <Modal
      open
      onClose={onClose}
      title="Añadir a playlist"
      description="Marca las playlists en las que quieres esta canción."
    >
      {isLoading ? (
        <div className="flex items-center justify-center py-8">
          <Spinner size="lg" />
        </div>
      ) : isError ? (
        <p className="py-4 text-sm text-danger">No se pudieron cargar tus playlists.</p>
      ) : playlists && playlists.length > 0 ? (
        <ul className="-mx-2 flex max-h-80 flex-col gap-0.5 overflow-y-auto">
          {playlists.map((pl) => {
            const checked = pl.songIds.includes(songId);
            const busy = pendingId === pl.id;
            return (
              <li key={pl.id}>
                <button
                  type="button"
                  onClick={() => toggle(pl)}
                  disabled={busy}
                  className="flex w-full items-center justify-between gap-3 rounded-md px-3 py-2.5 text-left transition-colors hover:bg-bg-hover disabled:cursor-not-allowed disabled:opacity-60"
                >
                  <span className="min-w-0">
                    <span className="block truncate text-sm font-medium text-text">
                      {pl.name}
                    </span>
                    <span className="block text-xs text-text-muted">
                      {pl.songIds.length}{" "}
                      {pl.songIds.length === 1 ? "canción" : "canciones"}
                    </span>
                  </span>
                  <span
                    aria-hidden
                    className={cn(
                      "flex h-5 w-5 shrink-0 items-center justify-center rounded border transition-colors",
                      checked
                        ? "border-accent bg-accent text-on-accent"
                        : "border-border",
                    )}
                  >
                    {busy ? (
                      <Spinner size="sm" />
                    ) : checked ? (
                      <Check className="h-3.5 w-3.5" />
                    ) : null}
                  </span>
                </button>
              </li>
            );
          })}
        </ul>
      ) : (
        <EmptyState
          icon={<ListMusic />}
          title="No tienes playlists"
          description="Crea una playlist desde la barra lateral para poder añadir canciones."
          className="border-none bg-transparent"
        />
      )}
    </Modal>
  );
}
