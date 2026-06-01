"use client";

import { useState } from "react";
import { Plus, Scissors, X } from "lucide-react";
import { toast } from "sonner";
import { Button } from "@/components/ui/Button";
import { Input } from "@/components/ui/Input";
import { Label } from "@/components/ui/Label";
import { Modal } from "@/components/ui/Modal";
import { useSplitArtist } from "@/lib/query/hooks";
import type { ArtistDTO } from "@/lib/types";

/**
 * Separa un artista mal etiquetado (p. ej. "Ill Pekeño / Ergo Pro") en varios
 * reales. El admin teclea los nombres (mínimo dos); el backend los resuelve
 * contra Last.fm, atribuye todas las canciones y álbumes del original a TODOS los
 * resultantes y borra el original.
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
      description={
        artist
          ? `Reparte todas las canciones y álbumes de «${artist.name}» entre los artistas que indiques. El original se borra.`
          : ""
      }
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
      <div className="flex flex-col gap-3">
        <Label>Artistas resultantes</Label>
        {names.map((name, i) => (
          <div key={i} className="flex items-center gap-2">
            <Input
              value={name}
              onChange={(e) => setNameAt(i, e.target.value)}
              placeholder={`Nombre del artista ${i + 1}`}
            />
            {names.length > 2 && (
              <button
                type="button"
                aria-label="Quitar"
                onClick={() => setNames((prev) => prev.filter((_, idx) => idx !== i))}
                className="text-text-muted hover:text-danger"
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
        <p className="text-xs text-text-subtle">
          Cada nombre se resuelve contra Last.fm (nombre canónico y MBID), igual que
          al escanear. Si un nombre ya existe como artista, se reutiliza.
        </p>
      </div>
    </Modal>
  );
}
