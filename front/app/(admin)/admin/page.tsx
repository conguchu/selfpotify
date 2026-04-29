"use client";

import Link from "next/link";
import { Disc3, ListMusic, Music2, Users } from "lucide-react";
import { Card, CardDescription, CardTitle } from "@/components/ui/Card";
import { Spinner } from "@/components/ui/Spinner";
import {
  useAlbums,
  useArtists,
  useMyPlaylists,
  useSongs,
  useUsers,
} from "@/lib/query/hooks";

export default function AdminDashboardPage() {
  const songs = useSongs();
  const artists = useArtists();
  const albums = useAlbums();
  const users = useUsers();
  const playlists = useMyPlaylists();

  const items = [
    {
      label: "Canciones",
      icon: Music2,
      query: songs,
      href: "/admin/music",
    },
    { label: "Artistas", icon: Disc3, query: artists, href: "/admin/music" },
    { label: "Álbumes", icon: Disc3, query: albums, href: "/admin/music" },
    { label: "Usuarios", icon: Users, query: users, href: "/admin/users" },
    { label: "Mis playlists", icon: ListMusic, query: playlists, href: "/home" },
  ];

  return (
    <div className="flex flex-col gap-6">
      <div>
        <h1 className="text-3xl font-bold tracking-tight">Panel de administración</h1>
        <p className="text-sm text-text-muted">
          Resumen del estado del servidor.
        </p>
      </div>
      <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-5">
        {items.map((item) => {
          const Icon = item.icon;
          const count = item.query.data?.length ?? 0;
          return (
            <Link key={item.label} href={item.href} className="block">
              <Card className="flex flex-col gap-3 transition-colors hover:border-accent">
                <div className="flex items-center gap-3">
                  <div className="flex h-10 w-10 items-center justify-center rounded-md bg-accent-soft text-accent">
                    <Icon className="h-5 w-5" />
                  </div>
                  <CardTitle className="text-sm font-medium uppercase tracking-wide text-text-muted">
                    {item.label}
                  </CardTitle>
                </div>
                {item.query.isLoading ? (
                  <Spinner />
                ) : item.query.isError ? (
                  <CardDescription className="text-danger">Error</CardDescription>
                ) : (
                  <p className="text-3xl font-bold tabular-nums">{count}</p>
                )}
              </Card>
            </Link>
          );
        })}
      </div>

      <Card>
        <CardTitle className="mb-2">Acciones rápidas</CardTitle>
        <CardDescription className="mb-4">
          Crea contenido o importa una carpeta del disco.
        </CardDescription>
        <div className="flex flex-wrap gap-3">
          <Link
            href="/admin/music"
            className="inline-flex h-10 items-center gap-2 rounded-md bg-accent px-4 text-sm font-medium text-text hover:bg-accent-hover"
          >
            <Music2 className="h-4 w-4" />
            Gestionar música
          </Link>
          <Link
            href="/admin/users"
            className="inline-flex h-10 items-center gap-2 rounded-md border border-border bg-bg-card px-4 text-sm font-medium text-text hover:bg-bg-hover"
          >
            <Users className="h-4 w-4" />
            Gestionar usuarios
          </Link>
        </div>
      </Card>
    </div>
  );
}
