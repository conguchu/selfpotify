"use client";

import Link from "next/link";
import { ListMusic, Lock, Users } from "lucide-react";
import { cn } from "@/lib/utils";
import { resolveImageUrl } from "@/lib/image";
import type { PlaylistDTO } from "@/lib/types";

export function PlaylistItem({
  playlist,
  active,
  className,
}: {
  playlist: PlaylistDTO;
  active?: boolean;
  className?: string;
}) {
  const songCount = playlist.songIds?.length ?? 0;
  const isShared = (playlist.collaboratorIds?.length ?? 0) > 0;
  return (
    <Link
      href={`/playlist/${playlist.id}`}
      className={cn(
        "flex items-center gap-3 rounded-md px-2 py-2 transition-colors",
        active
          ? "bg-bg-hover text-text"
          : "text-text-muted hover:bg-bg-hover hover:text-text",
        className,
      )}
    >
      {playlist.pictureUrl && (
        <img
          src={resolveImageUrl(playlist.pictureUrl)!}
          alt={playlist.name}
          className="h-10 w-10 shrink-0 rounded object-cover"
        />
      )}
      {!playlist.pictureUrl && (
        <div className="flex h-10 w-10 shrink-0 items-center justify-center rounded bg-bg-elevated text-accent-hover">
          <ListMusic className="h-5 w-5" />
        </div>
      )}
      <div className="min-w-0 flex-1">
        <p className="truncate text-sm font-medium" title={playlist.name}>
          {playlist.name}
        </p>
        <p className="truncate text-xs text-text-subtle">
          {songCount} canci{songCount === 1 ? "ón" : "ones"}
        </p>
      </div>
      {isShared ? (
        <Users
          className="h-3.5 w-3.5 shrink-0 text-text-subtle"
          aria-label="Compartida"
        />
      ) : null}
      {!playlist.isPublic ? (
        <Lock className="h-3.5 w-3.5 shrink-0 text-text-subtle" />
      ) : null}
    </Link>
  );
}
