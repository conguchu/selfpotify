"use client";

import { useParams } from "next/navigation";
import Link from "next/link";
import { ArrowLeft } from "lucide-react";
import { Card, CardDescription, CardTitle } from "@/components/ui/Card";
import { Spinner } from "@/components/ui/Spinner";
import { EmptyState } from "@/components/ui/EmptyState";
import { SongEditForm } from "@/components/admin/SongEditForm";
import { useSong } from "@/lib/query/hooks";

export default function AdminSongEditPage() {
  const params = useParams<{ id: string }>();
  const id = Number(params?.id);
  const songQuery = useSong(id);

  return (
    <div className="flex flex-col gap-6">
      <div>
        <Link
          href="/admin/songs"
          className="mb-2 inline-flex items-center gap-1.5 text-sm text-text-muted hover:text-text"
        >
          <ArrowLeft className="h-4 w-4" />
          Volver a canciones
        </Link>
        <h1 className="text-3xl font-bold tracking-tight">Editar canción</h1>
      </div>

      <Card>
        {songQuery.isLoading ? (
          <div className="flex items-center justify-center py-10">
            <Spinner />
          </div>
        ) : songQuery.isError || !songQuery.data ? (
          <EmptyState
            title="Canción no encontrada"
            description="La canción que intentas editar no existe o fue eliminada."
          />
        ) : (
          <>
            <CardTitle className="mb-1">{songQuery.data.title}</CardTitle>
            <CardDescription className="mb-4">
              ID #{songQuery.data.id}
              {songQuery.data.artistNames.length
                ? ` · ${songQuery.data.artistNames.join(", ")}`
                : ""}
            </CardDescription>
            <SongEditForm song={songQuery.data} />
          </>
        )}
      </Card>
    </div>
  );
}
