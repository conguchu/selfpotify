"use client";

import { useMemo, useState } from "react";
import { Search, Plus, Check } from "lucide-react";
import { toast } from "sonner";
import { Modal } from "@/components/ui/Modal";
import { Input } from "@/components/ui/Input";
import { Button } from "@/components/ui/Button";
import { Spinner } from "@/components/ui/Spinner";
import { useArtists, useCreateArtist } from "@/lib/query/hooks";

export interface PickedArtist {
  id: number;
  name: string;
}

/**
 * Modal de selección de artista: busca entre los existentes en la BBDD y, si no
 * está, permite crearlo con el nombre tecleado. Reutilizado por la subida de
 * canciones y por la edición de una canción.
 */
export function ArtistPickerModal({
  open,
  onClose,
  onSelect,
  currentArtistId,
}: {
  open: boolean;
  onClose: () => void;
  onSelect: (artist: PickedArtist) => void;
  currentArtistId?: number | null;
}) {
  const artistsQuery = useArtists(open);
  const createArtist = useCreateArtist();
  const [query, setQuery] = useState("");

  const artists = artistsQuery.data ?? [];
  const filtered = useMemo(() => {
    const q = query.trim().toLowerCase();
    if (!q) return artists;
    return artists.filter((a) => a.name.toLowerCase().includes(q));
  }, [artists, query]);

  const exactExists = useMemo(
    () =>
      artists.some(
        (a) => a.name.trim().toLowerCase() === query.trim().toLowerCase(),
      ),
    [artists, query],
  );

  const onCreate = async () => {
    const name = query.trim();
    if (!name) return;
    try {
      const created = await createArtist.mutateAsync(name);
      toast.success(`Artista "${created.name}" creado`);
      onSelect({ id: created.id, name: created.name });
      onClose();
    } catch (err) {
      toast.error(err instanceof Error ? err.message : "No se pudo crear el artista");
    }
  };

  return (
    <Modal open={open} onClose={onClose} title="Elegir artista">
      <div className="flex flex-col gap-3">
        <div className="relative">
          <Search className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-text-subtle" />
          <Input
            value={query}
            onChange={(e) => setQuery(e.target.value)}
            placeholder="Buscar o escribir un nombre nuevo"
            className="pl-9"
            autoFocus
          />
        </div>

        {/* Crear si el nombre tecleado no existe exactamente */}
        {query.trim() && !exactExists && (
          <Button
            type="button"
            variant="secondary"
            onClick={onCreate}
            loading={createArtist.isPending}
            leftIcon={<Plus className="h-4 w-4" />}
            className="self-start"
          >
            Crear «{query.trim()}»
          </Button>
        )}

        <div className="max-h-72 overflow-y-auto rounded-md border border-border">
          {artistsQuery.isLoading ? (
            <div className="flex items-center justify-center py-8">
              <Spinner />
            </div>
          ) : filtered.length === 0 ? (
            <p className="px-3 py-6 text-center text-sm text-text-muted">
              No hay artistas que coincidan.
            </p>
          ) : (
            <ul className="divide-y divide-border">
              {filtered.map((a) => {
                const isCurrent = a.id === currentArtistId;
                return (
                  <li key={a.id}>
                    <button
                      type="button"
                      onClick={() => {
                        onSelect({ id: a.id, name: a.name });
                        onClose();
                      }}
                      className="flex w-full items-center gap-2 px-3 py-2 text-left text-sm text-text hover:bg-bg-hover"
                    >
                      <span className="truncate">{a.name}</span>
                      {isCurrent && (
                        <Check className="ml-auto h-4 w-4 shrink-0 text-accent" />
                      )}
                    </button>
                  </li>
                );
              })}
            </ul>
          )}
        </div>
      </div>
    </Modal>
  );
}
