"use client";

import Link from "next/link";
import { ArrowLeft, Users } from "lucide-react";
import { EmptyState } from "@/components/ui/EmptyState";
import { Spinner } from "@/components/ui/Spinner";
import { FollowList } from "@/components/user/FollowList";
import {
  useFollowers,
  useFollowing,
  useMe,
  usePublicProfile,
} from "@/lib/query/hooks";

/**
 * Cuerpo común de las páginas {@code /user/[id]/followers} y
 * {@code /user/[id]/following}. La página delgada que vive en {@code app/}
 * solo pasa el {@code userId} y el modo; aquí se hace todo el render.
 *
 * <p>Detección de "lista mía": se compara el {@code [id]} de la URL con el
 * {@code id} de {@code GET /api/me}. Solo en ese caso se activan los botones
 * de "Siguiendo / Seguir" por fila (el requisito explícito del producto).
 */
export function FollowListPage({
  userId,
  mode,
}: {
  userId: number;
  mode: "followers" | "following";
}) {
  const profileQuery = usePublicProfile(userId);
  const meQuery = useMe();
  const followersQuery = useFollowers(userId, mode === "followers");
  const followingQuery = useFollowing(userId, mode === "following");

  const listQuery = mode === "followers" ? followersQuery : followingQuery;
  const isMyOwnList = !!meQuery.data && meQuery.data.id === userId;

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
        icon={<Users />}
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
  const users = listQuery.data ?? [];
  const title = mode === "followers" ? "Seguidores" : "Siguiendo";
  const subtitle =
    mode === "followers"
      ? isMyOwnList
        ? "Personas que te siguen"
        : `Personas que siguen a ${visibleName}`
      : isMyOwnList
        ? "Personas a las que sigues"
        : `Personas a las que sigue ${visibleName}`;

  return (
    <div className="flex flex-col gap-6">
      <header className="flex flex-col gap-2">
        <Link
          href={`/user/${userId}`}
          className="inline-flex w-fit items-center gap-1 text-xs font-medium text-text-muted transition-colors hover:text-text"
        >
          <ArrowLeft className="h-3.5 w-3.5" />
          Volver al perfil de {visibleName}
        </Link>
        <h1 className="text-3xl font-bold tracking-tight">{title}</h1>
        <p className="text-sm text-text-muted">{subtitle}</p>
      </header>

      {listQuery.isLoading ? (
        <div className="flex h-32 items-center justify-center">
          <Spinner size="md" />
        </div>
      ) : users.length === 0 ? (
        <EmptyState
          icon={<Users />}
          title={
            mode === "followers"
              ? isMyOwnList
                ? "Aún no tienes seguidores"
                : `${visibleName} todavía no tiene seguidores`
              : isMyOwnList
                ? "Aún no sigues a nadie"
                : `${visibleName} todavía no sigue a nadie`
          }
          description={
            mode === "followers"
              ? "Cuando alguien empiece a seguirte aparecerá aquí."
              : "Empieza a seguir a otros usuarios para verlos aquí."
          }
        />
      ) : (
        <FollowList users={users} showFollowButtons={isMyOwnList} />
      )}
    </div>
  );
}
