"use client";

import { useState } from "react";
import { Card, CardDescription, CardTitle } from "@/components/ui/Card";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/Tabs";
import { ThemeSettings } from "@/components/admin/ThemeSettings";
import { LibrarySettings } from "@/components/admin/LibrarySettings";

type SettingsTab = "appearance" | "library";

export default function AdminSettingsPage() {
  const [tab, setTab] = useState<SettingsTab>("appearance");

  return (
    <div className="flex flex-col gap-6">
      <div>
        <h1 className="text-3xl font-bold tracking-tight">Ajustes</h1>
        <p className="text-sm text-text-muted">
          Personaliza la apariencia y gestiona el origen de música del servidor.
        </p>
      </div>

      <Tabs value={tab} onValueChange={(v) => setTab(v as SettingsTab)}>
        <TabsList>
          <TabsTrigger value="appearance">Apariencia</TabsTrigger>
          <TabsTrigger value="library">Biblioteca</TabsTrigger>
        </TabsList>
        <TabsContent value="appearance">
          <Card>
            <CardTitle className="mb-1">Apariencia</CardTitle>
            <CardDescription className="mb-4">
              Nombre, logo y colores del tema. Los cambios se previsualizan en
              vivo y se aplican a toda la app al guardar.
            </CardDescription>
            <ThemeSettings />
          </Card>
        </TabsContent>
        <TabsContent value="library">
          <Card>
            <CardTitle className="mb-1">Biblioteca de música</CardTitle>
            <CardDescription className="mb-4">
              Gestiona las rutas que escanea selfpotify y el autocompletado de
              metadatos y carátulas.
            </CardDescription>
            <LibrarySettings />
          </Card>
        </TabsContent>
      </Tabs>
    </div>
  );
}
