"use client";

import Link from "next/link";
import { cn } from "@/lib/utils";

/**
 * Renderiza los artistas de una canción separados por comas. Cada artista con
 * id conocido es un enlace a su página (`/artist/{id}`); los que no tienen id
 * se muestran como texto plano. Empareja por índice `artistNames`/`artistIds`.
 *
 * `fallback` es lo que se muestra cuando no hay artistas. Si está vacío, no
 * renderiza nada.
 */
export function ArtistLinks({
  artistIds,
  artistNames,
  fallback = "",
  linkClassName,
}: {
  artistIds?: number[] | null;
  artistNames?: string[] | null;
  fallback?: string;
  linkClassName?: string;
}) {
  const artists = (artistNames ?? []).map((name, i) => ({
    name,
    id: artistIds?.[i],
  }));

  if (artists.length === 0) return <>{fallback}</>;

  return (
    <>
      {artists.map((a, i) => (
        <span key={`${a.id}-${i}`}>
          {i > 0 && ", "}
          {a.id != null ? (
            <Link
              href={`/artist/${a.id}`}
              onClick={(e) => e.stopPropagation()}
              className={cn("hover:text-text hover:underline", linkClassName)}
            >
              {a.name}
            </Link>
          ) : (
            a.name
          )}
        </span>
      ))}
    </>
  );
}
