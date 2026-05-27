"use client";

import { useEffect, useRef, useState } from "react";
import { toast } from "sonner";
import { Modal } from "@/components/ui/Modal";
import { Input } from "@/components/ui/Input";
import { Label } from "@/components/ui/Label";
import { Button } from "@/components/ui/Button";
import { Switch } from "@/components/ui/Switch";
import { ListMusic } from "lucide-react";

export interface PlaylistFormValues {
  name: string;
  description: string;
  isPublic: boolean;
}

/**
 * Formulario compartido por las modales de crear y editar playlist: nombre,
 * descripción y visibilidad. Cada caller pasa los valores iniciales, los
 * textos y el `onSubmit` con su mutación correspondiente.
 */
export function PlaylistFormModal({
  open,
  onClose,
  title,
  description,
  submitLabel,
  initial,
  loading,
  onSubmit,
  currentCoverUrl,
}: {
  open: boolean;
  onClose: () => void;
  title: string;
  description: string;
  submitLabel: string;
  initial: PlaylistFormValues;
  loading?: boolean;
  currentCoverUrl?: string | null;
  onSubmit: (values: PlaylistFormValues, coverFile: File | null) => Promise<void>;
}) {
  const [name, setName] = useState(initial.name);
  const [desc, setDesc] = useState(initial.description);
  const [isPublic, setIsPublic] = useState(initial.isPublic);
  const [coverFile, setCoverFile] = useState<File | null>(null);
  const [previewUrl, setPreviewUrl] = useState<string | null>(null);
  const fileInputRef = useRef<HTMLInputElement>(null);

  // Re-sincroniza el formulario con los valores iniciales cada vez que se abre.
  useEffect(() => {
    if (open) {
      setName(initial.name);
      setDesc(initial.description);
      setIsPublic(initial.isPublic);
      setCoverFile(null);
      setPreviewUrl(null);
    }
  }, [open, initial.name, initial.description, initial.isPublic]);

  // Limpia las URLs de preview al desmontar.
  useEffect(() => {
    return () => {
      if (previewUrl && previewUrl.startsWith("blob:")) {
        URL.revokeObjectURL(previewUrl);
      }
    };
  }, [previewUrl]);

  const handleCoverChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;

    setCoverFile(file);
    const newPreviewUrl = URL.createObjectURL(file);
    if (previewUrl && previewUrl.startsWith("blob:")) {
      URL.revokeObjectURL(previewUrl);
    }
    setPreviewUrl(newPreviewUrl);
  };

  const submit = async () => {
    const trimmed = name.trim();
    if (!trimmed) {
      toast.error("Pon un nombre a la playlist");
      return;
    }
    await onSubmit(
      { name: trimmed, description: desc.trim(), isPublic },
      coverFile
    );
  };

  return (
    <Modal
      open={open}
      onClose={onClose}
      title={title}
      description={description}
      footer={
        <>
          <Button variant="ghost" onClick={onClose}>
            Cancelar
          </Button>
          <Button onClick={submit} loading={loading}>
            {submitLabel}
          </Button>
        </>
      }
    >
      <div className="flex flex-col gap-4">
        <div className="flex flex-col gap-1.5">
          <Label>Carátula</Label>
          <button
            type="button"
            onClick={() => fileInputRef.current?.click()}
            className="relative h-24 w-24 flex-shrink-0 rounded-md border-2 border-dashed border-border bg-bg-card transition-colors hover:border-accent hover:bg-bg-hover"
          >
            {previewUrl && (
              <img
                src={previewUrl}
                alt="Vista previa de carátula"
                className="h-full w-full rounded-[5px] object-cover"
              />
            )}
            {!previewUrl && currentCoverUrl && (
              <img
                src={currentCoverUrl}
                alt="Vista previa de carátula"
                className="h-full w-full rounded-[5px] object-cover"
              />
            )}
            {!previewUrl && !currentCoverUrl && (
              <div className="flex h-full w-full items-center justify-center">
                <ListMusic className="h-8 w-8 text-text-muted" />
              </div>
            )}
          </button>
          <input
            ref={fileInputRef}
            type="file"
            accept="image/*"
            hidden
            onChange={handleCoverChange}
          />
          <p className="text-xs text-text-muted">
            Haz clic para cambiar (se recortará al cuadrado)
          </p>
        </div>

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
            value={desc}
            onChange={(e) => setDesc(e.target.value)}
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
