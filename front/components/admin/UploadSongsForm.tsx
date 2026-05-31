"use client";

import { useRef, useState } from "react";
import { UploadCloud, Music, Trash2, FileAudio } from "lucide-react";
import { toast } from "sonner";
import { Button } from "@/components/ui/Button";
import { Label } from "@/components/ui/Label";
import { EmptyState } from "@/components/ui/EmptyState";
import { Spinner } from "@/components/ui/Spinner";
import { Table, TBody, TD, TH, THead, TR } from "@/components/ui/Table";
import { useServerConfig, useUploadSongs } from "@/lib/query/hooks";
import { formatDuration } from "@/lib/utils";
import { cn } from "@/lib/utils";
import type { SongDTO } from "@/lib/types";

const ACCEPT = ".mp3,.wav,audio/mpeg,audio/wav,audio/x-wav";

function isAudio(file: File): boolean {
  const name = file.name.toLowerCase();
  return name.endsWith(".mp3") || name.endsWith(".wav");
}

export function UploadSongsForm() {
  const config = useServerConfig();
  const upload = useUploadSongs();
  const inputRef = useRef<HTMLInputElement | null>(null);

  const [files, setFiles] = useState<File[]>([]);
  const [dragging, setDragging] = useState(false);
  const [target, setTarget] = useState<string>(""); // "" = carpeta de datos por defecto
  const [uploaded, setUploaded] = useState<SongDTO[] | null>(null);

  const runningInDocker = config.data?.runningInDocker ?? false;
  const scanPaths = config.data?.scanPaths ?? [];
  const addedSongsDir = config.data?.addedSongsDir ?? "selfpotify_added";

  const addFiles = (incoming: FileList | File[]) => {
    const accepted: File[] = [];
    let rejected = 0;
    for (const f of Array.from(incoming)) {
      if (isAudio(f)) accepted.push(f);
      else rejected++;
    }
    if (rejected > 0) {
      toast.error(
        `${rejected} archivo(s) ignorado(s): solo se admiten .mp3 y .wav`,
      );
    }
    if (accepted.length === 0) return;
    // Deduplica por nombre+tamaño para no añadir el mismo audio dos veces.
    setFiles((prev) => {
      const seen = new Set(prev.map((f) => `${f.name}:${f.size}`));
      const merged = [...prev];
      for (const f of accepted) {
        const key = `${f.name}:${f.size}`;
        if (!seen.has(key)) {
          seen.add(key);
          merged.push(f);
        }
      }
      return merged;
    });
  };

  const onDrop = (e: React.DragEvent) => {
    e.preventDefault();
    setDragging(false);
    if (e.dataTransfer.files?.length) addFiles(e.dataTransfer.files);
  };

  const submit = async () => {
    if (files.length === 0) {
      toast.error("Añade al menos un archivo");
      return;
    }
    try {
      const result = await upload.mutateAsync({
        files,
        targetPath: !runningInDocker && target ? target : undefined,
      });
      setUploaded(result);
      setFiles([]);
      toast.success(
        `Subidas ${result.length} canci${result.length === 1 ? "ón" : "ones"}`,
      );
    } catch (err) {
      toast.error(err instanceof Error ? err.message : "Error al subir");
    }
  };

  return (
    <div className="flex flex-col gap-4">
      {/* Destino: en Docker es fijo; en local se elige entre las rutas configuradas. */}
      {runningInDocker ? (
        <p className="text-xs text-text-subtle">
          Los audios se guardarán en{" "}
          <code className="text-text-muted">{addedSongsDir}</code> dentro del
          volumen de datos y se añadirán a la biblioteca.
        </p>
      ) : (
        <div className="flex flex-col gap-1.5">
          <Label htmlFor="upload-target">Carpeta de destino</Label>
          <select
            id="upload-target"
            value={target}
            onChange={(e) => setTarget(e.target.value)}
            className={cn(
              "h-10 w-full rounded-md border border-border bg-bg-card px-3 text-sm text-text",
              "focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-accent focus-visible:border-accent",
            )}
          >
            <option value="">Carpeta de datos (por defecto)</option>
            {scanPaths.map((p) => (
              <option key={p} value={p}>
                {p}
              </option>
            ))}
          </select>
          <p className="text-xs text-text-subtle">
            Se creará una subcarpeta <code>selfpotify_added</code> dentro de la
            ruta elegida. La opción por defecto usa la carpeta de datos del
            servidor.
          </p>
        </div>
      )}

      {/* Dropzone */}
      <div
        role="button"
        tabIndex={0}
        onClick={() => inputRef.current?.click()}
        onKeyDown={(e) => {
          if (e.key === "Enter" || e.key === " ") {
            e.preventDefault();
            inputRef.current?.click();
          }
        }}
        onDragOver={(e) => {
          e.preventDefault();
          setDragging(true);
        }}
        onDragLeave={() => setDragging(false)}
        onDrop={onDrop}
        className={cn(
          "flex cursor-pointer flex-col items-center justify-center gap-2 rounded-lg border-2 border-dashed px-6 py-10 text-center transition-colors",
          "focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-accent",
          dragging
            ? "border-accent bg-accent-soft/40"
            : "border-border bg-bg-card/40 hover:border-text-subtle",
        )}
      >
        <UploadCloud className="h-9 w-9 text-text-subtle" />
        <p className="text-sm font-medium text-text">
          Arrastra audios aquí o haz clic para elegir
        </p>
        <p className="text-xs text-text-subtle">Formatos: .mp3 y .wav</p>
        <input
          ref={inputRef}
          type="file"
          accept={ACCEPT}
          multiple
          className="hidden"
          onChange={(e) => {
            if (e.target.files?.length) addFiles(e.target.files);
            e.target.value = "";
          }}
        />
      </div>

      {/* Cola de archivos pendientes */}
      {files.length > 0 && (
        <div className="flex flex-col gap-2">
          <ul className="flex flex-col gap-1.5">
            {files.map((f, i) => (
              <li
                key={`${f.name}:${f.size}`}
                className="flex items-center gap-2 rounded-md border border-border bg-bg px-3 py-2"
              >
                <FileAudio className="h-4 w-4 shrink-0 text-text-muted" />
                <span className="truncate text-sm text-text">{f.name}</span>
                <span className="ml-auto shrink-0 text-xs tabular-nums text-text-subtle">
                  {(f.size / 1_048_576).toFixed(1)} MB
                </span>
                <button
                  type="button"
                  onClick={() =>
                    setFiles((prev) => prev.filter((_, idx) => idx !== i))
                  }
                  className="shrink-0 text-text-subtle hover:text-danger"
                  aria-label={`Quitar ${f.name}`}
                >
                  <Trash2 className="h-4 w-4" />
                </button>
              </li>
            ))}
          </ul>
          <div className="flex items-center gap-3">
            <Button
              onClick={submit}
              loading={upload.isPending}
              leftIcon={<UploadCloud className="h-4 w-4" />}
            >
              Subir {files.length} archivo{files.length === 1 ? "" : "s"}
            </Button>
            <Button
              variant="ghost"
              onClick={() => setFiles([])}
              disabled={upload.isPending}
            >
              Vaciar
            </Button>
          </div>
        </div>
      )}

      {upload.isPending && (
        <div className="flex items-center gap-2 text-sm text-text-muted">
          <Spinner size="sm" /> Subiendo y escaneando metadatos…
        </div>
      )}

      {/* Resultado de la última subida */}
      {uploaded === null ? null : uploaded.length === 0 ? (
        <EmptyState
          icon={<Music />}
          title="Sin canciones nuevas"
          description="No se pudo extraer ninguna canción de los archivos subidos."
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
            {uploaded.map((s) => (
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
