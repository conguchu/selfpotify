"use client";

import { use, useEffect, useRef } from "react";
import { useRouter } from "next/navigation";
import { toast } from "sonner";
import { Spinner } from "@/components/ui/Spinner";
import { EmptyState } from "@/components/ui/EmptyState";
import { Button } from "@/components/ui/Button";
import { useRedeemPlaylistShareLink } from "@/lib/query/hooks";

/**
 * Página que canjea un magic link de playlist. Al abrirla un usuario
 * autenticado (la ruta vive bajo el grupo protegido), se llama a
 * `POST /api/playlists/share/{token}` y se redirige a la playlist. Si el token
 * es inválido o ya se usó, se muestra el error.
 */
export default function RedeemSharePage({
  params,
}: {
  params: Promise<{ token: string }>;
}) {
  const { token } = use(params);
  const router = useRouter();
  const redeem = useRedeemPlaylistShareLink();

  // El canje se dispara una sola vez por montaje (StrictMode monta dos veces en
  // dev; el guard evita un doble POST que gastaría el token dos veces).
  const startedRef = useRef(false);
  useEffect(() => {
    if (startedRef.current) return;
    startedRef.current = true;
    redeem.mutate(token, {
      onSuccess: (playlist) => {
        toast.success(`Te has unido a «${playlist.name}»`);
        router.replace(`/playlist/${playlist.id}`);
      },
      onError: (err) =>
        toast.error(
          err instanceof Error ? err.message : "No se pudo abrir el enlace",
        ),
    });
    // Solo debe ejecutarse al montar; el guard protege de re-ejecuciones.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  if (redeem.isError) {
    return (
      <EmptyState
        title="Enlace no válido"
        description={
          redeem.error instanceof Error
            ? redeem.error.message
            : "Este enlace de invitación no es válido o ya se ha usado."
        }
        action={
          <Button variant="primary" onClick={() => router.replace("/home")}>
            Ir al inicio
          </Button>
        }
      />
    );
  }

  return (
    <div className="flex flex-col items-center justify-center gap-4 py-20">
      <Spinner size="lg" />
      <p className="text-sm text-text-muted">Abriendo la playlist compartida…</p>
    </div>
  );
}
