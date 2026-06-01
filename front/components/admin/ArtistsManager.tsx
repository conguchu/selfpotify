"use client";

import { useMemo, useState } from "react";
import Link from "next/link";
import { Disc3, Merge, Pencil, Scissors, Search, Trash2 } from "lucide-react";
import { toast } from "sonner";
import { Avatar } from "@/components/ui/Avatar";
import { Button } from "@/components/ui/Button";
import { IconButton } from "@/components/ui/IconButton";
import { Input } from "@/components/ui/Input";
import { Modal } from "@/components/ui/Modal";
import { Spinner } from "@/components/ui/Spinner";
import { EmptyState } from "@/components/ui/EmptyState";
import { Table, TBody, TD, TH, THead, TR } from "@/components/ui/Table";
import { MergeArtistsModal } from "@/components/admin/MergeArtistsModal";
import { SplitArtistModal } from "@/components/admin/SplitArtistModal";
import { useArtists, useDeleteArtist } from "@/lib/query/hooks";
import type { ArtistDTO } from "@/lib/types";

/**
 * Lista de artistas del servidor con sus acciones de administración: editar
 * (nombre/foto), separar un artista en varios reales y juntar varios duplicados
 * en uno. La selección múltiple (checkboxes) habilita el botón «Juntar».
 */
export function ArtistsManager() {
  const artistsQuery = useArtists();
  const deleteArtist = useDeleteArtist();
  const [query, setQuery] = useState("");
  const [selected, setSelected] = useState<Set<number>>(new Set());
  const [splitTarget, setSplitTarget] = useState<ArtistDTO | null>(null);
  const [mergeOpen, setMergeOpen] = useState(false);
  const [confirmDelete, setConfirmDelete] = useState<ArtistDTO | null>(null);

  const artists = artistsQuery.data ?? [];
  const filtered = useMemo(() => {
    const q = query.trim().toLowerCase();
    const list = q
      ? artists.filter((a) => a.name.toLowerCase().includes(q))
      : artists;
    return [...list].sort((a, b) =>
      a.name.localeCompare(b.name, "es", { sensitivity: "base" }),
    );
  }, [artists, query]);

  const selectedArtists = useMemo(
    () => artists.filter((a) => selected.has(a.id)),
    [artists, selected],
  );

  const toggle = (id: number) =>
    setSelected((prev) => {
      const next = new Set(prev);
      if (next.has(id)) next.delete(id);
      else next.add(id);
      return next;
    });

  const onDelete = async () => {
    if (!confirmDelete) return;
    try {
      await deleteArtist.mutateAsync(confirmDelete.id);
      toast.success("Artista eliminado");
      setSelected((prev) => {
        const next = new Set(prev);
        next.delete(confirmDelete.id);
        return next;
      });
      setConfirmDelete(null);
    } catch (err) {
      toast.error(err instanceof Error ? err.message : "Error al eliminar");
    }
  };

  if (artistsQuery.isLoading) {
    return (
      <div className="flex items-center justify-center py-10">
        <Spinner />
      </div>
    );
  }
  if (artistsQuery.isError) {
    return (
      <p className="text-sm text-danger">
        Error al cargar artistas:{" "}
        {artistsQuery.error instanceof Error ? artistsQuery.error.message : "?"}
      </p>
    );
  }

  return (
    <div className="flex flex-col gap-4">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div className="relative max-w-sm flex-1">
          <Search className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-text-subtle" />
          <Input
            value={query}
            onChange={(e) => setQuery(e.target.value)}
            placeholder="Buscar artista"
            className="pl-9"
          />
        </div>
        <Button
          variant="secondary"
          leftIcon={<Merge className="h-4 w-4" />}
          disabled={selected.size < 2}
          onClick={() => setMergeOpen(true)}
        >
          Juntar{selected.size >= 2 ? ` (${selected.size})` : ""}
        </Button>
      </div>

      {filtered.length === 0 ? (
        <EmptyState
          icon={<Disc3 />}
          title={artists.length === 0 ? "No hay artistas" : "Sin resultados"}
          description={
            artists.length === 0
              ? "Importa o sube música para que aparezcan artistas."
              : "Ningún artista coincide con la búsqueda."
          }
        />
      ) : (
        <Table>
          <THead>
            <TR>
              <TH className="w-10" />
              <TH>Artista</TH>
              <TH className="text-right">Canciones</TH>
              <TH className="text-right">Álbumes</TH>
              <TH className="w-32 text-right">Acciones</TH>
            </TR>
          </THead>
          <TBody>
            {filtered.map((a) => (
              <TR key={a.id}>
                <TD>
                  <input
                    type="checkbox"
                    className="accent-accent"
                    aria-label={`Seleccionar ${a.name}`}
                    checked={selected.has(a.id)}
                    onChange={() => toggle(a.id)}
                  />
                </TD>
                <TD>
                  <div className="flex items-center gap-3">
                    <Avatar src={a.photoUrl} alt={a.name} size="sm" />
                    <span className="font-medium">{a.name}</span>
                  </div>
                </TD>
                <TD className="text-right tabular-nums text-text-muted">
                  {a.songIds?.length ?? 0}
                </TD>
                <TD className="text-right tabular-nums text-text-muted">
                  {a.albumIds?.length ?? 0}
                </TD>
                <TD className="text-right">
                  <div className="inline-flex items-center gap-1">
                    <IconButton
                      label="Separar"
                      variant="ghost"
                      size="sm"
                      onClick={() => setSplitTarget(a)}
                    >
                      <Scissors />
                    </IconButton>
                    <Link href={`/admin/artists/${a.id}`}>
                      <IconButton label="Editar" variant="ghost" size="sm">
                        <Pencil />
                      </IconButton>
                    </Link>
                    <IconButton
                      label="Eliminar"
                      variant="ghost"
                      size="sm"
                      onClick={() => setConfirmDelete(a)}
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

      <SplitArtistModal
        artist={splitTarget}
        open={!!splitTarget}
        onClose={() => setSplitTarget(null)}
      />

      <MergeArtistsModal
        artists={selectedArtists}
        open={mergeOpen}
        onClose={() => setMergeOpen(false)}
        onMerged={() => setSelected(new Set())}
      />

      <Modal
        open={!!confirmDelete}
        onClose={() => setConfirmDelete(null)}
        title="Eliminar artista"
        description={
          confirmDelete
            ? `Vas a borrar "${confirmDelete.name}". Esta acción no se puede deshacer.`
            : ""
        }
        footer={
          <>
            <Button variant="ghost" onClick={() => setConfirmDelete(null)}>
              Cancelar
            </Button>
            <Button
              variant="danger"
              loading={deleteArtist.isPending}
              onClick={onDelete}
            >
              Eliminar
            </Button>
          </>
        }
      >
        <p className="text-sm text-text-muted">
          Las canciones y álbumes no se borran: solo dejan de atribuirse a este
          artista.
        </p>
      </Modal>
    </div>
  );
}
