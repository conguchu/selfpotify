"use client";

import Link from "next/link";
import { Shield } from "lucide-react";
import { toast } from "sonner";
import { Avatar } from "@/components/ui/Avatar";
import { Button } from "@/components/ui/Button";
import { useFollowUser, useUnfollowUser } from "@/lib/query/hooks";
import type { UserSummaryDTO } from "@/lib/types";
import { cn } from "@/lib/utils";

/**
 * Cuadrícula de usuarios para las páginas de seguidores y siguiendo. Cada
 * celda es a la vez un enlace al perfil del usuario y, si así se pide,
 * incluye un botón "Siguiendo / Seguir" para cambiar mi relación con ese
 * usuario sin tener que entrar a su perfil.
 *
 * <p><b>{@code showFollowButtons}.</b> Solo se activa cuando el viewer ve sus
 * propias listas (lo decide la página padre comparando {@code me.id} con el
 * {@code [id]} de la URL). Lo pedimos como prop explícita porque las páginas
 * ya saben de quién es la lista — así el componente queda agnóstico y se
 * puede reutilizar más adelante (p. ej. en un modal de "elegir a quién
 * mencionar") sin que el botón se cuele.
 */
export function FollowList({
  users,
  showFollowButtons,
}: {
  users: UserSummaryDTO[];
  showFollowButtons: boolean;
}) {
  if (users.length === 0) return null;
  return (
    <div className="grid grid-cols-2 gap-4 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-5">
      {users.map((u) => (
        <FollowRow key={u.id} user={u} showButton={showFollowButtons} />
      ))}
    </div>
  );
}

function FollowRow({
  user,
  showButton,
}: {
  user: UserSummaryDTO;
  showButton: boolean;
}) {
  const followMutation = useFollowUser();
  const unfollowMutation = useUnfollowUser();
  const busy = followMutation.isPending || unfollowMutation.isPending;
  const iFollow = user.isFollowedByMe === true;
  const visibleName = user.displayName?.trim() || user.username;
  const isAdmin = user.type === "ADMIN";

  const onToggle = (e: React.MouseEvent) => {
    // El botón vive dentro de un <Link>; sin esto el click navegaría al
    // perfil además de disparar el follow.
    e.preventDefault();
    e.stopPropagation();
    const mutation = iFollow ? unfollowMutation : followMutation;
    mutation.mutate(user.id, {
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
    <Link
      href={`/user/${user.id}`}
      className={cn(
        "group flex flex-col items-center gap-3 rounded-lg bg-bg-card/40 p-4 transition-colors hover:bg-bg-hover",
      )}
    >
      <Avatar
        src={user.avatarUrl}
        alt={visibleName}
        size="lg"
        className="h-28 w-28 shadow-xl"
      />
      <div className="w-full min-w-0 text-center">
        <p
          className="truncate text-sm font-semibold text-text"
          title={visibleName}
        >
          {visibleName}
        </p>
        <p
          className="truncate text-xs text-text-muted"
          title={`@${user.username}`}
        >
          @{user.username}
        </p>
        {isAdmin ? (
          <p className="mt-1 inline-flex items-center gap-1 text-[10px] uppercase tracking-wide text-accent-hover">
            <Shield className="h-3 w-3" aria-hidden />
            Admin
          </p>
        ) : null}
      </div>
      {showButton ? (
        <Button
          size="sm"
          variant={iFollow ? "secondary" : "primary"}
          onClick={onToggle}
          loading={busy}
          className="w-full"
        >
          {iFollow ? "Siguiendo" : "Seguir"}
        </Button>
      ) : null}
    </Link>
  );
}
