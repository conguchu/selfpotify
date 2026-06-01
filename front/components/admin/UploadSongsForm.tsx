"use client";

import { useRef, useState } from "react";
import { UploadCloud, Trash2, FileAudio, Check, User as UserIcon } from "lucide-react";
import { toast } from "sonner";
import { Button } from "@/components/ui/Button";
import { Input } from "@/components/ui/Input";
import { Label } from "@/components/ui/Label";
import { Spinner } from "@/components/ui/Spinner";
import { ArtistPickerModal } from "@/components/admin/ArtistPickerModal";
import { CoverDropzone } from "@/components/admin/CoverDropzone";
import {
  useServerConfig,
  useUploadSongsToStaging,
  useCommitSongs,
} from "@/lib/query/hooks";
import { cn } from "@/lib/utils";
import type { SongDraft } from "@/lib/types";

const ACCEPT = ".mp3,.wav,audio/mpeg,audio/wav,audio/x-wav";

function isAudio(file: File): boolean {
  const name = file.name.toLowerCase();
  return name.endsWith(".mp3") || name.endsWith(".wav");
}

/** Borrador editable en el panel (extiende el del backend con el artista resuelto). */
interface DraftRow extends SongDraft {
  artistId: number | null;
  artistLabel: string; // nombre mostrado (existente elegido o el extraído)
}

export function UploadSongsForm({ onDone }: { onDone?: () => void } = {}) {
  const config = useServerConfig();
  const stage = useUploadSongsToStaging();
  const commit = useCommitSongs();
  const inputRef = useRef<HTMLInputElement | null>(null);

  const [files, setFiles] = useState<File[]>([]);
  const [dragging, setDragging] = useState(false);
  const [target, setTarget] = useState<string>("");
  const [seeded, setSeeded] = useState(false);
  const [drafts, setDrafts] = useState<DraftRow[] | null>(null);
  const [pickerFor, setPickerFor] = useState<number | null>(null);

  const scanPaths = config.data?.scanPaths ?? [];
  // Destino por defecto: la primera carpeta de música configurada.
  if (!seeded && config.data) {
    setTarget(scanPaths[0] ?? "");
    setSeeded(true);
  }

  const addFiles = (incoming: FileList | File[]) => {
    const accepted: File[] = [];
    let rejected = 0;
    for (const f of Array.from(incoming)) {
      if (isAudio(f)) accepted.push(f);
      else rejected++;
    }
    if (rejected > 0) {
      toast.error(`${rejected} archivo(s) ignorado(s): solo .mp3 y .wav`);
    }
    if (accepted.length === 0) return;
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

  const onUpload = async () => {
    if (files.length === 0) {
      toast.error("Añade al menos un archivo");
      return;
    }
    if (!target) {
      toast.error("Elige una carpeta de música destino");
      return;
    }
    try {
      const result = await stage.mutateAsync(files);
      setDrafts(
        result.map((d) => ({
          ...d,
          artistId: d.suggestedArtistId,
          artistLabel: d.artistName ?? "",
        })),
      );
      setFiles([]);
      toast.success("Revisa los datos antes de confirmar");
    } catch (err) {
      toast.error(err instanceof Error ? err.message : "Error al subir");
    }
  };

  const updateDraft = (i: number, patch: Partial<DraftRow>) => {
    setDrafts((prev) =>
      prev ? prev.map((d, idx) => (idx === i ? { ...d, ...patch } : d)) : prev,
    );
  };

  const onConfirm = async () => {
    if (!drafts || drafts.length === 0) return;
    try {
      const result = await commit.mutateAsync({
        targetPath: target,
        songs: drafts.map((d) => ({
          stagingToken: d.stagingToken,
          fileName: d.fileName,
          title: d.title,
          // artista existente elegido; si no, crear con el nombre mostrado
          artistId: d.artistId ?? undefined,
          newArtistName: d.artistId ? undefined : d.artistLabel || undefined,
          genre: d.genre,
          bpm: d.bpm,
          duration_ms: d.duration_ms,
          picture_url: d.picture_url,
        })),
      });
      toast.success(
        `Importadas ${result.length} canci${result.length === 1 ? "ón" : "ones"}`,
      );
      setDrafts(null);
      onDone?.();
    } catch (err) {
      toast.error(err instanceof Error ? err.message : "Error al confirmar");
    }
  };

  if (config.isLoading) {
    return (
      <div className="flex items-center justify-center py-10">
        <Spinner />
      </div>
    );
  }

  // ---- Fase 2: preview editable -------------------------------------------
  if (drafts) {
    return (
      <div className="flex flex-col gap-4">
        <p className="text-sm text-text-muted">
          Ajusta los datos de cada canción antes de incorporarla a la biblioteca.
        </p>
        <ul className="flex flex-col gap-3">
          {drafts.map((d, i) => (
            <li
              key={d.stagingToken}
              className="flex flex-col gap-3 rounded-md border border-border bg-bg p-3 sm:flex-row"
            >
              <CoverDropzone
                value={d.picture_url}
                onChange={(url) => updateDraft(i, { picture_url: url })}
              />
              <div className="grid flex-1 grid-cols-1 gap-2 sm:grid-cols-2">
                <div className="flex flex-col gap-1 sm:col-span-2">
                  <Label htmlFor={`t-${i}`}>Título</Label>
                  <Input
                    id={`t-${i}`}
                    value={d.title}
                    onChange={(e) => updateDraft(i, { title: e.target.value })}
                  />
                </div>
                <div className="flex flex-col gap-1 sm:col-span-2">
                  <Label>Artista</Label>
                  <Button
                    type="button"
                    variant="secondary"
                    onClick={() => setPickerFor(i)}
                    leftIcon={<UserIcon className="h-4 w-4" />}
                    className="justify-start"
                  >
                    {d.artistLabel || "Sin artista — elegir"}
                    {d.artistId ? (
                      <Check className="ml-auto h-4 w-4 text-accent" />
                    ) : null}
                  </Button>
                </div>
                <div className="flex flex-col gap-1">
                  <Label htmlFor={`g-${i}`}>Género</Label>
                  <Input
                    id={`g-${i}`}
                    value={d.genre ?? ""}
                    onChange={(e) => updateDraft(i, { genre: e.target.value })}
                  />
                </div>
                <div className="flex flex-col gap-1">
                  <Label htmlFor={`b-${i}`}>BPM</Label>
                  <Input
                    id={`b-${i}`}
                    type="number"
                    min={0}
                    value={d.bpm || ""}
                    onChange={(e) => updateDraft(i, { bpm: Number(e.target.value) })}
                  />
                </div>
                <div className="flex flex-col gap-1 sm:col-span-2">
                  <Label htmlFor={`d-${i}`}>Duración (ms)</Label>
                  <Input
                    id={`d-${i}`}
                    type="number"
                    min={0}
                    value={d.duration_ms || ""}
                    onChange={(e) =>
                      updateDraft(i, { duration_ms: Number(e.target.value) })
                    }
                  />
                </div>
              </div>
            </li>
          ))}
        </ul>
        <div className="flex items-center gap-3">
          <Button
            onClick={onConfirm}
            loading={commit.isPending}
            leftIcon={<Check className="h-4 w-4" />}
          >
            Confirmar e importar {drafts.length}
          </Button>
          <Button
            variant="ghost"
            onClick={() => setDrafts(null)}
            disabled={commit.isPending}
          >
            Cancelar
          </Button>
        </div>

        <ArtistPickerModal
          open={pickerFor !== null}
          onClose={() => setPickerFor(null)}
          currentArtistId={pickerFor !== null ? drafts[pickerFor]?.artistId : null}
          onSelect={(a) => {
            if (pickerFor !== null) {
              updateDraft(pickerFor, { artistId: a.id, artistLabel: a.name });
            }
          }}
        />
      </div>
    );
  }

  // ---- Fase 1: elegir destino y archivos ----------------------------------
  return (
    <div className="flex flex-col gap-4">
      {/* Carpeta de música destino (una de las configuradas) */}
      <div className="flex flex-col gap-1.5">
        <Label htmlFor="upload-target">Carpeta de música destino</Label>
        {scanPaths.length === 0 ? (
          <p className="rounded-md border border-border bg-bg px-3 py-2 text-sm text-text-muted">
            No hay carpetas de música configuradas. Añade una en{" "}
            <span className="text-text">Ajustes → Biblioteca</span> antes de subir.
          </p>
        ) : (
          <select
            id="upload-target"
            value={target}
            onChange={(e) => setTarget(e.target.value)}
            className={cn(
              "h-10 w-full rounded-md border border-border bg-bg-card px-3 text-sm text-text",
              "focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-accent focus-visible:border-accent",
            )}
          >
            {scanPaths.map((p) => (
              <option key={p} value={p}>
                {p}
              </option>
            ))}
          </select>
        )}
        <p className="text-xs text-text-subtle">
          Las canciones subidas se guardan en una subcarpeta{" "}
          <code>selfpotify_added</code> dentro de la carpeta elegida.
        </p>
      </div>

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
        onDrop={(e) => {
          e.preventDefault();
          setDragging(false);
          if (e.dataTransfer.files?.length) addFiles(e.dataTransfer.files);
        }}
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
              onClick={onUpload}
              loading={stage.isPending}
              leftIcon={<UploadCloud className="h-4 w-4" />}
            >
              Subir y revisar {files.length} archivo{files.length === 1 ? "" : "s"}
            </Button>
            <Button
              variant="ghost"
              onClick={() => setFiles([])}
              disabled={stage.isPending}
            >
              Vaciar
            </Button>
          </div>
        </div>
      )}

      {stage.isPending && (
        <div className="flex items-center gap-2 text-sm text-text-muted">
          <Spinner size="sm" /> Subiendo y extrayendo metadatos…
        </div>
      )}
    </div>
  );
}
