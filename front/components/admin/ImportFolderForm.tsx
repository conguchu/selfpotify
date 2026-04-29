"use client";

import { useState } from "react";
import { FolderInput, Music } from "lucide-react";
import { toast } from "sonner";
import { Button } from "@/components/ui/Button";
import { Input } from "@/components/ui/Input";
import { Label } from "@/components/ui/Label";
import { EmptyState } from "@/components/ui/EmptyState";
import { Table, TBody, TD, TH, THead, TR } from "@/components/ui/Table";
import { useImportFolder } from "@/lib/query/hooks";
import { formatDuration } from "@/lib/utils";
import type { SongDTO } from "@/lib/types";

export function ImportFolderForm() {
  const [path, setPath] = useState("");
  const [imported, setImported] = useState<SongDTO[] | null>(null);
  const importFolder = useImportFolder();

  const submit = async (e: React.FormEvent) => {
    e.preventDefault();
    const trimmed = path.trim();
    if (!trimmed) {
      toast.error("Indica una ruta absoluta");
      return;
    }
    try {
      const result = await importFolder.mutateAsync({ path: trimmed });
      setImported(result);
      toast.success(
        `Importadas ${result.length} canci${result.length === 1 ? "ón" : "ones"}`,
      );
    } catch (err) {
      toast.error(err instanceof Error ? err.message : "Error al importar");
    }
  };

  return (
    <div className="flex flex-col gap-4">
      <form onSubmit={submit} className="flex flex-col gap-3">
        <div className="flex flex-col gap-1.5">
          <Label htmlFor="import-path">Ruta absoluta de la carpeta</Label>
          <Input
            id="import-path"
            value={path}
            onChange={(e) => setPath(e.target.value)}
            placeholder="/Users/yo/Música/Mi librería"
          />
          <p className="text-xs text-text-subtle">
            El servidor la recorrerá recursivamente buscando archivos{" "}
            <code>.mp3</code> y <code>.wav</code>.
          </p>
        </div>
        <Button
          type="submit"
          loading={importFolder.isPending}
          leftIcon={<FolderInput className="h-4 w-4" />}
          className="self-start"
        >
          Escanear e importar
        </Button>
      </form>

      {imported === null ? null : imported.length === 0 ? (
        <EmptyState
          icon={<Music />}
          title="Sin nuevas canciones"
          description="No se encontraron archivos compatibles en esa ruta."
        />
      ) : (
        <Table>
          <THead>
            <TR>
              <TH>ID</TH>
              <TH>Título</TH>
              <TH>Género</TH>
              <TH className="text-right">Duración</TH>
            </TR>
          </THead>
          <TBody>
            {imported.map((s) => (
              <TR key={s.id}>
                <TD className="tabular-nums text-text-muted">{s.id}</TD>
                <TD className="font-medium">{s.title}</TD>
                <TD className="text-text-muted">{s.genre || "—"}</TD>
                <TD className="text-right tabular-nums text-text-muted">
                  {formatDuration(s.duration_ms)}
                </TD>
              </TR>
            ))}
          </TBody>
        </Table>
      )}
    </div>
  );
}
