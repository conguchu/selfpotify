"use client";

import { useEffect, useState } from "react";
import { Merge } from "lucide-react";
import { toast } from "sonner";
import { Avatar } from "@/components/ui/Avatar";
import { Button } from "@/components/ui/Button";
import { Input } from "@/components/ui/Input";
import { Label } from "@/components/ui/Label";
import { Modal } from "@/components/ui/Modal";
import { useMergeArtists } from "@/lib/query/hooks";
import type { ArtistDTO } from "@/lib/types";

/**
 * Une varios artistas duplicados en uno solo. El admin elige cuál es el
 * superviviente (conserva su id y su MBID); el resto se borra tras traspasarle
 * sus canciones y álbumes. Opcionalmente se renombra al superviviente.
 */
export function MergeArtistsModal({
  artists,
  open,
  onClose,
  onMerged,
}: {
  artists: ArtistDTO[];
  open: boolean;
  onClose: () => void;
  onMerged?: () => void;
}) {
  const merge = useMergeArtists();
  const [survivorId, setSurvivorId] = useState<number | null>(null);
  const [name, setName] = useState("");

  // Al abrir (o cambiar la selección), el superviviente por defecto es el primero
  // y el nombre final se precarga con el suyo.
  useEffect(() => {
    if (open && artists.length > 0) {
      setSurvivorId((prev) =>
        prev != null && artists.some((a) => a.id === prev) ? prev : artists[0].id,
      );
    }
  }, [open, artists]);

  useEffect(() => {
    const survivor = artists.find((a) => a.id === survivorId);
    if (survivor) setName(survivor.name ?? "");
  }, [survivorId, artists]);

  const submit = async () => {
    if (survivorId == null) {
      toast.error("Elige el artista superviviente");
      return;
    }
    try {
      const result = await merge.mutateAsync({
        ids: artists.map((a) => a.id),
        survivorId,
        name: name.trim() || undefined,
      });
      toast.success(`${artists.length} artistas unidos en «${result.name}»`);
      onMerged?.();
      onClose();
    } catch (err) {
      toast.error(err instanceof Error ? err.message : "No se pudo juntar los artistas");
    }
  };

  return (
    <Modal
      open={open}
      onClose={onClose}
      title="Juntar artistas"
      description="Unifica los artistas seleccionados en uno solo. El superviviente conserva su identidad (MBID) y absorbe las canciones y álbumes del resto, que se borran."
      footer={
        <>
          <Button variant="ghost" onClick={onClose}>
            Cancelar
          </Button>
          <Button
            leftIcon={<Merge className="h-4 w-4" />}
            loading={merge.isPending}
            onClick={submit}
            disabled={artists.length < 2}
          >
            Juntar
          </Button>
        </>
      }
    >
      <div className="flex flex-col gap-4">
        <div className="flex flex-col gap-2">
          <Label>Superviviente</Label>
          <ul className="divide-y divide-border rounded-md border border-border">
            {artists.map((a) => (
              <li key={a.id}>
                <label className="flex cursor-pointer items-center gap-3 px-3 py-2 hover:bg-bg-hover">
                  <input
                    type="radio"
                    name="survivor"
                    className="accent-accent"
                    checked={survivorId === a.id}
                    onChange={() => setSurvivorId(a.id)}
                  />
                  <Avatar src={a.photoUrl} alt={a.name} size="sm" />
                  <span className="truncate text-sm text-text">{a.name}</span>
                  <span className="ml-auto text-xs text-text-subtle">
                    {a.songIds?.length ?? 0} canción(es)
                  </span>
                </label>
              </li>
            ))}
          </ul>
        </div>

        <div className="flex flex-col gap-1.5">
          <Label htmlFor="merge-name">Nombre final</Label>
          <Input
            id="merge-name"
            value={name}
            onChange={(e) => setName(e.target.value)}
            placeholder="Nombre del artista unificado"
          />
        </div>
      </div>
    </Modal>
  );
}
