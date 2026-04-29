"use client";

import { useState } from "react";
import { toast } from "sonner";
import { Modal } from "@/components/ui/Modal";
import { Input } from "@/components/ui/Input";
import { Label } from "@/components/ui/Label";
import { Button } from "@/components/ui/Button";
import { Switch } from "@/components/ui/Switch";
import { useCreatePlaylist } from "@/lib/query/hooks";

export function CreatePlaylistModal({
  open,
  onClose,
}: {
  open: boolean;
  onClose: () => void;
}) {
  const [name, setName] = useState("");
  const [description, setDescription] = useState("");
  const [isPublic, setIsPublic] = useState(false);
  const create = useCreatePlaylist();

  const reset = () => {
    setName("");
    setDescription("");
    setIsPublic(false);
  };

  const submit = async () => {
    const trimmed = name.trim();
    if (!trimmed) {
      toast.error("Pon un nombre a la playlist");
      return;
    }
    try {
      await create.mutateAsync({
        name: trimmed,
        description: description.trim() || undefined,
        isPublic,
        songIds: [],
      });
      toast.success("Playlist creada");
      reset();
      onClose();
    } catch (err) {
      toast.error(err instanceof Error ? err.message : "Error al crear playlist");
    }
  };

  return (
    <Modal
      open={open}
      onClose={() => {
        reset();
        onClose();
      }}
      title="Nueva playlist"
      description="Crea una colección vacía y añade canciones después."
      footer={
        <>
          <Button variant="ghost" onClick={onClose}>
            Cancelar
          </Button>
          <Button onClick={submit} loading={create.isPending}>
            Crear
          </Button>
        </>
      }
    >
      <div className="flex flex-col gap-4">
        <div className="flex flex-col gap-1.5">
          <Label htmlFor="playlist-name">Nombre</Label>
          <Input
            id="playlist-name"
            value={name}
            onChange={(e) => setName(e.target.value)}
            placeholder="Mi mix"
            autoFocus
          />
        </div>
        <div className="flex flex-col gap-1.5">
          <Label htmlFor="playlist-desc">Descripción (opcional)</Label>
          <Input
            id="playlist-desc"
            value={description}
            onChange={(e) => setDescription(e.target.value)}
            placeholder="Para conducir, gym..."
          />
        </div>
        <div className="flex items-center justify-between rounded-md border border-border bg-bg-card px-3 py-2">
          <div>
            <p className="text-sm font-medium text-text">Pública</p>
            <p className="text-xs text-text-muted">
              Otros usuarios podrán verla.
            </p>
          </div>
          <Switch
            ariaLabel="Hacer pública la playlist"
            checked={isPublic}
            onChange={setIsPublic}
          />
        </div>
      </div>
    </Modal>
  );
}
