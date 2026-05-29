"use client";

import Link from "next/link";
import { ListMusic, Pencil, Shield, User as UserIcon } from "lucide-react";
import { Avatar } from "@/components/ui/Avatar";
import { Badge } from "@/components/ui/Badge";
import { EmptyState } from "@/components/ui/EmptyState";
import { Spinner } from "@/components/ui/Spinner";
import { PlaylistCard } from "@/components/music/PlaylistCard";
import { usePublicProfile, useUserPublicPlaylists } from "@/lib/query/hooks";
import { useAuthStore } from "@/lib/auth/store";

/**
 * Vista pública del perfil de un usuario. Es la misma para "mi perfil"
 * (renderizada bajo {@code /profile}) y para el perfil de otro usuario
 * (renderizada bajo {@code /user/[id]}). El dueño del perfil ve un icono de
 * lápiz que enlaza a {@code /profile/edit}, donde se editan nombre y foto.
 *
 * <p>La detección del dueño se hace comparando el username del perfil con el
 * del auth store. Es estable (el username es único e inmutable) y evita tener
 * que pasar el id del usuario en sesión: el {@code SecurityContext} del
 * backend ya garantiza que {@code /api/me} y este componente coincidan.
 */
export function UserProfileView({ userId }: { userId: number }) {
  const profileQuery = usePublicProfile(userId);
  const playlistsQuery = useUserPublicPlaylists(userId);
  const myUsername = useAuthStore((s) => s.username);

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
  const isOwner = myUsername === profile.username;
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
        <div className="flex min-w-0 flex-1 flex-col gap-2 text-center sm:text-left">
          <p className="text-xs uppercase tracking-wide text-text-muted">
            Perfil
          </p>
          <div className="flex flex-wrap items-center justify-center gap-3 sm:justify-start">
            <h1 className="break-words text-4xl font-bold tracking-tight">
              {visibleName}
            </h1>
            {isOwner ? (
              // El lápiz solo se pinta para el dueño del perfil — los demás
              // usuarios no lo ven ni pueden alcanzarlo (la página de edición
              // es /profile/edit, que opera siempre sobre /api/me).
              <Link
                href="/profile/edit"
                aria-label="Editar mi perfil"
                title="Editar perfil"
                className="inline-flex h-9 w-9 items-center justify-center rounded-full border border-border bg-bg-card text-text-muted transition-colors hover:border-accent hover:bg-bg-hover hover:text-text focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-accent focus-visible:ring-offset-2 focus-visible:ring-offset-bg"
              >
                <Pencil className="h-4 w-4" />
              </Link>
            ) : null}
          </div>
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
            description={
              isOwner
                ? "Aún no has compartido ninguna playlist. Crea una y márcala como pública para que aparezca aquí."
                : `${visibleName} todavía no comparte ninguna playlist.`
            }
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
