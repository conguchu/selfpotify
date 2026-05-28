"use client";

import { use, useMemo } from "react";
import Link from "next/link";
import { ArrowLeft, Disc3, Music, Play } from "lucide-react";
import { CoverArt } from "@/components/music/CoverArt";
import { Spinner } from "@/components/ui/Spinner";
import { EmptyState } from "@/components/ui/EmptyState";
import { SongRow } from "@/components/music/SongRow";
import { useAlbum, useSongs } from "@/lib/query/hooks";
import { usePlayerStore } from "@/lib/player/store";
import { cn, formatDuration, pluralize } from "@/lib/utils";

/**
 * Página de álbum (`/album/{id}`).
 *
 * El backend no expone un endpoint dedicado para "canciones de un álbum", así
 * que combinamos `GET /api/albums/{id}` (que ya da los `songIds` y la portada)
 * con `GET /api/songs` y filtramos en cliente preservando el orden de
 * `songIds` (igual que vienen de `Album.songs` en JPA). El catálogo es
 * pequeño por la propia naturaleza del proyecto, así que evitar N+1 peticiones
 * pesa más que ahorrar el listado completo.
 */
export default function AlbumPage({
  params,
}: {
  params: Promise<{ id: string }>;
}) {
  const { id } = use(params);
  const albumId = Number.parseInt(id, 10);

  const albumQuery = useAlbum(albumId);
  const songsQuery = useSongs();
  const playSong = usePlayerStore((s) => s.playSong);
  const current = usePlayerStore((s) => s.current);

  // Resuelve los SongDTO en el orden de songIds. useMemo evita recalcular en
  // cada render mientras el catálogo no cambie.
  const tracks = useMemo(() => {
    const album = albumQuery.data;
    const all = songsQuery.data;
    if (!album || !all) return [];
    const byId = new Map(all.map((s) => [s.id, s] as const));
    return album.songIds
      .map((sid) => byId.get(sid))
      .filter((s): s is NonNullable<typeof s> => Boolean(s));
  }, [albumQuery.data, songsQuery.data]);

  if (albumQuery.isLoading) {
    return (
      <div className="flex items-center justify-center py-20">
        <Spinner size="lg" />
      </div>
    );
  }

  if (albumQuery.isError || !albumQuery.data) {
    return (
      <EmptyState
        title="Álbum no encontrado"
        description="Puede que no exista o que el servidor no lo tenga registrado."
        action={
          <Link href="/home" className="text-sm text-accent-hover hover:underline">
            Volver al inicio
          </Link>
        }
      />
    );
  }

  const album = albumQuery.data;
  const totalMs = tracks.reduce((acc, s) => acc + (s.duration_ms ?? 0), 0);
  // El nombre del artista no viaja en AlbumDTO; lo deducimos de las canciones
  // (todas las del álbum suelen compartir el mismo artista principal).
  const artistNames = Array.from(
    new Set(
      tracks.flatMap((s) => s.artistNames ?? []).filter((n) => n && n.trim()),
    ),
  );

  const playAll = () => {
    if (tracks.length > 0) playSong(tracks[0], tracks);
  };

  return (
    <div className="flex flex-col gap-6">
      <Link
        href="/home"
        className="inline-flex w-fit items-center gap-1.5 text-sm text-text-muted transition-colors hover:text-text"
      >
        <ArrowLeft className="h-4 w-4" />
        Volver
      </Link>

      <header className="flex items-end gap-6 rounded-lg bg-gradient-to-b from-accent-soft to-bg-card/40 p-6">
        <CoverArt
          src={album.pictureUrl}
          alt={album.name}
          size="xl"
          rounded="md"
          className="shadow-2xl"
        />
        <div className="flex flex-1 flex-col gap-3 pb-1">
          <span className="text-xs font-semibold uppercase tracking-wide text-text-muted">
            Álbum
          </span>
          <h1 className="text-5xl font-bold tracking-tight">{album.name}</h1>
          <p className="text-sm text-text-muted">
            {artistNames.length > 0 ? (
              <span className="font-medium text-text">
                {artistNames.join(", ")}
              </span>
            ) : null}
            {artistNames.length > 0 ? " · " : ""}
            {tracks.length}{" "}
            {pluralize(tracks.length, "canción", "canciones")}
            {totalMs > 0 ? ` · ${formatDuration(totalMs)}` : ""}
          </p>
        </div>
      </header>

      <div className="flex items-center gap-4">
        <button
          type="button"
          onClick={playAll}
          disabled={tracks.length === 0}
          aria-label="Reproducir álbum"
          className={cn(
            "flex h-14 w-14 items-center justify-center rounded-full bg-accent text-text shadow-xl transition-all",
            "hover:scale-105 hover:bg-accent-hover active:scale-100",
            "disabled:cursor-not-allowed disabled:opacity-50 disabled:hover:scale-100",
          )}
        >
          <Play className="h-6 w-6" fill="currentColor" />
        </button>
        <h2 className="text-xl font-bold tracking-tight">Canciones</h2>
      </div>

      {songsQuery.isLoading ? (
        <div className="flex items-center justify-center py-12">
          <Spinner size="lg" />
        </div>
      ) : songsQuery.isError ? (
        <p className="text-sm text-danger">
          Error al cargar las canciones:{" "}
          {songsQuery.error instanceof Error
            ? songsQuery.error.message
            : "?"}
        </p>
      ) : tracks.length > 0 ? (
        <div className="flex flex-col">
          {tracks.map((song, i) => (
            <SongRow
              key={song.id}
              index={i}
              song={song}
              onPlay={() => playSong(song, tracks)}
              className={
                current?.id === song.id ? "bg-bg-hover/60" : undefined
              }
            />
          ))}
        </div>
      ) : (
        <EmptyState
          icon={<Music />}
          title="Este álbum aún no tiene canciones"
          description="Cuando se escaneen archivos asociados a este álbum aparecerán aquí."
        />
      )}

      {tracks.length === 0 ? (
        <p className="flex items-center gap-2 text-xs text-text-subtle">
          <Disc3 className="h-3.5 w-3.5" />
          Álbum vacío
        </p>
      ) : null}
    </div>
  );
}
