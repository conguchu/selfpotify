"use client";

import { useCallback, useRef, useState } from "react";
import { useRouter } from "next/navigation";
import { Sparkles, Users } from "lucide-react";
import { Coverflow } from "@/components/ui/Coverflow";
import { ArtistSlide } from "@/components/home/ArtistSlide";
import { SongSlide } from "@/components/home/SongSlide";
import { GenreCoverflowSection } from "@/components/home/GenreCoverflowSection";
import { Spinner } from "@/components/ui/Spinner";
import { EmptyState } from "@/components/ui/EmptyState";
import {
  useHomeFeed,
  useRecentGenres,
  useDailyDiscoveries,
} from "@/lib/query/hooks";
import { getRandomSongs } from "@/lib/api/songs";
import { usePlayerStore } from "@/lib/player/store";
import { useAuthStore } from "@/lib/auth/store";
import { useScrollRestoration } from "@/lib/use-scroll-restoration";
import type { SongDTO } from "@/lib/types";

export default function HomePage() {
  const router = useRouter();
  // Conserva la posición de scroll al navegar a un artista/género y volver.
  const scrollRef = useScrollRestoration<HTMLDivElement>("home");
  const username = useAuthStore((s) => s.username);
  const playSong = usePlayerStore((s) => s.playSong);
  // Descubrimientos diarios: 9 canciones estables durante el día.
  const dailyQuery = useDailyDiscoveries();
  // Cada visita a /home dispara GET /api/feed, que regenera el feed
  // del usuario en el backend antes de devolver los artistas recomendados.
  const feedQuery = useHomeFeed();
  // GET /api/feed/genres: los 10 géneros escuchados más recientemente.
  const genresQuery = useRecentGenres();
  // El backend puede repetir un género si se escuchó varias veces;
  // mostramos cada uno una sola vez conservando el orden de recencia.
  const recentGenres = genresQuery.data ? [...new Set(genresQuery.data)] : [];
  const artists = feedQuery.data ?? [];
  const daily = dailyQuery.data ?? [];

  // Canciones extra cargadas por scroll infinito en descubrimientos diarios.
  const [extraSongs, setExtraSongs] = useState<SongDTO[]>([]);
  const [loadingMore, setLoadingMore] = useState(false);
  const loadingMoreRef = useRef(false);

  const allDailySongs = [...daily, ...extraSongs];
  // Cuando está cargando más canciones se añade un elemento sentinel al final
  // del carrusel que se renderiza como card de carga (solo spinner).
  const LOADER_ID = -1;
  const carouselItems = loadingMore
    ? [...allDailySongs, { id: LOADER_ID } as SongDTO]
    : allDailySongs;

  const handleDailyIndexChange = useCallback(
    async (index: number) => {
      if (allDailySongs.length === 0) return;
      if (index < allDailySongs.length - 2) return;
      if (loadingMoreRef.current) return;
      loadingMoreRef.current = true;
      setLoadingMore(true);
      try {
        const [more] = await Promise.all([
          getRandomSongs(10),
          new Promise((r) => setTimeout(r, 700)),
        ]);
        setExtraSongs((prev) => [...prev, ...(more as typeof prev)]);
      } catch {
        // silencioso: simplemente no se añaden más
      } finally {
        loadingMoreRef.current = false;
        setLoadingMore(false);
      }
    },
    // eslint-disable-next-line react-hooks/exhaustive-deps
    [allDailySongs.length],
  );

  // Contenedor a pantalla completa con scroll-snap vertical. Los márgenes
  // negativos cancelan el `px-6 py-6` del <main> de AppShell (full-bleed) y el
  // `+3rem` recupera la altura que restaría ese `py-6` (1.5rem × 2). Depende de
  // que el padding de <main> siga siendo `6`.
  return (
    <div
      ref={scrollRef}
      className="home-snap -mx-6 -my-6 h-[calc(100%+3rem)] overflow-y-auto"
    >
      {/* Descubrimientos diarios + saludo */}
      <section
        data-scroll-key="daily"
        className="flex h-full w-full snap-start snap-always flex-col gap-6 px-6 py-6"
      >
        <header>
          <h1 className="text-3xl font-bold tracking-tight">
            {username ? `Hola, ${username}` : "Hola"}
          </h1>
        </header>

        <div className="flex items-center gap-2">
          <Sparkles className="h-5 w-5 text-accent-text" />
          <h2 className="text-xl font-bold tracking-tight">
            Tus descubrimientos diarios
          </h2>
        </div>

        <div className="flex flex-1 items-center justify-center">
          {dailyQuery.isLoading ? (
            <Spinner size="lg" />
          ) : dailyQuery.isError ? (
            <p className="text-sm text-danger">
              Error al cargar tus descubrimientos:{" "}
              {dailyQuery.error instanceof Error
                ? dailyQuery.error.message
                : "?"}
            </p>
          ) : allDailySongs.length > 0 ? (
            <Coverflow
              items={carouselItems}
              getKey={(s) => s.id}
              loop={false}
              ariaLabel="Descubrimientos diarios"
              onIndexChange={handleDailyIndexChange}
              renderItem={(song, { isCenter }) =>
                song.id === LOADER_ID ? (
                  <div className="flex flex-col items-center gap-4">
                    <div className="flex aspect-square w-full items-center justify-center">
                      <Spinner size="lg" />
                    </div>
                  </div>
                ) : (
                  <SongSlide
                    song={song}
                    isCenter={isCenter}
                    onPlay={() => playSong(song, allDailySongs)}
                  />
                )
              }
            />
          ) : (
            <EmptyState
              icon={<Sparkles />}
              title="Aún no hay descubrimientos"
              description="Cuando el servidor tenga música, aquí aparecerán 9 canciones nuevas cada día."
            />
          )}
        </div>
      </section>

      {/* Artistas recomendados */}
      <section
        data-scroll-key="artists"
        className="flex h-full w-full snap-start snap-always flex-col gap-6 px-6 py-6"
      >
        <div className="flex items-center gap-2">
          <Users className="h-5 w-5 text-accent-text" />
          <h2 className="text-xl font-bold tracking-tight">
            Artistas recomendados para ti
          </h2>
        </div>

        <div className="flex flex-1 items-center justify-center">
          {feedQuery.isLoading ? (
            <Spinner size="lg" />
          ) : feedQuery.isError ? (
            <p className="text-sm text-danger">
              Error al cargar tu feed:{" "}
              {feedQuery.error instanceof Error ? feedQuery.error.message : "?"}
            </p>
          ) : artists.length > 0 ? (
            <Coverflow
              items={artists}
              getKey={(a) => a.id}
              ariaLabel="Artistas recomendados"
              onActivateCenter={(artist) => router.push(`/artist/${artist.id}`)}
              renderItem={(artist, { isCenter }) => (
                <ArtistSlide artist={artist} isCenter={isCenter} />
              )}
            />
          ) : (
            <EmptyState
              icon={<Users />}
              title="Aún no hay artistas que recomendar"
              description="Cuando el servidor tenga música con escuchas, aquí aparecerán los artistas más populares."
            />
          )}
        </div>
      </section>

      {/* Una sección por género reciente */}
      {genresQuery.isLoading ? (
        <section className="flex h-full w-full snap-start snap-always items-center justify-center">
          <Spinner size="lg" />
        </section>
      ) : recentGenres.length === 0 && !genresQuery.isError ? (
        <section className="flex h-full w-full snap-start snap-always items-center justify-center px-6">
          <EmptyState
            icon={<Sparkles />}
            title="Aún no has escuchado nada"
            description="Cuando escuches canciones, aquí aparecerán secciones con los géneros que más reproduces."
          />
        </section>
      ) : (
        recentGenres.map((genre) => (
          <GenreCoverflowSection key={genre} genre={genre} />
        ))
      )}
    </div>
  );
}
