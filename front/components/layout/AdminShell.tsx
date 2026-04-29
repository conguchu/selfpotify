"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import { Disc3, Music2, Shield, Users } from "lucide-react";
import { ProfileMenu } from "./ProfileMenu";
import { ProtectedRoute } from "./ProtectedRoute";
import { cn } from "@/lib/utils";

const NAV = [
  { href: "/admin", label: "Resumen", icon: Shield, exact: true },
  { href: "/admin/users", label: "Usuarios", icon: Users },
  { href: "/admin/music", label: "Música", icon: Music2 },
];

export function AdminShell({
  children,
  title,
}: {
  children: React.ReactNode;
  title?: string;
}) {
  const pathname = usePathname();
  return (
    <ProtectedRoute requireAdmin>
      <div className="flex h-screen flex-col">
        <header className="flex h-16 items-center justify-between border-b border-border bg-bg-elevated px-6">
          <div className="flex items-center gap-6">
            <Link href="/admin" className="flex items-center gap-2">
              <Disc3 className="h-6 w-6 text-accent" />
              <span className="text-base font-bold tracking-tight">
                selfpotify <span className="text-accent">admin</span>
              </span>
            </Link>
            <nav className="flex items-center gap-1">
              {NAV.map((item) => {
                const active = item.exact
                  ? pathname === item.href
                  : pathname?.startsWith(item.href);
                const Icon = item.icon;
                return (
                  <Link
                    key={item.href}
                    href={item.href}
                    className={cn(
                      "inline-flex items-center gap-2 rounded-md px-3 py-1.5 text-sm font-medium transition-colors",
                      active
                        ? "bg-bg-hover text-text"
                        : "text-text-muted hover:bg-bg-hover hover:text-text",
                    )}
                  >
                    <Icon className="h-4 w-4" />
                    {item.label}
                  </Link>
                );
              })}
            </nav>
          </div>
          <div className="flex items-center gap-3">
            <Link
              href="/home"
              className="text-xs font-medium text-text-muted hover:text-text"
            >
              Ir a la app
            </Link>
            <ProfileMenu />
          </div>
        </header>
        <main className="flex-1 overflow-y-auto px-6 py-6">
          {title ? (
            <h1 className="mb-6 text-2xl font-bold tracking-tight">{title}</h1>
          ) : null}
          {children}
        </main>
      </div>
    </ProtectedRoute>
  );
}
