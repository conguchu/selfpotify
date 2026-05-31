"use client";

import { Card, CardDescription, CardTitle } from "@/components/ui/Card";
import { UploadSongsForm } from "@/components/admin/UploadSongsForm";

export default function AdminMusicPage() {
  return (
    <div className="flex flex-col gap-6">
      <div>
        <h1 className="text-3xl font-bold tracking-tight">Música</h1>
        <p className="text-sm text-text-muted">
          Sube audios por arrastrar y soltar, revisa sus datos y añádelos a la
          biblioteca.
        </p>
      </div>

      <Card>
        <CardTitle className="mb-1">Subir audios (drag &amp; drop)</CardTitle>
        <CardDescription className="mb-4">
          Arrastra archivos <code>.mp3</code>/<code>.wav</code>. Antes de
          incorporarlos podrás ajustar título, artista, carátula, género, BPM y
          duración de cada canción. Se guardan en una carpeta{" "}
          <code>selfpotify_added</code> del servidor.
        </CardDescription>
        <UploadSongsForm />
      </Card>
    </div>
  );
}
