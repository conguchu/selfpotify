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
 * dropdown con dos acciones: <em>Ver tu perfil</em> —la propia tarjeta de
 * sesión hace de botón, así no se repite el icono dos veces seguidas— y
 * <em>Cerrar sesión</em>. La foto y el nombre visible se leen de
 * {@code /api/me} para que el avatar refleje la última subida; el username y
 * los roles vienen del auth store para no parpadear si la red tarda.
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
        {/* La tarjeta de sesión es a la vez la entrada "Ver tu perfil":
            tener la fila clicable evita duplicar @username + icono + texto
            en dos items consecutivos, que era ruidoso. El hover y el cursor
            dejan claro que es un botón. */}
        <DropdownItem
          onClick={() => router.push("/profile")}
          className="items-center gap-3 py-2"
        >
          {isAdmin ? (
            <Shield className="h-4 w-4 shrink-0 text-accent" />
          ) : (
            <User className="h-4 w-4 shrink-0 text-text-muted" />
          )}
          <div className="min-w-0 flex-1">
            <p className="truncate text-sm font-medium text-text">
              {displayName}
            </p>
            <p className="truncate text-xs text-text-muted">
              {isAdmin ? "Administrador" : "Usuario"}
              {meQuery.data?.displayName ? ` · @${username}` : ""}
            </p>
          </div>
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
