"use client";

import { Sidebar } from "./Sidebar";
import { Topbar } from "./Topbar";
import { PlayerBar } from "./PlayerBar";
import { ProtectedRoute } from "./ProtectedRoute";

export function AppShell({
  children,
  title,
}: {
  children: React.ReactNode;
  title?: string;
}) {
  return (
    <ProtectedRoute>
      <div className="flex h-screen flex-col">
        <div className="flex flex-1 overflow-hidden">
          <Sidebar />
          <div className="flex flex-1 flex-col overflow-hidden">
            <Topbar title={title} />
            <main className="flex-1 overflow-y-auto px-6 py-6">{children}</main>
          </div>
        </div>
        <PlayerBar />
      </div>
    </ProtectedRoute>
  );
}
