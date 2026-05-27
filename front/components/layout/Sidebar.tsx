"use client";

import { useState } from "react";
import Link from "next/link";
import { usePathname } from "next/navigation";
import { Home, Plus } from "lucide-react";
import { AppLogo } from "./AppLogo";
import { IconButton } from "@/components/ui/IconButton";
import { Spinner } from "@/components/ui/Spinner";
import { EmptyState } from "@/components/ui/EmptyState";
import { CreatePlaylistModal } from "@/components/music/CreatePlaylistModal";
import { PlaylistItem } from "@/components/music/PlaylistItem";
import { useMyPlaylists, useAppName } from "@/lib/query/hooks";
import { cn } from "@/lib/utils";

export function Sidebar() {
  const pathname = usePathname();
  const [createOpen, setCreateOpen] = useState(false);
  const playlistsQuery = useMyPlaylists();
  const appName = useAppName();

  return (
    <aside className="flex h-full w-72 shrink-0 flex-col gap-3 border-r border-border bg-bg-elevated p-4">
      <Link href="/home" className="flex items-center gap-2 px-2 py-1">
        <AppLogo className="h-10 w-10" />
        <span className="text-lg font-bold tracking-tight">{appName}</span>
      </Link>

      <nav className="flex flex-col gap-1">
        <NavLink href="/home" icon={<Home className="h-5 w-5" />} active={pathname === "/home"}>
          Inicio
        </NavLink>
      </nav>

      <div className="mt-2 flex items-center justify-between px-2">
        <span className="text-xs font-semibold uppercase tracking-wide text-text-subtle">
          Tus playlists
        </span>
        <IconButton
          label="Nueva playlist"
          variant="ghost"
          size="sm"
          onClick={() => setCreateOpen(true)}
        >
          <Plus />
        </IconButton>
      </div>

      <div className="-mx-1 flex flex-1 flex-col gap-0.5 overflow-y-auto px-1 pb-2">
        {playlistsQuery.isLoading ? (
          <div className="flex items-center justify-center py-6">
            <Spinner />
          </div>
        ) : playlistsQuery.isError ? (
          <p className="px-2 text-xs text-danger">Error al cargar playlists</p>
        ) : playlistsQuery.data && playlistsQuery.data.length > 0 ? (
          playlistsQuery.data.map((p) => (
            <PlaylistItem
              key={p.id}
              playlist={p}
              active={pathname === `/playlist/${p.id}`}
            />
          ))
        ) : (
          <EmptyState
            title="Sin playlists"
            description="Crea tu primera playlist con el botón de arriba."
            className="border-none bg-transparent px-2 py-4"
          />
        )}
      </div>

      <CreatePlaylistModal open={createOpen} onClose={() => setCreateOpen(false)} />
    </aside>
  );
}

function NavLink({
  href,
  icon,
  active,
  children,
}: {
  href: string;
  icon: React.ReactNode;
  active?: boolean;
  children: React.ReactNode;
}) {
  return (
    <Link
      href={href}
      className={cn(
        "flex items-center gap-3 rounded-md px-3 py-2 text-sm font-medium transition-colors",
        active
          ? "bg-bg-hover text-text"
          : "text-text-muted hover:bg-bg-hover hover:text-text",
      )}
    >
      {icon}
      {children}
    </Link>
  );
}
