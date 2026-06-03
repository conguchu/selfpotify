"use client";

import { use, useEffect, useRef, useState } from "react";
import { useRouter } from "next/navigation";
import { toast } from "sonner";
import { Spinner } from "@/components/ui/Spinner";
import { EmptyState } from "@/components/ui/EmptyState";
import { Button } from "@/components/ui/Button";
import { useRedeemPlaylistShareLink } from "@/lib/query/hooks";

/** Heurística de user-agent móvil, espejo del `front/middleware.ts`. */
function isMobileUserAgent(): boolean {
  if (typeof navigator === "undefined") return false;
  return /Android|webOS|iPhone|iPad|iPod|BlackBerry|IEMobile|Opera Mini|Mobile/i.test(
    navigator.userAgent,
  );
}

/**
 * Ventana de gracia para el handoff a la app nativa: si la app está instalada,
 * el SO cambia de contexto (la pestaña se oculta) antes de este plazo; si no, el
 * navegador sigue visible y continuamos con el canje web.
 */
const DEEP_LINK_FALLBACK_MS = 1200;

/**
 * Página que canjea un magic link de playlist.
 *
 * En **escritorio** canjea directamente en web: `POST /api/playlists/share/{token}`
 * y redirige a la playlist.
 *
 * En **móvil** intenta primero un *handoff* a la app nativa redirigiendo a
 * `selfpotify://playlist/share/{token}` (esquema propio; ver README → "Apertura
 * en la app móvil"). Si la app no está instalada —el navegador no cambia de
 * contexto dentro de `DEEP_LINK_FALLBACK_MS`— se continúa con el canje web. Esta
 * ruta está **exenta** del middleware que redirige los móviles a `/mobile`,
 * precisamente para poder ejecutar aquí este puente.
 */
export default function RedeemSharePage({
  params,
}: {
  params: Promise<{ token: string }>;
}) {
  const { token } = use(params);
  const router = useRouter();
  const redeem = useRedeemPlaylistShareLink();

  // En móvil mostramos un paso intermedio mientras se intenta abrir la app.
  const [tryingApp, setTryingApp] = useState(false);

  // El canje web se dispara una sola vez (StrictMode monta dos veces en dev; el
  // guard evita un doble POST que gastaría el token dos veces).
  const redeemStartedRef = useRef(false);

  function redeemOnWeb() {
    if (redeemStartedRef.current) return;
    redeemStartedRef.current = true;
    setTryingApp(false);
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
  }

  useEffect(() => {
    // Escritorio: canje web directo, como hasta ahora.
    if (!isMobileUserAgent()) {
      redeemOnWeb();
      return;
    }

    // Móvil: intentar el handoff a la app y, si no ocurre, caer al canje web.
    setTryingApp(true);

    // Si la app toma el control, la pestaña pasa a oculta: cancelamos el fallback.
    const onVisibility = () => {
      if (document.visibilityState === "hidden") {
        clearTimeout(fallback);
      }
    };
    document.addEventListener("visibilitychange", onVisibility);

    const fallback = setTimeout(redeemOnWeb, DEEP_LINK_FALLBACK_MS);

    // Lanzar el intento de deep link. Si la app no maneja el esquema, esto es
    // un no-op silencioso en la mayoría de navegadores móviles.
    window.location.href = `selfpotify://playlist/share/${token}`;

    return () => {
      clearTimeout(fallback);
      document.removeEventListener("visibilitychange", onVisibility);
    };
    // Solo al montar; el guard interno protege de re-ejecuciones.
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
      <p className="text-sm text-text-muted">
        {tryingApp
          ? "Abriendo en la app de Selfpotify…"
          : "Abriendo la playlist compartida…"}
      </p>
      {tryingApp && (
        <Button variant="ghost" onClick={redeemOnWeb}>
          Continuar en el navegador
        </Button>
      )}
    </div>
  );
}
