"use client";

import Link from "next/link";
import { Play } from "lucide-react";
import { Avatar } from "@/components/ui/Avatar";
import type { ArtistDTO } from "@/lib/types";
import { cn } from "@/lib/utils";

/**
 * Tarjeta de artista al estilo Spotify: foto circular, nombre y etiqueta.
 * Al pulsarla navega a la página del artista (`/artist/{id}`).
 */
export function ArtistCard({
  artist,
  className,
}: {
  artist: ArtistDTO;
  className?: string;
}) {
  return (
    <Link
      href={`/artist/${artist.id}`}
      className={cn(
        "group relative flex flex-col items-center gap-3 rounded-lg bg-bg-card/40 p-4 transition-colors hover:bg-bg-hover",
        className,
      )}
    >
      <div className="relative">
        <Avatar
          src={artist.photoUrl}
          alt={artist.name}
          size="lg"
          className="h-36 w-36 shadow-xl"
        />
        <span
          aria-hidden
          className={cn(
            "absolute bottom-1 right-1 flex h-12 w-12 items-center justify-center rounded-full",
            "bg-accent text-text shadow-xl transition-all",
            "translate-y-2 opacity-0 group-hover:translate-y-0 group-hover:opacity-100",
          )}
        >
          <Play className="h-5 w-5" fill="currentColor" />
        </span>
      </div>
      <div className="w-full min-w-0 text-center">
        <p className="truncate text-sm font-semibold" title={artist.name}>
          {artist.name}
        </p>
        <p className="text-xs text-text-muted">Artista</p>
      </div>
    </Link>
  );
}
