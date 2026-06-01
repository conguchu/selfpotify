"use client";

import { useParams } from "next/navigation";
import Link from "next/link";
import { ArrowLeft, Disc3 } from "lucide-react";
import { Card, CardDescription, CardTitle } from "@/components/ui/Card";
import { Spinner } from "@/components/ui/Spinner";
import { EmptyState } from "@/components/ui/EmptyState";
import { ArtistEditForm } from "@/components/admin/ArtistEditForm";
import { useArtist } from "@/lib/query/hooks";

export default function AdminArtistEditPage() {
  const params = useParams<{ id: string }>();
  const id = Number(params?.id);
  const artistQuery = useArtist(id);

  return (
    <div className="flex flex-col gap-6">
      <div>
        <Link
          href="/admin/artists"
          className="mb-2 inline-flex items-center gap-1.5 text-sm text-text-muted hover:text-text"
        >
          <ArrowLeft className="h-4 w-4" />
          Volver a artistas
        </Link>
        <h1 className="text-3xl font-bold tracking-tight">Editar artista</h1>
      </div>

      <Card>
        {artistQuery.isLoading ? (
          <div className="flex items-center justify-center py-10">
            <Spinner />
          </div>
        ) : artistQuery.isError || !artistQuery.data ? (
          <EmptyState
            title="Artista no encontrado"
            description="El artista que intentas editar no existe o fue eliminado."
          />
        ) : (
          <>
            <CardTitle className="mb-1">{artistQuery.data.name}</CardTitle>
            <CardDescription className="mb-4">
              ID #{artistQuery.data.id} ·{" "}
              {artistQuery.data.songIds?.length ?? 0} canción(es) ·{" "}
              {artistQuery.data.albumIds?.length ?? 0} álbum(es)
            </CardDescription>
            <Link
              href={`/admin/artists/${artistQuery.data.id}/albums`}
              className="mb-5 inline-flex h-9 items-center gap-2 self-start rounded-md border border-border bg-bg-card px-3 text-sm font-medium text-text hover:bg-bg-hover"
            >
              <Disc3 className="h-4 w-4" />
              Ver álbumes ({artistQuery.data.albumIds?.length ?? 0})
            </Link>
            <ArtistEditForm artist={artistQuery.data} />
          </>
        )}
      </Card>
    </div>
  );
}
