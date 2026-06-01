"use client";

import { useMemo, useState } from "react";
import { Check, Plus, Scissors, Search, X } from "lucide-react";
import { toast } from "sonner";
import { Button } from "@/components/ui/Button";
import { Input } from "@/components/ui/Input";
import { Label } from "@/components/ui/Label";
import { Modal } from "@/components/ui/Modal";
import { useArtists, useSplitArtist } from "@/lib/query/hooks";
import type { ArtistDTO } from "@/lib/types";

/**
 * Separa un artista mal etiquetado (p. ej. "Ill Pekeño / Ergo Pro") en varios
 * reales. El admin teclea los nombres (mínimo dos); cada campo tiene una lupa que
 * busca artistas ya existentes en la BBDD para reutilizarlos. El backend resuelve
 * cada nombre contra Last.fm, atribuye todas las canciones y álbumes del original
 * a TODOS los resultantes y borra el original.
 */
export function SplitArtistModal({
  artist,
  open,
  onClose,
}: {
  artist: ArtistDTO | null;
  open: boolean;
  onClose: () => void;
}) {
  const split = useSplitArtist();
  const artistsQuery = useArtists(open);
  const [names, setNames] = useState<string[]>(["", ""]);

  const reset = () => setNames(["", ""]);

  const setNameAt = (i: number, value: string) =>
    setNames((prev) => prev.map((n, idx) => (idx === i ? value : n)));

  const submit = async () => {
    if (!artist) return;
    const clean = names.map((n) => n.trim()).filter(Boolean);
    if (clean.length < 2) {
      toast.error("Introduce al menos dos nombres");
      return;
    }
    try {
      const result = await split.mutateAsync({ id: artist.id, names: clean });
      toast.success(
        `«${artist.name}» separado en ${result.length} artistas: ${result
          .map((a) => a.name)
          .join(", ")}`,
      );
      reset();
      onClose();
    } catch (err) {
      toast.error(err instanceof Error ? err.message : "No se pudo separar el artista");
    }
  };

  return (
    <Modal
      open={open}
      onClose={onClose}
      title="Separar artista"
      description="Reparte todas las canciones y álbumes del artista entre los reales que indiques. El original se borra."
      footer={
        <>
          <Button variant="ghost" onClick={onClose}>
            Cancelar
          </Button>
          <Button
            leftIcon={<Scissors className="h-4 w-4" />}
            loading={split.isPending}
            onClick={submit}
          >
            Separar
          </Button>
        </>
      }
    >
      <div className="flex flex-col gap-4">
        {/* El nombre del artista a separar, destacado: es de aquí de donde el
            admin saca los nombres reales que va a teclear abajo. */}
        <div className="rounded-md border border-border bg-bg-card/40 px-4 py-3">
          <p className="text-[11px] font-medium uppercase tracking-wide text-text-subtle">
            Artista a separar
          </p>
          <p className="break-words text-2xl font-bold leading-tight text-text">
            {artist?.name ?? "—"}
          </p>
        </div>

        <div className="flex flex-col gap-2">
          <Label>Artistas resultantes</Label>
          {names.map((name, i) => (
            <div key={i} className="flex items-start gap-2">
              <ArtistNameField
                value={name}
                onChange={(v) => setNameAt(i, v)}
                placeholder={`Nombre del artista ${i + 1}`}
                artists={artistsQuery.data ?? []}
              />
              {names.length > 2 && (
                <button
                  type="button"
                  aria-label="Quitar"
                  onClick={() => setNames((prev) => prev.filter((_, idx) => idx !== i))}
                  className="mt-2.5 text-text-muted hover:text-danger"
                >
                  <X className="h-4 w-4" />
                </button>
              )}
            </div>
          ))}
          <Button
            type="button"
            variant="ghost"
            size="sm"
            leftIcon={<Plus className="h-4 w-4" />}
            className="self-start"
            onClick={() => setNames((prev) => [...prev, ""])}
          >
            Añadir otro
          </Button>
        </div>

        <p className="text-xs text-text-subtle">
          Cada nombre se resuelve contra Last.fm (nombre canónico y MBID). Si ya
          existe un artista con ese nombre, se reutiliza en lugar de crear uno
          nuevo: usa la lupa para buscarlo.
        </p>
      </div>
    </Modal>
  );
}

/**
 * Campo de nombre con buscador integrado: al escribir muestra los artistas ya
 * existentes en la BBDD que coinciden, para reutilizarlos en lugar de duplicar.
 * Si el texto coincide exactamente con uno existente, lo señala con un check.
 */
function ArtistNameField({
  value,
  onChange,
  placeholder,
  artists,
}: {
  value: string;
  onChange: (value: string) => void;
  placeholder?: string;
  artists: ArtistDTO[];
}) {
  const [focused, setFocused] = useState(false);
  const q = value.trim().toLowerCase();

  const suggestions = useMemo(() => {
    if (!q) return [];
    return artists.filter((a) => a.name.toLowerCase().includes(q)).slice(0, 6);
  }, [artists, q]);

  const exists = useMemo(
    () => !!q && artists.some((a) => a.name.trim().toLowerCase() === q),
    [artists, q],
  );

  return (
    <div className="relative flex-1">
      <div className="relative">
        <Search className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-text-subtle" />
        <Input
          value={value}
          onChange={(e) => onChange(e.target.value)}
          onFocus={() => setFocused(true)}
          onBlur={() => setTimeout(() => setFocused(false), 120)}
          placeholder={placeholder}
          className="pl-9 pr-20"
        />
        {exists && (
          <span className="absolute right-3 top-1/2 inline-flex -translate-y-1/2 items-center gap-1 text-xs text-accent">
            <Check className="h-3.5 w-3.5" /> existe
          </span>
        )}
      </div>

      {focused && suggestions.length > 0 && (
        <ul className="absolute z-10 mt-1 max-h-56 w-full overflow-y-auto rounded-md border border-border bg-bg-elevated shadow-lg">
          {suggestions.map((a) => (
            <li key={a.id}>
              <button
                type="button"
                // onMouseDown (no onClick) para que el clic gane al blur del input.
                onMouseDown={(e) => {
                  e.preventDefault();
                  onChange(a.name);
                  setFocused(false);
                }}
                className="flex w-full items-center gap-2 px-3 py-2 text-left text-sm text-text hover:bg-bg-hover"
              >
                <Search className="h-3.5 w-3.5 shrink-0 text-text-subtle" />
                <span className="truncate">{a.name}</span>
                <span className="ml-auto text-xs text-text-subtle">
                  {a.songIds?.length ?? 0} canción(es)
                </span>
              </button>
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}
