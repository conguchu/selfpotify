"use client";

import Link from "next/link";
import { ListMusic, Lock, Users } from "lucide-react";
import { resolveImageUrl } from "@/lib/image";
import type { PlaylistDTO } from "@/lib/types";
import { cn, pluralize } from "@/lib/utils";

/**
 * Tarjeta cuadrada de playlist al estilo Spotify, pensada para grids de
 * resultados. Variante "card" de {@code PlaylistItem} (que es de sidebar).
 * Al pulsarla navega a la página de la playlist.
 */
export function PlaylistCard({
  playlist,
  className,
}: {
  playlist: PlaylistDTO;
  className?: string;
}) {
  const songCount = playlist.songIds?.length ?? 0;
  const resolved = resolveImageUrl(playlist.pictureUrl);
  // Una playlist con colaboradores se marca como compartida. `collaboratorIds`
  // solo llega poblado en listados propios (/my, /shared); en perfiles ajenos
  // es null y el icono no se pinta, que es justo lo que queremos.
  const isShared = (playlist.collaboratorIds?.length ?? 0) > 0;
  return (
    <Link
      href={`/playlist/${playlist.id}`}
      className={cn(
        "group relative flex flex-col gap-3 rounded-lg bg-bg-card/40 p-4 transition-colors hover:bg-bg-hover",
        className,
      )}
    >
      <div className="relative aspect-square w-full overflow-hidden rounded-md border border-border bg-bg-card">
        {resolved ? (
          // Usamos <img> en vez de next/image: la URL puede venir de un asset
          // del backend o de un CDN externo y aquí no necesitamos optimización.
          // eslint-disable-next-line @next/next/no-img-element
          <img
            src={resolved}
            alt={playlist.name}
            className="h-full w-full object-cover"
          />
        ) : (
          <div className="flex h-full w-full items-center justify-center bg-gradient-to-br from-accent/30 to-bg-card text-accent-text">
            <ListMusic className="h-14 w-14" />
          </div>
        )}
        {!playlist.isPublic ? (
          <span
            aria-label="Privada"
            className="absolute right-2 top-2 flex items-center gap-1 rounded bg-bg-elevated/85 px-1.5 py-0.5 text-[10px] font-medium text-text-muted"
          >
            <Lock className="h-3 w-3" />
            Privada
          </span>
        ) : null}
        {isShared ? (
          <span
            aria-label="Compartida"
            title="Playlist compartida"
            className="absolute left-2 top-2 flex items-center gap-1 rounded bg-bg-elevated/85 px-1.5 py-0.5 text-[10px] font-medium text-text-muted"
          >
            <Users className="h-3 w-3" />
            Compartida
          </span>
        ) : null}
      </div>
      <div className="min-w-0">
        <p className="truncate text-sm font-semibold" title={playlist.name}>
          {playlist.name}
        </p>
        <p className="truncate text-xs text-text-muted">
          {songCount} {pluralize(songCount, "canción", "canciones")}
        </p>
      </div>
    </Link>
  );
}
