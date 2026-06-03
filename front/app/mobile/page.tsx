import { headers } from "next/headers";
import { AppLogo } from "@/components/layout/AppLogo";

/** Enlace oficial a las releases de GitHub donde se publica la app móvil. */
const RELEASES_URL = "https://github.com/conguchu/selfpotify/releases";

/** Reconstruye la URL pública del servidor a partir de las cabeceras de la petición. */
async function serverUrl(): Promise<string> {
  const h = await headers();
  const host = h.get("x-forwarded-host") ?? h.get("host") ?? "";
  const proto = h.get("x-forwarded-proto") ?? "http";
  return host ? `${proto}://${host}` : "";
}

/**
 * Pantalla exclusiva para móviles. El frontend web no es responsive, así que el
 * middleware redirige aquí cualquier acceso desde un móvil (salvo
 * `/playlist/share/*`). Invita a descargar la app nativa desde las releases de
 * GitHub. Desde escritorio el middleware redirige a `/home`.
 *
 * Cuando se llega con `?origin=playlist-share` (fallback del `intent:` de un
 * enlace de invitación a playlist cuando la app NO está instalada) el copy cambia
 * para explicar que hay una invitación esperando, manteniendo el botón de descarga.
 */
export default async function MobilePage({
  searchParams,
}: {
  searchParams: Promise<{ origin?: string }>;
}) {
  const { origin } = await searchParams;
  const fromPlaylistShare = origin === "playlist-share";
  const server = fromPlaylistShare ? await serverUrl() : "";

  return (
    <main className="flex min-h-screen flex-col items-center justify-center gap-6 bg-bg px-6 text-center text-text">
      <AppLogo className="h-20 w-20" />
      {fromPlaylistShare ? (
        <>
          <h1 className="text-2xl font-bold">
            Te han invitado a colaborar en una playlist, ¿te lo vas a perder?
          </h1>
          <p className="max-w-sm text-text-muted">
            Instálate la app y regístrate en el servidor{" "}
            <span className="font-semibold text-text">{server}</span>.
          </p>
        </>
      ) : (
        <>
          <h1 className="text-2xl font-bold">Descarga la app de Selfpotify</h1>
          <p className="max-w-sm text-text-muted">
            La versión web no está disponible en dispositivos móviles. Descarga la
            app nativa para disfrutar de tu música desde el móvil.
          </p>
        </>
      )}
      <a
        href={RELEASES_URL}
        target="_blank"
        rel="noopener noreferrer"
        className="rounded-full bg-accent px-6 py-3 font-semibold text-on-accent transition hover:bg-accent-hover"
      >
        Descargar la app
      </a>
    </main>
  );
}
