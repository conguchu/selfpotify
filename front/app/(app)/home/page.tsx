"use client";

import { Sparkles, Users } from "lucide-react";
import { ArtistCard } from "@/components/music/ArtistCard";
import { Spinner } from "@/components/ui/Spinner";
import { EmptyState } from "@/components/ui/EmptyState";
import { useHomeFeed } from "@/lib/query/hooks";
import { useAuthStore } from "@/lib/auth/store";

export default function HomePage() {
  const username = useAuthStore((s) => s.username);
  // Cada visita a /home dispara GET /api/feed, que regenera el feed
  // del usuario en el backend antes de devolver los artistas recomendados.
  const feedQuery = useHomeFeed();

  return (
    <div className="flex flex-col gap-8">
      <header>
        <h1 className="text-3xl font-bold tracking-tight">
          {username ? `Hola, ${username}` : "Hola"}
        </h1>
        <p className="text-sm text-text-muted">
          Tu feed se renueva en cada visita con los artistas del momento.
        </p>
      </header>

      <section className="flex flex-col gap-4">
        <div className="flex items-center gap-2">
          <Sparkles className="h-5 w-5 text-accent" />
          <h2 className="text-xl font-bold tracking-tight">
            Artistas recomendados para ti
          </h2>
        </div>

        {feedQuery.isLoading ? (
          <div className="flex items-center justify-center py-20">
            <Spinner size="lg" />
          </div>
        ) : feedQuery.isError ? (
          <p className="text-sm text-danger">
            Error al cargar tu feed:{" "}
            {feedQuery.error instanceof Error ? feedQuery.error.message : "?"}
          </p>
        ) : feedQuery.data && feedQuery.data.length > 0 ? (
          <div className="grid grid-cols-2 gap-3 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-5 xl:grid-cols-6">
            {feedQuery.data.map((artist) => (
              <ArtistCard key={artist.id} artist={artist} />
            ))}
          </div>
        ) : (
          <EmptyState
            icon={<Users />}
            title="Aún no hay artistas que recomendar"
            description="Cuando el servidor tenga música con escuchas, aquí aparecerán los artistas más populares."
          />
        )}
      </section>
    </div>
  );
}
