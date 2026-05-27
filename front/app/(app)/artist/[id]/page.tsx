"use client";

import { use } from "react";
import Link from "next/link";
import { ArrowLeft, Music, Play } from "lucide-react";
import { Avatar } from "@/components/ui/Avatar";
import { Spinner } from "@/components/ui/Spinner";
import { EmptyState } from "@/components/ui/EmptyState";
import { SongRow } from "@/components/music/SongRow";
import { useArtist, useArtistTopTracks } from "@/lib/query/hooks";
import { usePlayerStore } from "@/lib/player/store";
import { cn, pluralize } from "@/lib/utils";

export default function ArtistPage({
  params,
}: {
  params: Promise<{ id: string }>;
}) {
  const { id } = use(params);
  const artistId = Number.parseInt(id, 10);

  const artistQuery = useArtist(artistId);
  const tracksQuery = useArtistTopTracks(artistId);
  const playSong = usePlayerStore((s) => s.playSong);
  const current = usePlayerStore((s) => s.current);

  // Las top tracks ya llegan aplanadas a `SongDTO` (con escuchas derivadas).
  const tracks = tracksQuery.data?.tracks ?? [];

  if (artistQuery.isLoading) {
    return (
      <div className="flex items-center justify-center py-20">
        <Spinner size="lg" />
      </div>
    );
  }

  if (artistQuery.isError || !artistQuery.data) {
    return (
      <EmptyState
        title="Artista no encontrado"
        description="Puede que no exista o que el servidor no lo tenga registrado."
        action={
          <Link href="/home" className="text-sm text-accent-hover hover:underline">
            Volver al inicio
          </Link>
        }
      />
    );
  }

  const artist = artistQuery.data;
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
        <Avatar
          src={artist.photoUrl}
          alt={artist.name}
          size="lg"
          className="h-44 w-44 shadow-2xl"
        />
        <div className="flex flex-1 flex-col gap-3 pb-1">
          <span className="text-xs font-semibold uppercase tracking-wide text-text-muted">
            Artista
          </span>
          <h1 className="text-5xl font-bold tracking-tight">{artist.name}</h1>
          <p className="text-sm text-text-muted">
            {artist.songIds.length}{" "}
            {pluralize(artist.songIds.length, "canción", "canciones")} ·{" "}
            {artist.albumIds.length}{" "}
            {pluralize(artist.albumIds.length, "álbum", "álbumes")}
          </p>
        </div>
      </header>

      {/* Acciones */}
      <div className="flex items-center gap-4">
        <button
          type="button"
          onClick={playAll}
          disabled={tracks.length === 0}
          aria-label="Reproducir top canciones"
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

      {/* Top 10 canciones */}
      {tracksQuery.isLoading ? (
        <div className="flex items-center justify-center py-12">
          <Spinner size="lg" />
        </div>
      ) : tracksQuery.isError ? (
        <p className="text-sm text-danger">
          Error al cargar las canciones:{" "}
          {tracksQuery.error instanceof Error ? tracksQuery.error.message : "?"}
        </p>
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
          title="Este artista aún no tiene canciones"
          description="Cuando se importe música suya, sus temas más escuchados aparecerán aquí."
        />
      )}
    </div>
  );
}
