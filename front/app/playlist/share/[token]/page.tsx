"use client";

import { use, useEffect, useRef, useState } from "react";
import { useRouter } from "next/navigation";
import { toast } from "sonner";
import { Spinner } from "@/components/ui/Spinner";
import { EmptyState } from "@/components/ui/EmptyState";
import { Button } from "@/components/ui/Button";
import { useRedeemPlaylistShareLink } from "@/lib/query/hooks";
import { useAuthStore } from "@/lib/auth/store";

/** Heurística de user-agent móvil, espejo del `front/middleware.ts`. */
function isMobileUserAgent(): boolean {
  if (typeof navigator === "undefined") return false;
  return /Android|webOS|iPhone|iPad|iPod|BlackBerry|IEMobile|Opera Mini|Mobile/i.test(
    navigator.userAgent,
  );
}

/** `true` si el navegador corre sobre Android (soporta el esquema `intent:`). */
function isAndroid(): boolean {
  if (typeof navigator === "undefined") return false;
  return /Android/i.test(navigator.userAgent);
}

/**
 * URL `intent:` de Chrome/Samsung/Firefox en Android. El SO decide: si la app
 * está instalada la abre; si no, navega automáticamente al `browser_fallback_url`
 * (la pantalla `/mobile` con el copy de invitación). Así no hace falta heurística
 * de temporizador para detectar la instalación.
 */
function androidIntentUrl(token: string): string {
  const fallback = `${window.location.origin}/mobile?origin=playlist-share`;
  return (
    `intent://playlist/share/${token}#Intent;` +
    `scheme=selfpotify;` +
    `package=davila.anton.selfpotify;` +
    `S.browser_fallback_url=${encodeURIComponent(fallback)};` +
    `end`
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
 * **Vive fuera del grupo protegido `(app)`** a propósito: el puente al deep link
 * debe ejecutarse aunque el visitante no tenga sesión web (el caso típico de
 * quien recibe el enlace en el móvil). Si estuviera bajo `ProtectedRoute`, un
 * usuario sin sesión sería redirigido a `/login` —y el middleware lo mandaría a
 * `/mobile`— antes de poder intentar abrir la app. También está **exenta** del
 * middleware que redirige los móviles a `/mobile`.
 *
 * En **escritorio** canjea directamente en web: `POST /api/playlists/share/{token}`
 * y redirige a la playlist (requiere sesión; si no la hay, va a `/login`).
 *
 * En **Android** usa una URL `intent:` con `browser_fallback_url`: el SO abre la
 * app si está instalada y, si no, navega solo a `/mobile?origin=playlist-share`
 * (pantalla de descarga con copy de invitación). En **iOS/otros móviles** intenta
 * el esquema propio `selfpotify://playlist/share/{token}` y, si no hay handoff
 * dentro de `DEEP_LINK_FALLBACK_MS`, continúa con el canje web. Ver README →
 * "Apertura en la app móvil".
 */
export default function RedeemSharePage({
  params,
}: {
  params: Promise<{ token: string }>;
}) {
  const { token } = use(params);
  const router = useRouter();
  const redeem = useRedeemPlaylistShareLink();
  const hydrated = useAuthStore((s) => s.hydrated);
  const token_ = useAuthStore((s) => s.token);

  // En móvil mostramos un paso intermedio mientras se intenta abrir la app.
  const [tryingApp, setTryingApp] = useState(false);

  // El canje web se dispara una sola vez (StrictMode monta dos veces en dev; el
  // guard evita un doble POST que gastaría el token dos veces).
  const redeemStartedRef = useRef(false);

  function redeemOnWeb() {
    if (redeemStartedRef.current) return;
    redeemStartedRef.current = true;
    setTryingApp(false);
    // Sin sesión web no se puede canjear aquí: al login (en móvil el middleware
    // lo llevará a `/mobile` para descargar la app).
    if (!token_) {
      router.replace("/login");
      return;
    }
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
    // Esperamos a rehidratar la sesión para que el fallback web conozca el token.
    if (!hydrated) return;

    // Escritorio: canje web directo, como hasta ahora.
    if (!isMobileUserAgent()) {
      redeemOnWeb();
      return;
    }

    setTryingApp(true);

    // Android: el `intent:` resuelve por sí mismo (app o `browser_fallback_url`),
    // sin temporizadores. No hay canje web aquí: o abre la app o cae a `/mobile`.
    if (isAndroid()) {
      window.location.href = androidIntentUrl(token);
      return;
    }

    // iOS/otros: intentar el esquema propio y, si no hay handoff, caer al canje web.
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
    // Re-ejecuta al rehidratar; los guards internos protegen de duplicados.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [hydrated]);

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
    <div className="flex min-h-screen flex-col items-center justify-center gap-4 py-20">
      <Spinner size="lg" />
      <p className="text-sm text-text-muted">
        {tryingApp
          ? "Abriendo en la app de Selfpotify…"
          : "Abriendo la playlist compartida…"}
      </p>
      {tryingApp && !isAndroid() && (
        <Button variant="ghost" onClick={redeemOnWeb}>
          Continuar en el navegador
        </Button>
      )}
    </div>
  );
}
