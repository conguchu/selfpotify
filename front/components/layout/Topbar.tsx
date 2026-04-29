"use client";

import { ProfileMenu } from "./ProfileMenu";

export function Topbar({ title }: { title?: string }) {
  return (
    <header className="sticky top-0 z-20 flex h-16 items-center justify-between border-b border-border bg-bg-elevated/80 px-6 backdrop-blur">
      <div className="min-w-0">
        {title ? (
          <h1 className="truncate text-lg font-semibold tracking-tight text-text">
            {title}
          </h1>
        ) : (
          <span className="text-sm text-text-muted">Selfpotify</span>
        )}
      </div>
      <div className="flex items-center gap-3">
        <ProfileMenu />
      </div>
    </header>
  );
}
