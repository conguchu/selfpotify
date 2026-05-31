"use client";

import { use } from "react";
import Link from "next/link";
import { ArrowLeft, Disc3, Music, Play } from "lucide-react";
import { Spinner } from "@/components/ui/Spinner";
import { EmptyState } from "@/components/ui/EmptyState";
import { SongRow } from "@/components/music/SongRow";
import { useGenreTopSongs } from "@/lib/query/hooks";
import { usePlayerStore } from "@/lib/player/store";
import { cn, pluralize } from "@/lib/utils";

export default function GenrePage({
  params,
}: {
  params: Promise<{ genre: string }>;
}) {
  // El segmento dinámico llega ya decodificado por el router.
  const { genre } = use(params);
  const genreName = decodeURIComponent(genre);

  const songsQuery = useGenreTopSongs(genreName);
  const playSong = usePlayerStore((s) => s.playSong);
  const current = usePlayerStore((s) => s.current);

  // `GET /api/songs/{genre}/top` ya devuelve SongDTO, sin conversión.
  const tracks = songsQuery.data?.top ?? [];

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

      {/* Cabecera estilo Spotify */}
      <header className="flex items-end gap-6 rounded-lg bg-gradient-to-b from-accent-soft to-bg-card/40 p-6">
        <div className="flex h-44 w-44 items-center justify-center rounded-lg bg-gradient-to-br from-accent to-accent-soft shadow-2xl">
          <Disc3 className="h-24 w-24 text-text/80" />
        </div>
        <div className="flex flex-1 flex-col gap-3 pb-1">
          <span className="text-xs font-semibold uppercase tracking-wide text-text-muted">
            Género
          </span>
          <h1 className="text-5xl font-bold capitalize tracking-tight">
            {genreName}
          </h1>
          <p className="text-sm text-text-muted">
            {tracks.length}{" "}
            {pluralize(tracks.length, "canción", "canciones")} más escuchadas
          </p>
        </div>
      </header>

      {/* Acciones */}
      <div className="flex items-center gap-4">
        <button
          type="button"
          onClick={playAll}
          disabled={tracks.length === 0}
          aria-label="Reproducir top canciones del género"
          className={cn(
            "flex h-14 w-14 items-center justify-center rounded-full bg-accent text-text shadow-xl transition-all",
            "hover:scale-105 hover:bg-accent-hover active:scale-100",
            "disabled:cursor-not-allowed disabled:opacity-50 disabled:hover:scale-100",
          )}
        >
          <Play className="h-6 w-6" fill="currentColor" />
        </button>
        <h2 className="text-xl font-bold tracking-tight">Populares</h2>
      </div>

      {/* Top 10 canciones del género */}
      {songsQuery.isLoading ? (
        <div className="flex items-center justify-center py-12">
          <Spinner size="lg" />
        </div>
      ) : songsQuery.isError ? (
        <EmptyState
          icon={<Music />}
          title="No hay canciones de este género"
          description="Puede que aún no se haya importado música de este género."
          action={
            <Link
              href="/home"
              className="text-sm text-accent-text hover:underline"
            >
              Volver al inicio
            </Link>
          }
        />
      ) : tracks.length > 0 ? (
        <div className="flex flex-col">
          {tracks.map((song, i) => (
            <SongRow
              key={song.id}
              index={i}
              song={song}
              onPlay={() => playSong(song, tracks)}
              className={current?.id === song.id ? "bg-bg-hover/60" : undefined}
            />
          ))}
        </div>
      ) : (
        <EmptyState
          icon={<Music />}
          title="Este género aún no tiene canciones"
          description="Cuando se importe música de este género, sus temas más escuchados aparecerán aquí."
        />
      )}
    </div>
  );
}
