"use client";

import { Card, CardDescription, CardTitle } from "@/components/ui/Card";
import { SongsManager } from "@/components/admin/SongsManager";

export default function AdminSongsPage() {
  return (
    <div className="flex flex-col gap-6">
      <div>
        <h1 className="text-3xl font-bold tracking-tight">Canciones</h1>
        <p className="text-sm text-text-muted">
          Gestiona el catálogo: edita los metadatos o elimina canciones.
        </p>
      </div>

      <Card>
        <CardTitle className="mb-1">Catálogo</CardTitle>
        <CardDescription className="mb-4">
          Usa el lápiz para editar los datos de una canción en su propia página.
        </CardDescription>
        <SongsManager />
      </Card>
    </div>
  );
}
