"use client";

import Link from "next/link";
import { ListMusic, Pencil, Shield, User as UserIcon } from "lucide-react";
import { toast } from "sonner";
import { Avatar } from "@/components/ui/Avatar";
import { Badge } from "@/components/ui/Badge";
import { Button } from "@/components/ui/Button";
import { EmptyState } from "@/components/ui/EmptyState";
import { Spinner } from "@/components/ui/Spinner";
import { PlaylistCard } from "@/components/music/PlaylistCard";
import {
  useFollowUser,
  useMyPlaylists,
  usePublicProfile,
  useSharedPlaylists,
  useUnfollowUser,
  useUserPublicPlaylists,
} from "@/lib/query/hooks";
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
 *
 * <p>Bajo el avatar se muestran dos contadores estilo Spotify (seguidores y
 * siguiendo); ambos enlazan a las páginas de lista {@code /user/[id]/followers}
 * y {@code /user/[id]/following}, que están montadas también para "mi propio
 * id" (no hay rutas {@code /profile/followers}: simplemente navegamos siempre
 * por {@code /user/[id]/...} con el id que sea).
 */
export function UserProfileView({ userId }: { userId: number }) {
  const profileQuery = usePublicProfile(userId);
  const myUsername = useAuthStore((s) => s.username);
  // Saber si soy yo se hace por username (estable y único). Lo derivamos en
  // cuanto llega el perfil; antes de eso `isOwner` queda en false y ambas
  // queries de playlists se quedan deshabilitadas vía su flag `enabled`.
  const isOwner = !!myUsername && myUsername === profileQuery.data?.username;
  // Para "yo mismo" hago `GET /api/playlists/my` (devuelve públicas y
  // privadas); para otro usuario, `GET /api/playlists/user/{id}` (solo
  // públicas, ya filtradas por el backend). Solo una de las dos se dispara,
  // de modo que un visitante nunca pega al endpoint que listaría las mías.
  const ownPlaylistsQuery = useMyPlaylists(isOwner);
  // En mi propio perfil mezclo mis playlists con las compartidas conmigo (en
  // las que soy colaborador): ambas aparecen en "Tus playlists", y las
  // compartidas se distinguen por el icono de personas de PlaylistCard.
  const sharedPlaylistsQuery = useSharedPlaylists(isOwner);
  const publicPlaylistsQuery = useUserPublicPlaylists(userId, !isOwner);

  const followMutation = useFollowUser();
  const unfollowMutation = useUnfollowUser();

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
  const playlistsLoading = isOwner
    ? ownPlaylistsQuery.isLoading || sharedPlaylistsQuery.isLoading
    : publicPlaylistsQuery.isLoading;
  const playlists = isOwner
    ? [...(ownPlaylistsQuery.data ?? []), ...(sharedPlaylistsQuery.data ?? [])]
    : (publicPlaylistsQuery.data ?? []);
  const followBusy = followMutation.isPending || unfollowMutation.isPending;
  // `isFollowedByMe` viaja del backend ya resuelto contra el SecurityContext;
  // basta con leerlo (null para un usuario sin sesión o para uno mismo).
  const iFollow = profile.isFollowedByMe === true;

  const onToggleFollow = () => {
    const mutation = iFollow ? unfollowMutation : followMutation;
    mutation.mutate(profile.id, {
      onError: (err) =>
        toast.error(
          err instanceof Error
            ? err.message
            : iFollow
              ? "No se pudo dejar de seguir"
              : "No se pudo seguir",
        ),
    });
  };

  return (
    <div className="flex flex-col gap-10">
      <header className="flex flex-col items-center gap-4 sm:flex-row sm:items-end sm:gap-6">
        <Avatar
          src={profile.avatarUrl}
          alt={visibleName}
          size="lg"
          className="h-40 w-40 shadow-2xl"
        />
        <div className="flex min-w-0 flex-1 flex-col gap-3 text-center sm:text-left">
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
          {/*
            Contadores estilo Spotify: dos enlaces clicables que llevan a la
            lista correspondiente. Mostramos siempre los dos, incluso si están
            a 0, para que la UI no salte cuando llegues a 1.
          */}
          <div className="flex flex-wrap items-center justify-center gap-4 text-sm sm:justify-start">
            <Link
              href={`/user/${profile.id}/followers`}
              className="text-text-muted transition-colors hover:text-text"
            >
              <span className="font-semibold text-text">
                {profile.followersCount}
              </span>{" "}
              {profile.followersCount === 1 ? "seguidor" : "seguidores"}
            </Link>
            <span aria-hidden className="text-text-subtle">
              ·
            </span>
            <Link
              href={`/user/${profile.id}/following`}
              className="text-text-muted transition-colors hover:text-text"
            >
              <span className="font-semibold text-text">
                {profile.followingCount}
              </span>{" "}
              siguiendo
            </Link>
            {/*
              Botón de follow: visible solo al mirar el perfil de otro
              usuario. `isFollowedByMe === true` ⇒ "Siguiendo" (variante
              `secondary` para que se note que ya estás dentro); cualquier
              otro caso ⇒ "Seguir" (acento). Mientras la mutación está en
              vuelo se deshabilita pero mantiene el texto del estado nuevo.
            */}
            {!isOwner ? (
              <Button
                size="sm"
                variant={iFollow ? "secondary" : "primary"}
                onClick={onToggleFollow}
                loading={followBusy}
                className="ml-1"
              >
                {iFollow ? "Siguiendo" : "Seguir"}
              </Button>
            ) : null}
          </div>
        </div>
      </header>

      <section className="flex flex-col gap-4">
        <h2 className="text-xl font-bold tracking-tight">
          {isOwner ? "Tus playlists" : "Playlists públicas"}
        </h2>
        {playlistsLoading ? (
          <div className="flex h-32 items-center justify-center">
            <Spinner size="md" />
          </div>
        ) : playlists.length === 0 ? (
          <EmptyState
            icon={<ListMusic />}
            title={isOwner ? "Sin playlists" : "Sin playlists públicas"}
            description={
              isOwner
                ? "Aún no has creado ninguna playlist."
                : `${visibleName} todavía no comparte ninguna playlist.`
            }
          />
        ) : (
          // PlaylistCard ya pinta el candado en la esquina superior derecha
          // cuando isPublic=false, así que las privadas (solo visibles aquí
          // si soy el dueño) quedan identificadas de un vistazo.
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
