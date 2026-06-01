"use client";

import { useMemo, useState } from "react";
import Link from "next/link";
import { Pencil, Trash2, Music, Search, UploadCloud } from "lucide-react";
import { toast } from "sonner";
import { Button } from "@/components/ui/Button";
import { IconButton } from "@/components/ui/IconButton";
import { Input } from "@/components/ui/Input";
import { Modal } from "@/components/ui/Modal";
import { Spinner } from "@/components/ui/Spinner";
import { EmptyState } from "@/components/ui/EmptyState";
import { Table, TBody, TD, TH, THead, TR } from "@/components/ui/Table";
import { UploadSongsForm } from "@/components/admin/UploadSongsForm";
import { useDeleteSong, useSongs } from "@/lib/query/hooks";
import { formatDuration } from "@/lib/utils";
import type { SongDTO } from "@/lib/types";

export function SongsManager() {
  const songsQuery = useSongs();
  const deleteSong = useDeleteSong();
  const [query, setQuery] = useState("");
  const [confirmDelete, setConfirmDelete] = useState<SongDTO | null>(null);
  const [uploadOpen, setUploadOpen] = useState(false);

  const songs = songsQuery.data ?? [];
  const filtered = useMemo(() => {
    const q = query.trim().toLowerCase();
    if (!q) return songs;
    return songs.filter(
      (s) =>
        s.title.toLowerCase().includes(q) ||
        (s.genre ?? "").toLowerCase().includes(q) ||
        s.artistNames.some((a) => a.toLowerCase().includes(q)),
    );
  }, [songs, query]);

  const onDelete = async () => {
    if (!confirmDelete) return;
    try {
      await deleteSong.mutateAsync(confirmDelete.id);
      toast.success("Canción eliminada");
      setConfirmDelete(null);
    } catch (err) {
      toast.error(err instanceof Error ? err.message : "Error al eliminar");
    }
  };

  if (songsQuery.isLoading) {
    return (
      <div className="flex items-center justify-center py-10">
        <Spinner />
      </div>
    );
  }
  if (songsQuery.isError) {
    return (
      <p className="text-sm text-danger">
        Error al cargar canciones:{" "}
        {songsQuery.error instanceof Error ? songsQuery.error.message : "?"}
      </p>
    );
  }

  return (
    <div className="flex flex-col gap-4">
      <div className="flex items-center justify-between gap-3">
        <div className="relative w-full max-w-sm">
          <Search className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-text-subtle" />
          <Input
            value={query}
            onChange={(e) => setQuery(e.target.value)}
            placeholder="Buscar por título, artista o género"
            className="pl-9"
          />
        </div>
        <Button
          onClick={() => setUploadOpen(true)}
          leftIcon={<UploadCloud className="h-4 w-4" />}
        >
          Subir audios
        </Button>
      </div>

      {filtered.length === 0 ? (
        <EmptyState
          icon={<Music />}
          title={songs.length === 0 ? "No hay canciones" : "Sin resultados"}
          description={
            songs.length === 0
              ? 'Usa el botón "Subir audios" para añadir canciones por arrastrar y soltar.'
              : "Ninguna canción coincide con la búsqueda."
          }
        />
      ) : (
        <Table>
          <THead>
            <TR>
              <TH>ID</TH>
              <TH>Título</TH>
              <TH>Artistas</TH>
              <TH>Género</TH>
              <TH className="text-right">BPM</TH>
              <TH className="text-right">Duración</TH>
              <TH className="w-24 text-right">Acciones</TH>
            </TR>
          </THead>
          <TBody>
            {filtered.map((s) => (
              <TR key={s.id}>
                <TD className="tabular-nums text-text-muted">{s.id}</TD>
                <TD className="font-medium">{s.title}</TD>
                <TD className="text-text-muted">
                  {s.artistNames.length ? s.artistNames.join(", ") : "—"}
                </TD>
                <TD className="text-text-muted">{s.genre || "—"}</TD>
                <TD className="text-right tabular-nums text-text-muted">
                  {s.bpm || "—"}
                </TD>
                <TD className="text-right tabular-nums text-text-muted">
                  {formatDuration(s.duration_ms)}
                </TD>
                <TD className="text-right">
                  <div className="inline-flex items-center gap-1">
                    <Link href={`/admin/songs/${s.id}`}>
                      <IconButton label="Editar" variant="ghost" size="sm">
                        <Pencil />
                      </IconButton>
                    </Link>
                    <IconButton
                      label="Eliminar"
                      variant="ghost"
                      size="sm"
                      onClick={() => setConfirmDelete(s)}
                    >
                      <Trash2 />
                    </IconButton>
                  </div>
                </TD>
              </TR>
            ))}
          </TBody>
        </Table>
      )}

      <Modal
        open={!!confirmDelete}
        onClose={() => setConfirmDelete(null)}
        title="Eliminar canción"
        description={
          confirmDelete
            ? `Vas a borrar "${confirmDelete.title}". Esta acción no se puede deshacer.`
            : ""
        }
        footer={
          <>
            <Button variant="ghost" onClick={() => setConfirmDelete(null)}>
              Cancelar
            </Button>
            <Button
              variant="danger"
              loading={deleteSong.isPending}
              onClick={onDelete}
            >
              Eliminar
            </Button>
          </>
        }
      >
        <p className="text-sm text-text-muted">
          Solo se elimina la canción del catálogo; el archivo de audio en disco no
          se borra.
        </p>
      </Modal>

      <Modal
        open={uploadOpen}
        onClose={() => setUploadOpen(false)}
        title="Subir audios (drag & drop)"
        description="Arrastra archivos .mp3/.wav, revisa sus datos y añádelos a la biblioteca. Se guardan en una carpeta selfpotify_added del servidor."
        className="max-w-3xl max-h-[85vh] overflow-y-auto"
      >
        <UploadSongsForm onDone={() => setUploadOpen(false)} />
      </Modal>
    </div>
  );
}
