"use client";

import { Card, CardDescription, CardTitle } from "@/components/ui/Card";
import { ArtistsManager } from "@/components/admin/ArtistsManager";

export default function AdminArtistsPage() {
  return (
    <div className="flex flex-col gap-6">
      <div>
        <h1 className="text-3xl font-bold tracking-tight">Artistas</h1>
        <p className="text-sm text-text-muted">
          Edita nombres y fotos, separa artistas mal etiquetados y une duplicados.
        </p>
      </div>

      <Card>
        <CardTitle className="mb-1">Artistas del catálogo</CardTitle>
        <CardDescription className="mb-4">
          Usa «Separar» para repartir un artista conjunto (p. ej. «Ill Pekeño /
          Ergo Pro») en varios reales, y selecciona dos o más para «Juntar»
          duplicados (p. ej. «El alfa» y «El Alfa») en uno solo.
        </CardDescription>
        <ArtistsManager />
      </Card>
    </div>
  );
}
