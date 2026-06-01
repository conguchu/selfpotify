"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import { Save, Trash2 } from "lucide-react";
import { toast } from "sonner";
import { Button } from "@/components/ui/Button";
import { Input } from "@/components/ui/Input";
import { Label } from "@/components/ui/Label";
import { Modal } from "@/components/ui/Modal";
import { CoverDropzone } from "@/components/admin/CoverDropzone";
import { useDeleteArtist, useUpdateArtist } from "@/lib/query/hooks";
import type { ArtistDTO } from "@/lib/types";

/**
 * Edición manual de un artista: nombre y foto. La foto se sube por drag&drop al
 * mismo almacén que las carátulas (/assets/covers) y se guarda su URL. El MBID no
 * se toca: es identidad resuelta automáticamente por el escaneo/subida. También
 * permite borrar el artista (sus canciones no se borran, solo dejan de
 * atribuírsele).
 */
export function ArtistEditForm({ artist }: { artist: ArtistDTO }) {
  const router = useRouter();
  const update = useUpdateArtist();
  const remove = useDeleteArtist();

  const [name, setName] = useState(artist.name ?? "");
  const [photoUrl, setPhotoUrl] = useState<string | null>(artist.photoUrl ?? null);
  const [confirmDelete, setConfirmDelete] = useState(false);

  const submit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!name.trim()) {
      toast.error("El nombre es obligatorio");
      return;
    }
    try {
      await update.mutateAsync({
        id: artist.id,
        payload: { name: name.trim(), photoUrl: photoUrl || null },
      });
      toast.success("Artista actualizado");
      router.push("/admin/artists");
    } catch (err) {
      toast.error(err instanceof Error ? err.message : "Error al guardar");
    }
  };

  const onDelete = async () => {
    try {
      await remove.mutateAsync(artist.id);
      toast.success("Artista eliminado");
      router.push("/admin/artists");
    } catch (err) {
      toast.error(err instanceof Error ? err.message : "Error al eliminar");
    }
  };

  return (
    <form onSubmit={submit} className="flex flex-col gap-4">
      <div className="flex flex-col gap-1.5">
        <Label>Foto</Label>
        <CoverDropzone value={photoUrl} onChange={setPhotoUrl} />
      </div>

      <div className="flex flex-col gap-1.5">
        <Label htmlFor="ae-name">Nombre *</Label>
        <Input
          id="ae-name"
          value={name}
          onChange={(e) => setName(e.target.value)}
          autoFocus
        />
      </div>

      <div className="flex flex-wrap items-center gap-3">
        <Button
          type="submit"
          leftIcon={<Save className="h-4 w-4" />}
          loading={update.isPending}
        >
          Guardar cambios
        </Button>
        <Button
          type="button"
          variant="ghost"
          onClick={() => router.push("/admin/artists")}
          disabled={update.isPending}
        >
          Cancelar
        </Button>
        <Button
          type="button"
          variant="danger"
          className="ml-auto"
          leftIcon={<Trash2 className="h-4 w-4" />}
          onClick={() => setConfirmDelete(true)}
        >
          Eliminar
        </Button>
      </div>

      <Modal
        open={confirmDelete}
        onClose={() => setConfirmDelete(false)}
        title="Eliminar artista"
        description={`Vas a borrar "${artist.name}". Esta acción no se puede deshacer.`}
        footer={
          <>
            <Button variant="ghost" onClick={() => setConfirmDelete(false)}>
              Cancelar
            </Button>
            <Button variant="danger" loading={remove.isPending} onClick={onDelete}>
              Eliminar
            </Button>
          </>
        }
      >
        <p className="text-sm text-text-muted">
          Las canciones y álbumes no se borran: solo dejan de atribuirse a este
          artista. Para repartir su música entre otros artistas usa «Separar»; para
          unificar duplicados usa «Juntar».
        </p>
      </Modal>
    </form>
  );
}
