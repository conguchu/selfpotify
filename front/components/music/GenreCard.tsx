"use client";

import Link from "next/link";
import { Disc3, Play } from "lucide-react";
import { cn } from "@/lib/utils";

/**
 * Tarjeta de género al estilo Spotify. Como un género es solo un nombre
 * (sin imagen), se representa con un mosaico de color y un icono.
 * Al pulsarla navega a la página del género (`/genre/{genre}`).
 */
export function GenreCard({
  genre,
  className,
}: {
  genre: string;
  className?: string;
}) {
  return (
    <Link
      href={`/genre/${encodeURIComponent(genre)}`}
      className={cn(
        "group relative flex flex-col items-center gap-3 rounded-lg bg-bg-card/40 p-4 transition-colors hover:bg-bg-hover",
        className,
      )}
    >
      <div className="relative">
        <div className="flex h-36 w-36 items-center justify-center rounded-full bg-gradient-to-br from-accent to-accent-soft shadow-xl">
          <Disc3 className="h-16 w-16 text-text/80" />
        </div>
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
        <p className="truncate text-sm font-semibold capitalize" title={genre}>
          {genre}
        </p>
        <p className="text-xs text-text-muted">Género</p>
      </div>
    </Link>
  );
}
