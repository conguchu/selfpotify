"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import { AlertTriangle } from "lucide-react";
import { toast } from "sonner";
import { Button } from "@/components/ui/Button";
import { Card, CardDescription, CardTitle } from "@/components/ui/Card";
import { Input } from "@/components/ui/Input";
import { Label } from "@/components/ui/Label";
import { Modal } from "@/components/ui/Modal";
import { useResetServer } from "@/lib/query/hooks";
import { useAuthStore } from "@/lib/auth/store";

/**
 * Zona de peligro del panel: reset total del servidor. Borra la BBDD y la
 * configuración (vuelve al estado de primer despliegue, reseedeando el admin del
 * .env). Tras el reset se cierra la sesión, porque el usuario actual deja de
 * existir y el setup vuelve a estar pendiente.
 */
export function DangerZone() {
  const router = useRouter();
  const reset = useResetServer();
  const logout = useAuthStore((s) => s.logout);
  const [open, setOpen] = useState(false);
  const [confirmText, setConfirmText] = useState("");

  const onReset = async () => {
    try {
      const res = await reset.mutateAsync();
      logout();
      toast.success(res.message ?? "Servidor reseteado");
      router.replace("/login");
    } catch (err) {
      toast.error(err instanceof Error ? err.message : "No se pudo resetear");
    }
  };

  return (
    <>
      <Card className="border-danger/40">
        <div className="flex items-center gap-2">
          <AlertTriangle className="h-5 w-5 text-danger" />
          <CardTitle className="text-danger">Zona de peligro</CardTitle>
        </div>
        <CardDescription className="mb-4 mt-1">
          Resetea el servidor: borra todas las canciones, artistas, álbumes,
          playlists, usuarios y la configuración. El administrador del{" "}
          <code>.env</code> se vuelve a crear. Esta acción no se puede deshacer.
        </CardDescription>
        <Button
          variant="danger"
          leftIcon={<AlertTriangle className="h-4 w-4" />}
          onClick={() => {
            setConfirmText("");
            setOpen(true);
          }}
        >
          Resetear servidor
        </Button>
      </Card>

      <Modal
        open={open}
        onClose={() => setOpen(false)}
        title="Resetear el servidor"
        description="Vas a borrar TODOS los datos y la configuración. No hay vuelta atrás."
        footer={
          <>
            <Button variant="ghost" onClick={() => setOpen(false)}>
              Cancelar
            </Button>
            <Button
              variant="danger"
              loading={reset.isPending}
              disabled={confirmText !== "RESET"}
              onClick={onReset}
            >
              Resetear todo
            </Button>
          </>
        }
      >
        <div className="flex flex-col gap-1.5">
          <Label htmlFor="reset-confirm">
            Escribe <span className="font-mono text-danger">RESET</span> para
            confirmar
          </Label>
          <Input
            id="reset-confirm"
            value={confirmText}
            onChange={(e) => setConfirmText(e.target.value)}
            placeholder="RESET"
            autoFocus
          />
        </div>
      </Modal>
    </>
  );
}
