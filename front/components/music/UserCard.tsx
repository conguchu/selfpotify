"use client";

import Link from "next/link";
import { Shield, User as UserIcon } from "lucide-react";
import { Avatar } from "@/components/ui/Avatar";
import type { UserSummaryDTO } from "@/lib/types";
import { cn } from "@/lib/utils";

/**
 * Tarjeta de usuario para los resultados de búsqueda. Avatar circular grande,
 * nombre visible (displayName del perfil con fallback al username) y badge de
 * rol si es admin. Es un enlace a la página pública del usuario
 * ({@code /user/[id]}), donde se pueden ver sus playlists públicas.
 */
export function UserCard({
  user,
  className,
}: {
  user: UserSummaryDTO;
  className?: string;
}) {
  const visibleName = user.displayName?.trim() || user.username;
  const isAdmin = user.type === "ADMIN";
  return (
    <Link
      href={`/user/${user.id}`}
      className={cn(
        "group relative flex flex-col items-center gap-3 rounded-lg bg-bg-card/40 p-4 transition-colors hover:bg-bg-hover",
        className,
      )}
    >
      <Avatar
        src={user.avatarUrl}
        alt={visibleName}
        size="lg"
        className="h-36 w-36 shadow-xl"
      />
      <div className="w-full min-w-0 text-center">
        <p className="truncate text-sm font-semibold" title={visibleName}>
          {visibleName}
        </p>
        <p className="flex items-center justify-center gap-1 text-xs text-text-muted">
          {isAdmin ? (
            <Shield className="h-3 w-3 text-accent" aria-hidden />
          ) : (
            <UserIcon className="h-3 w-3" aria-hidden />
          )}
          {isAdmin ? "Administrador" : "Usuario"}
          {user.displayName ? (
            <span className="text-text-subtle">· @{user.username}</span>
          ) : null}
        </p>
      </div>
    </Link>
  );
}
