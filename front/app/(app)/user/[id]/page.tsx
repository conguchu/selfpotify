"use client";

import { use } from "react";
import { ListMusic, Shield, User as UserIcon } from "lucide-react";
import { Avatar } from "@/components/ui/Avatar";
import { Badge } from "@/components/ui/Badge";
import { EmptyState } from "@/components/ui/EmptyState";
import { Spinner } from "@/components/ui/Spinner";
import { PlaylistCard } from "@/components/music/PlaylistCard";
import { usePublicProfile, useUserPublicPlaylists } from "@/lib/query/hooks";

/**
 * Página pública del perfil de otro usuario (`/user/[id]`). Renderiza la vista
 * mínima ({@link import('@/lib/types').UserSummaryDTO}) que devuelve
 * {@code GET /api/users/{id}/public} más las playlists públicas del usuario,
 * que ya tienen su endpoint propio ({@code GET /api/playlists/user/{userId}}).
 *
 * <p>La página es accesible para cualquier usuario autenticado: se enlaza
 * desde los resultados de búsqueda (dropdown global y página
 * {@code /search}). No expone nada que no sea ya consultable: el username y la
 * vista mínima del perfil ya viajan en {@code /api/search}, y las playlists
 * privadas siguen escondidas (el backend solo devuelve las {@code isPublic}).
 */
export default function PublicUserPage({
  params,
}: {
  params: Promise<{ id: string }>;
}) {
  const { id } = use(params);
  const userId = Number.parseInt(id, 10);
  const profileQuery = usePublicProfile(userId);
  const playlistsQuery = useUserPublicPlaylists(userId);

  if (profileQuery.isLoading) {
    return (
      <div className="flex h-64 items-center justify-center">
        <Spinner size="lg" />
      </div>
    );
  }

  if (profileQuery.isError || !profileQuery.data) {
    return (
      <EmptyState
        icon={<UserIcon />}
        title="Usuario no encontrado"
        description={
          profileQuery.error instanceof Error
            ? profileQuery.error.message
            : "El usuario no existe o ya no está disponible."
        }
      />
    );
  }

  const profile = profileQuery.data;
  const visibleName = profile.displayName?.trim() || profile.username;
  const isAdmin = profile.type === "ADMIN";
  const playlists = playlistsQuery.data ?? [];

  return (
    <div className="flex flex-col gap-10">
      <header className="flex flex-col items-center gap-4 sm:flex-row sm:items-end sm:gap-6">
        <Avatar
          src={profile.avatarUrl}
          alt={visibleName}
          size="lg"
          className="h-40 w-40 shadow-2xl"
        />
        <div className="flex min-w-0 flex-col gap-2 text-center sm:text-left">
          <p className="text-xs uppercase tracking-wide text-text-muted">
            Perfil
          </p>
          <h1 className="break-words text-4xl font-bold tracking-tight">
            {visibleName}
          </h1>
          <div className="flex flex-wrap items-center justify-center gap-2 text-sm text-text-muted sm:justify-start">
            <span className="font-mono">@{profile.username}</span>
            {isAdmin ? (
              <Badge variant="accent" className="inline-flex items-center gap-1">
                <Shield className="h-3 w-3" aria-hidden />
                Administrador
              </Badge>
            ) : null}
          </div>
        </div>
      </header>

      <section className="flex flex-col gap-4">
        <h2 className="text-xl font-bold tracking-tight">
          Playlists públicas
        </h2>
        {playlistsQuery.isLoading ? (
          <div className="flex h-32 items-center justify-center">
            <Spinner size="md" />
          </div>
        ) : playlists.length === 0 ? (
          <EmptyState
            icon={<ListMusic />}
            title="Sin playlists públicas"
            description={`${visibleName} todavía no comparte ninguna playlist.`}
          />
        ) : (
          <div className="grid grid-cols-2 gap-4 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-5">
            {playlists.map((p) => (
              <PlaylistCard key={p.id} playlist={p} />
            ))}
          </div>
        )}
      </section>
    </div>
  );
}
