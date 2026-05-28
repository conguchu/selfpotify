"use client";

import Link from "next/link";
import { CoverArt } from "./CoverArt";
import type { AlbumDTO } from "@/lib/types";
import { cn, pluralize } from "@/lib/utils";

/**
 * Tarjeta de álbum al estilo Spotify. Foto cuadrada, nombre y conteo de
 * canciones. Al pulsarla navega a la página del álbum.
 *
 * (Hoy `/album/{id}` no existe como ruta; el href está preparado para cuando
 * se añada y, mientras tanto, devuelve un 404 sin romper el resto de la UI.)
 */
export function AlbumCard({
  album,
  className,
}: {
  album: AlbumDTO;
  className?: string;
}) {
  const songCount = album.songIds?.length ?? 0;
  return (
    <Link
      href={`/album/${album.id}`}
      className={cn(
        "group relative flex flex-col gap-3 rounded-lg bg-bg-card/40 p-4 transition-colors hover:bg-bg-hover",
        className,
      )}
    >
      <CoverArt
        src={album.pictureUrl}
        alt={album.name}
        size="lg"
        rounded="md"
        className="h-auto w-full aspect-square"
      />
      <div className="min-w-0">
        <p className="truncate text-sm font-semibold" title={album.name}>
          {album.name}
        </p>
        <p className="truncate text-xs text-text-muted">
          {songCount} {pluralize(songCount, "canción", "canciones")}
        </p>
      </div>
    </Link>
  );
}
