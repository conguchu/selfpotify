"use client";

import { useEffect, useState } from "react";
import { Save } from "lucide-react";
import { toast } from "sonner";
import { Button } from "@/components/ui/Button";
import { Input } from "@/components/ui/Input";
import { Label } from "@/components/ui/Label";
import { Modal } from "@/components/ui/Modal";
import { CoverDropzone } from "@/components/admin/CoverDropzone";
import { useUpdateAlbum } from "@/lib/query/hooks";
import type { AlbumDTO } from "@/lib/types";

/**
 * Edita el nombre y la portada de un álbum. La portada se sube por drag&drop al
 * mismo almacén que las carátulas (/assets/covers) y se guarda su URL.
 */
export function AlbumEditModal({
  album,
  open,
  onClose,
}: {
  album: AlbumDTO | null;
  open: boolean;
  onClose: () => void;
}) {
  const update = useUpdateAlbum();
  const [name, setName] = useState("");
  const [photoUrl, setPhotoUrl] = useState<string | null>(null);

  useEffect(() => {
    if (open && album) {
      setName(album.name ?? "");
      setPhotoUrl(album.pictureUrl ?? null);
    }
  }, [open, album]);

  const submit = async () => {
    if (!album) return;
    if (!name.trim()) {
      toast.error("El nombre es obligatorio");
      return;
    }
    try {
      await update.mutateAsync({
        id: album.id,
        payload: { name: name.trim(), photoUrl: photoUrl || null },
      });
      toast.success("Álbum actualizado");
      onClose();
    } catch (err) {
      toast.error(err instanceof Error ? err.message : "Error al guardar");
    }
  };

  return (
    <Modal
      open={open}
      onClose={onClose}
      title="Editar álbum"
      footer={
        <>
          <Button variant="ghost" onClick={onClose}>
            Cancelar
          </Button>
          <Button
            leftIcon={<Save className="h-4 w-4" />}
            loading={update.isPending}
            onClick={submit}
          >
            Guardar
          </Button>
        </>
      }
    >
      <div className="flex flex-col gap-4">
        <div className="flex flex-col gap-1.5">
          <Label>Portada</Label>
          <CoverDropzone value={photoUrl} onChange={setPhotoUrl} />
        </div>
        <div className="flex flex-col gap-1.5">
          <Label htmlFor="album-name">Nombre *</Label>
          <Input
            id="album-name"
            value={name}
            onChange={(e) => setName(e.target.value)}
          />
        </div>
      </div>
    </Modal>
  );
}
