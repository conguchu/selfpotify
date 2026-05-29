"use client";

import { useRouter } from "next/navigation";
import { LogOut, Shield, User } from "lucide-react";
import { Avatar } from "@/components/ui/Avatar";
import {
  DropdownContent,
  DropdownItem,
  DropdownLabel,
  DropdownMenu,
  DropdownSeparator,
  DropdownTrigger,
} from "@/components/ui/DropdownMenu";
import { useAuthStore } from "@/lib/auth/store";
import { useMe } from "@/lib/query/hooks";

/**
 * Botón circular del topbar con la foto/inicial del usuario en sesión. Abre un
 * dropdown con el resumen del perfil, un enlace para editarlo y el botón de
 * cerrar sesión. Lee el perfil real (foto + nombre visible) desde el endpoint
 * {@code /api/me} para que el avatar refleje la foto subida al instante; la
 * identidad de la sesión (username + roles) se sigue tomando del auth store
 * para no parpadear si la red tarda.
 */
export function ProfileMenu() {
  const router = useRouter();
  const username = useAuthStore((s) => s.username);
  const isAdmin = useAuthStore((s) => s.roles.includes("ROLE_ADMIN"));
  const logout = useAuthStore((s) => s.logout);
  // useMe solo se dispara con sesión activa (apiFetch añade Authorization),
  // pero igualmente lo deshabilitamos sin username para no hacer ruido.
  const meQuery = useMe(!!username);
  const displayName = meQuery.data?.displayName?.trim() || username || "";
  const avatarUrl = meQuery.data?.avatarUrl ?? null;

  if (!username) return null;

  return (
    <DropdownMenu>
      <DropdownTrigger className="rounded-full focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-accent focus-visible:ring-offset-2 focus-visible:ring-offset-bg">
        <Avatar src={avatarUrl} alt={displayName} size="md" />
      </DropdownTrigger>
      <DropdownContent>
        <DropdownLabel>Sesión</DropdownLabel>
        <div className="flex items-center gap-2 px-3 pb-2 pt-1">
          {isAdmin ? (
            <Shield className="h-4 w-4 text-accent" />
          ) : (
            <User className="h-4 w-4 text-text-muted" />
          )}
          <div className="min-w-0">
            <p className="truncate text-sm font-medium text-text">
              {displayName}
            </p>
            <p className="text-xs text-text-muted">
              {isAdmin ? "Administrador" : "Usuario"}
              {meQuery.data?.displayName ? ` · @${username}` : ""}
            </p>
          </div>
        </div>
        <DropdownSeparator />
        <DropdownItem onClick={() => router.push("/profile")}>
          <User className="h-4 w-4" />
          Ver tu perfil
        </DropdownItem>
        <DropdownSeparator />
        <DropdownItem variant="danger" onClick={logout}>
          <LogOut className="h-4 w-4" />
          Cerrar sesión
        </DropdownItem>
      </DropdownContent>
    </DropdownMenu>
  );
}
