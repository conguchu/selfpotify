"use client";

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

export function ProfileMenu() {
  const username = useAuthStore((s) => s.username);
  const isAdmin = useAuthStore((s) => s.roles.includes("ROLE_ADMIN"));
  const logout = useAuthStore((s) => s.logout);

  if (!username) return null;

  return (
    <DropdownMenu>
      <DropdownTrigger className="rounded-full focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-accent focus-visible:ring-offset-2 focus-visible:ring-offset-bg">
        <Avatar alt={username} size="md" />
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
            <p className="truncate text-sm font-medium text-text">{username}</p>
            <p className="text-xs text-text-muted">
              {isAdmin ? "Administrador" : "Usuario"}
            </p>
          </div>
        </div>
        <DropdownSeparator />
        <DropdownItem variant="danger" onClick={logout}>
          <LogOut className="h-4 w-4" />
          Cerrar sesión
        </DropdownItem>
      </DropdownContent>
    </DropdownMenu>
  );
}
