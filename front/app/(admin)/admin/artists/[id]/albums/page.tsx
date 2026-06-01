"use client";

import { useMemo, useState } from "react";
import { useParams } from "next/navigation";
import Link from "next/link";
import { ArrowLeft, Disc, Pencil } from "lucide-react";
import { Avatar } from "@/components/ui/Avatar";
import { Card, CardDescription, CardTitle } from "@/components/ui/Card";
import { IconButton } from "@/components/ui/IconButton";
import { Spinner } from "@/components/ui/Spinner";
import { EmptyState } from "@/components/ui/EmptyState";
import { Table, TBody, TD, TH, THead, TR } from "@/components/ui/Table";
import { AlbumEditModal } from "@/components/admin/AlbumEditModal";
import { useAlbums, useArtist } from "@/lib/query/hooks";
import type { AlbumDTO } from "@/lib/types";

export default function AdminArtistAlbumsPage() {
  const params = useParams<{ id: string }>();
  const id = Number(params?.id);
  const artistQuery = useArtist(id);
  const albumsQuery = useAlbums();
  const [editing, setEditing] = useState<AlbumDTO | null>(null);

  // Los álbumes del artista se filtran por los albumIds que trae su DTO: el DTO
  // de álbum no expone el artistId, así que la pertenencia se resuelve aquí.
  const albums = useMemo(() => {
    const ids = new Set(artistQuery.data?.albumIds ?? []);
    return (albumsQuery.data ?? [])
      .filter((a) => ids.has(a.id))
      .sort((a, b) => a.name.localeCompare(b.name, "es", { sensitivity: "base" }));
  }, [albumsQuery.data, artistQuery.data]);

  const loading = artistQuery.isLoading || albumsQuery.isLoading;

  return (
    <div className="flex flex-col gap-6">
      <div>
        <Link
          href={`/admin/artists/${id}`}
          className="mb-2 inline-flex items-center gap-1.5 text-sm text-text-muted hover:text-text"
        >
          <ArrowLeft className="h-4 w-4" />
          Volver al artista
        </Link>
        <h1 className="text-3xl font-bold tracking-tight">
          Álbumes{artistQuery.data ? ` de ${artistQuery.data.name}` : ""}
        </h1>
      </div>

      <Card>
        <CardTitle className="mb-1">Álbumes del artista</CardTitle>
        <CardDescription className="mb-4">
          Edita el nombre y la portada de cada álbum.
        </CardDescription>

        {loading ? (
          <div className="flex items-center justify-center py-10">
            <Spinner />
          </div>
        ) : albums.length === 0 ? (
          <EmptyState
            icon={<Disc />}
            title="Sin álbumes"
            description="Este artista no tiene álbumes en el catálogo."
          />
        ) : (
          <Table>
            <THead>
              <TR>
                <TH>Álbum</TH>
                <TH className="text-right">Canciones</TH>
                <TH className="w-20 text-right">Acciones</TH>
              </TR>
            </THead>
            <TBody>
              {albums.map((a) => (
                <TR key={a.id}>
                  <TD>
                    <div className="flex items-center gap-3">
                      <Avatar src={a.pictureUrl} alt={a.name} size="sm" />
                      <span className="font-medium">{a.name}</span>
                    </div>
                  </TD>
                  <TD className="text-right tabular-nums text-text-muted">
                    {a.songIds?.length ?? 0}
                  </TD>
                  <TD className="text-right">
                    <IconButton
                      label="Editar"
                      variant="ghost"
                      size="sm"
                      onClick={() => setEditing(a)}
                    >
                      <Pencil />
                    </IconButton>
                  </TD>
                </TR>
              ))}
            </TBody>
          </Table>
        )}
      </Card>

      <AlbumEditModal
        album={editing}
        open={!!editing}
        onClose={() => setEditing(null)}
      />
    </div>
  );
}
