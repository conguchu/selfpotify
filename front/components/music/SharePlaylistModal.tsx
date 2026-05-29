"use client";

import * as React from "react";
import { Copy, RefreshCw } from "lucide-react";
import { toast } from "sonner";
import { Modal } from "@/components/ui/Modal";
import { Button } from "@/components/ui/Button";
import { Input } from "@/components/ui/Input";
import { Spinner } from "@/components/ui/Spinner";
import { useCreatePlaylistShareLink } from "@/lib/query/hooks";

/**
 * Modal para invitar a colaborar en una playlist. Al abrirse genera un magic
 * link de un solo uso (`POST /api/playlists/{id}/share`) que el dueño copia y
 * comparte. Cada enlace sirve para una persona; "Generar otro enlace" crea uno
 * nuevo para invitar a más gente.
 */
export function SharePlaylistModal({
  playlistId,
  open,
  onClose,
}: {
  playlistId: number;
  open: boolean;
  onClose: () => void;
}) {
  const createLink = useCreatePlaylistShareLink();
  const [token, setToken] = React.useState<string | null>(null);

  const generate = React.useCallback(() => {
    createLink.mutate(playlistId, {
      onSuccess: (res) => setToken(res.token),
      onError: (err) =>
        toast.error(
          err instanceof Error ? err.message : "No se pudo generar el enlace",
        ),
    });
  }, [createLink, playlistId]);

  // Genera un enlace fresco cada vez que se abre el modal y limpia el anterior
  // al cerrarse (el guard `wasOpen` evita regenerar en cada render mientras
  // sigue abierto).
  const wasOpen = React.useRef(false);
  React.useEffect(() => {
    if (open && !wasOpen.current) {
      wasOpen.current = true;
      setToken(null);
      generate();
    } else if (!open) {
      wasOpen.current = false;
      setToken(null);
    }
  }, [open, generate]);

  const shareUrl =
    token && typeof window !== "undefined"
      ? `${window.location.origin}/playlist/share/${token}`
      : "";

  const onCopy = async () => {
    if (!shareUrl) return;
    try {
      await navigator.clipboard.writeText(shareUrl);
      toast.success("Enlace copiado al portapapeles");
    } catch {
      toast.error("No se pudo copiar el enlace");
    }
  };

  return (
    <Modal
      open={open}
      onClose={onClose}
      title="Compartir playlist"
      description="Comparte este enlace para invitar a alguien a colaborar. Es de un solo uso: cuando lo abran, se unirán como colaboradores y podrán añadir y quitar canciones."
      footer={
        <Button variant="ghost" onClick={onClose}>
          Listo
        </Button>
      }
    >
      {createLink.isPending && !token ? (
        <div className="flex items-center justify-center py-8">
          <Spinner size="lg" />
        </div>
      ) : (
        <div className="flex flex-col gap-3">
          <div className="flex items-center gap-2">
            <Input
              readOnly
              value={shareUrl}
              onFocus={(e) => e.currentTarget.select()}
              aria-label="Enlace de invitación"
            />
            <Button
              variant="secondary"
              leftIcon={<Copy className="h-4 w-4" />}
              onClick={onCopy}
              disabled={!shareUrl}
            >
              Copiar
            </Button>
          </div>
          <button
            type="button"
            onClick={generate}
            disabled={createLink.isPending}
            className="inline-flex w-fit items-center gap-1.5 text-xs text-text-muted transition-colors hover:text-text disabled:cursor-not-allowed disabled:opacity-60"
          >
            <RefreshCw className="h-3.5 w-3.5" />
            Generar otro enlace
          </button>
        </div>
      )}
    </Modal>
  );
}
