"use client";

import { useState } from "react";
import { Card, CardDescription, CardTitle } from "@/components/ui/Card";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/Tabs";
import { ImportFolderForm } from "@/components/admin/ImportFolderForm";
import { CreateSongForm } from "@/components/admin/CreateSongForm";
import { UploadSongsForm } from "@/components/admin/UploadSongsForm";

type MusicTab = "upload" | "import" | "manual";

export default function AdminMusicPage() {
  const [tab, setTab] = useState<MusicTab>("upload");

  return (
    <div className="flex flex-col gap-6">
      <div>
        <h1 className="text-3xl font-bold tracking-tight">Música</h1>
        <p className="text-sm text-text-muted">
          Sube audios, importa una carpeta entera o añade canciones manualmente.
        </p>
      </div>

      <Tabs value={tab} onValueChange={(v) => setTab(v as MusicTab)}>
        <TabsList>
          <TabsTrigger value="upload">Subir audios</TabsTrigger>
          <TabsTrigger value="import">Importar carpeta</TabsTrigger>
          <TabsTrigger value="manual">Añadir canción manual</TabsTrigger>
        </TabsList>
        <TabsContent value="upload">
          <Card>
            <CardTitle className="mb-1">Subir audios (drag &amp; drop)</CardTitle>
            <CardDescription className="mb-4">
              Arrastra archivos <code>.mp3</code>/<code>.wav</code>. Se guardan en
              una carpeta <code>selfpotify_added</code> del servidor y se añaden a
              la biblioteca con sus metadatos.
            </CardDescription>
            <UploadSongsForm />
          </Card>
        </TabsContent>
        <TabsContent value="import">
          <Card>
            <CardTitle className="mb-1">Importar desde el disco</CardTitle>
            <CardDescription className="mb-4">
              El servidor escaneará la carpeta indicada y persistirá las canciones
              encontradas.
            </CardDescription>
            <ImportFolderForm />
          </Card>
        </TabsContent>
        <TabsContent value="manual">
          <Card>
            <CardTitle className="mb-1">Crear una canción</CardTitle>
            <CardDescription className="mb-4">
              Útil para añadir canciones sueltas con metadatos personalizados.
              La ruta debe ser absoluta y accesible para el servidor.
            </CardDescription>
            <CreateSongForm />
          </Card>
        </TabsContent>
      </Tabs>
    </div>
  );
}
