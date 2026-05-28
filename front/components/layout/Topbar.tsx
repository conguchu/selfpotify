"use client";

import Link from "next/link";
import { Shield } from "lucide-react";
import { ProfileMenu } from "./ProfileMenu";
import { SearchBar } from "./SearchBar";
import { useAppName } from "@/lib/query/hooks";
import { useAuthStore } from "@/lib/auth/store";

export function Topbar({ title }: { title?: string }) {
  const appName = useAppName();
  const isAdmin = useAuthStore((s) => s.roles.includes("ROLE_ADMIN"));
  return (
    <header className="sticky top-0 z-20 flex h-16 items-center gap-4 border-b border-border bg-bg-elevated/80 px-6 backdrop-blur">
      {title ? (
        <div className="min-w-0 shrink-0">
          <h1 className="truncate text-lg font-semibold tracking-tight text-text">
            {title}
          </h1>
        </div>
      ) : null}
      {/* Centra la barra de búsqueda y la deja flexible para ocupar el hueco. */}
      <div className="flex flex-1 justify-center">
        <SearchBar />
      </div>
      <div className="flex shrink-0 items-center gap-3">
        {isAdmin ? (
          <Link
            href="/admin"
            className="inline-flex items-center gap-1.5 text-xs font-medium text-text-muted transition-colors hover:text-text"
          >
            <Shield className="h-4 w-4" />
            Panel admin
          </Link>
        ) : null}
        <ProfileMenu />
      </div>
    </header>
  );
}
