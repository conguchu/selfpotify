"use client";

import { Disc3 } from "lucide-react";
import { Coverflow } from "@/components/ui/Coverflow";
import { SongSlide } from "@/components/home/SongSlide";
import { Spinner } from "@/components/ui/Spinner";
import { EmptyState } from "@/components/ui/EmptyState";
import { useGenreTopSongs } from "@/lib/query/hooks";
import { usePlayerStore } from "@/lib/player/store";

/**
 * Sección a pantalla completa con un coverflow de las canciones más escuchadas
 * de un género. Cada género es su propio componente para poder usar su propio
 * hook `useGenreTopSongs` (no se pueden llamar hooks en un bucle).
 *
 * Al pulsar el botón de play de una carátula se reproduce esa canción con el
 * resto del género como cola.
 */
export function GenreCoverflowSection({ genre }: { genre: string }) {
  const { data, isLoading, isError, error } = useGenreTopSongs(genre);
  const playSong = usePlayerStore((s) => s.playSong);
  const songs = data?.top ?? [];

  return (
    <section
      data-scroll-key={`genre:${genre}`}
      className="flex h-full w-full snap-start snap-always flex-col gap-6 px-6 py-6"
    >
      <div className="flex items-center gap-2">
        <Disc3 className="h-5 w-5 text-accent" />
        <h2 className="text-xl font-bold capitalize tracking-tight">{genre}</h2>
      </div>

      <div className="flex flex-1 items-center justify-center">
        {isLoading ? (
          <Spinner size="lg" />
        ) : isError ? (
          <p className="text-sm text-danger">
            Error al cargar el género:{" "}
            {error instanceof Error ? error.message : "?"}
          </p>
        ) : songs.length > 0 ? (
          <Coverflow
            items={songs}
            getKey={(s) => s.id}
            ariaLabel={`Canciones del género ${genre}`}
            renderItem={(song, { isCenter }) => (
              <SongSlide
                song={song}
                isCenter={isCenter}
                onPlay={() => playSong(song, songs)}
              />
            )}
          />
        ) : (
          <EmptyState
            icon={<Disc3 />}
            title="Este género aún no tiene canciones"
            description="Cuando se importe música de este género, sus temas más escuchados aparecerán aquí."
          />
        )}
      </div>
    </section>
  );
}
