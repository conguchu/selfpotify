"use client";

import * as React from "react";
import { Copy, Minus, RefreshCw } from "lucide-react";
import { toast } from "sonner";
import { Modal } from "@/components/ui/Modal";
import { Button } from "@/components/ui/Button";
import { Input } from "@/components/ui/Input";
import { Spinner } from "@/components/ui/Spinner";
import { Avatar } from "@/components/ui/Avatar";
import { IconButton } from "@/components/ui/IconButton";
import {
  useCreatePlaylistShareLink,
  usePlaylistCollaborators,
  useRemovePlaylistCollaborator,
} from "@/lib/query/hooks";

/**
 * Modal para compartir una playlist: genera un magic link de un solo uso
 * (`POST /api/playlists/{id}/share`) para invitar a colaborar y muestra la
 * lista de colaboradores actuales, cada uno con un botón para quitarlo.
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
  const collaboratorsQuery = usePlaylistCollaborators(playlistId, open);
  const removeCollaborator = useRemovePlaylistCollaborator();
  const [token, setToken] = React.useState<string | null>(null);
  const [removingId, setRemovingId] = React.useState<number | null>(null);

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

  const onRemove = async (userId: number, name: string) => {
    setRemovingId(userId);
    try {
      await removeCollaborator.mutateAsync({ playlistId, userId });
      toast.success(`Has quitado a ${name}`);
    } catch (err) {
      toast.error(
        err instanceof Error ? err.message : "No se pudo quitar al colaborador",
      );
    } finally {
      setRemovingId(null);
    }
  };

  const collaborators = collaboratorsQuery.data ?? [];

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
      <div className="flex flex-col gap-5">
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

        <div className="flex flex-col gap-2">
          <h3 className="text-sm font-semibold text-text">Colaboradores</h3>
          {collaboratorsQuery.isLoading ? (
            <div className="flex items-center justify-center py-4">
              <Spinner />
            </div>
          ) : collaborators.length === 0 ? (
            <p className="text-sm text-text-muted">
              Todavía no hay colaboradores.
            </p>
          ) : (
            <ul className="-mx-2 flex max-h-60 flex-col gap-0.5 overflow-y-auto">
              {collaborators.map((c) => {
                const name = c.displayName?.trim() || c.username;
                const busy = removingId === c.id;
                return (
                  <li
                    key={c.id}
                    className="flex items-center justify-between gap-3 rounded-md px-2 py-1.5"
                  >
                    <span className="flex min-w-0 items-center gap-2">
                      <Avatar src={c.avatarUrl} alt={name} size="sm" />
                      <span className="min-w-0">
                        <span className="block truncate text-sm font-medium text-text">
                          {name}
                        </span>
                        <span className="block truncate text-xs text-text-muted">
                          @{c.username}
                        </span>
                      </span>
                    </span>
                    <IconButton
                      label={`Quitar a ${name}`}
                      variant="ghost"
                      size="sm"
                      onClick={() => onRemove(c.id, name)}
                      disabled={busy}
                    >
                      {busy ? <Spinner size="sm" /> : <Minus />}
                    </IconButton>
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
