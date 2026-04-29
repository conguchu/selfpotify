"use client";

import { Music } from "lucide-react";
import { SongGrid } from "@/components/music/SongGrid";
import { Spinner } from "@/components/ui/Spinner";
import { EmptyState } from "@/components/ui/EmptyState";
import { useSongs } from "@/lib/query/hooks";
import { useAuthStore } from "@/lib/auth/store";

export default function HomePage() {
  const username = useAuthStore((s) => s.username);
  const songsQuery = useSongs();

  return (
    <div className="flex flex-col gap-6">
      <header>
        <h1 className="text-3xl font-bold tracking-tight">
          {username ? `Hola, ${username}` : "Hola"}
        </h1>
        <p className="text-sm text-text-muted">
          Todas las canciones disponibles en tu servidor.
        </p>
      </header>

      {songsQuery.isLoading ? (
        <div className="flex items-center justify-center py-20">
          <Spinner size="lg" />
        </div>
      ) : songsQuery.isError ? (
        <p className="text-sm text-danger">
          Error al cargar canciones:{" "}
          {songsQuery.error instanceof Error ? songsQuery.error.message : "?"}
        </p>
      ) : songsQuery.data && songsQuery.data.length > 0 ? (
        <SongGrid songs={songsQuery.data} />
      ) : (
        <EmptyState
          icon={<Music />}
          title="Aún no hay música"
          description="Pídele al administrador que importe canciones desde el panel de admin."
        />
      )}
    </div>
  );
}
