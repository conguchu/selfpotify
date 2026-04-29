"use client";

import { useState } from "react";
import { Card, CardDescription, CardTitle } from "@/components/ui/Card";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/Tabs";
import { ImportFolderForm } from "@/components/admin/ImportFolderForm";
import { CreateSongForm } from "@/components/admin/CreateSongForm";

export default function AdminMusicPage() {
  const [tab, setTab] = useState<"import" | "manual">("import");

  return (
    <div className="flex flex-col gap-6">
      <div>
        <h1 className="text-3xl font-bold tracking-tight">Música</h1>
        <p className="text-sm text-text-muted">
          Importa una carpeta entera o añade canciones manualmente.
        </p>
      </div>

      <Tabs value={tab} onValueChange={(v) => setTab(v as "import" | "manual")}>
        <TabsList>
          <TabsTrigger value="import">Importar carpeta</TabsTrigger>
          <TabsTrigger value="manual">Añadir canción manual</TabsTrigger>
        </TabsList>
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
