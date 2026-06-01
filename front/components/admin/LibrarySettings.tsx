"use client";

import { useState } from "react";
import { Plus, Trash2, Save, FolderPlus } from "lucide-react";
import { toast } from "sonner";
import { Button } from "@/components/ui/Button";
import { Input } from "@/components/ui/Input";
import { Label } from "@/components/ui/Label";
import { Switch } from "@/components/ui/Switch";
import { Spinner } from "@/components/ui/Spinner";
import { EmptyState } from "@/components/ui/EmptyState";
import { ApiError } from "@/lib/api/client";
import {
  useAddScanPath,
  usePublicConfig,
  useRemoveScanPath,
  useServerConfig,
  useUpdateServerConfig,
} from "@/lib/query/hooks";

/**
 * Gestión de la biblioteca: rutas de escaneo (origen de música), intervalo de
 * escaneo periódico y toggles de autocompletado de metadatos (Last.fm) y
 * carátulas, igual que el wizard. Los toggles solo se pueden activar si la
 * capacidad correspondiente está habilitada en el servidor (.env).
 */
export function LibrarySettings() {
  const config = useServerConfig();
  const publicConfig = usePublicConfig();
  const addPath = useAddScanPath();
  const removePath = useRemoveScanPath();
  const saveConfig = useUpdateServerConfig();

  const [pathInput, setPathInput] = useState("");
  const [seeded, setSeeded] = useState(false);
  const [intervalSeconds, setIntervalSeconds] = useState(3600);
  const [autoMeta, setAutoMeta] = useState(false);
  const [autoCover, setAutoCover] = useState(false);

  const lastfmEnabled = publicConfig.data?.lastfmEnabled ?? false;
  const coverArtEnabled = publicConfig.data?.coverArtEnabled ?? false;

  if (!seeded && config.data) {
    setIntervalSeconds(config.data.scanIntervalSeconds);
    setAutoMeta(config.data.autoCompleteMetadata);
    setAutoCover(config.data.autoCompleteCoverArt);
    setSeeded(true);
  }

  const onAddPath = async () => {
    const p = pathInput.trim();
    if (!p) {
      toast.error("Indica una ruta absoluta");
      return;
    }
    try {
      await addPath.mutateAsync(p);
      toast.success("Ruta añadida y en escaneo");
      setPathInput("");
    } catch (err) {
      const msg =
        err instanceof ApiError && err.status === 409
          ? "Esa ruta ya estaba en la lista"
          : err instanceof Error
            ? err.message
            : "No se pudo añadir la ruta";
      toast.error(msg);
    }
  };

  const onRemovePath = async (p: string) => {
    try {
      await removePath.mutateAsync(p);
      toast.success("Ruta eliminada del escaneo");
    } catch (err) {
      toast.error(err instanceof Error ? err.message : "No se pudo quitar");
    }
  };

  const onSaveConfig = async () => {
    if (intervalSeconds < 30 || intervalSeconds > 86400) {
      toast.error("El intervalo debe estar entre 30 y 86400 segundos");
      return;
    }
    try {
      await saveConfig.mutateAsync({
        scanIntervalSeconds: intervalSeconds,
        autoCompleteMetadata: lastfmEnabled && autoMeta,
        autoCompleteCoverArt: coverArtEnabled && autoCover,
      });
      toast.success("Configuración de biblioteca guardada");
    } catch (err) {
      toast.error(err instanceof Error ? err.message : "Error al guardar");
    }
  };

  if (config.isLoading || !config.data) {
    return (
      <div className="flex items-center justify-center py-10">
        <Spinner />
      </div>
    );
  }

  const scanPaths = config.data.scanPaths;

  return (
    <div className="flex flex-col gap-6">
      {/* Rutas de escaneo */}
      <div className="flex flex-col gap-3">
        <div className="flex flex-col gap-1.5">
          <Label htmlFor="ls-path">Carpetas de música escaneadas</Label>
          <div className="flex gap-2">
            <Input
              id="ls-path"
              value={pathInput}
              onChange={(e) => setPathInput(e.target.value)}
              onKeyDown={(e) => {
                if (e.key === "Enter") {
                  e.preventDefault();
                  onAddPath();
                }
              }}
              placeholder="/ruta/absoluta/a/tu/musica"
            />
            <Button
              type="button"
              variant="secondary"
              onClick={onAddPath}
              loading={addPath.isPending}
              leftIcon={<Plus className="h-4 w-4" />}
            >
              Añadir
            </Button>
          </div>
          <p className="text-xs text-text-subtle">
            Rutas absolutas en el servidor. Se escanean recursivamente buscando{" "}
            <code>.mp3</code> y <code>.wav</code>.
          </p>
        </div>

        {scanPaths.length === 0 ? (
          <EmptyState
            icon={<FolderPlus />}
            title="Sin rutas configuradas"
            description="Añade una carpeta para que selfpotify escanee música."
          />
        ) : (
          <ul className="flex flex-col gap-1.5">
            {scanPaths.map((p) => (
              <li
                key={p}
                className="flex items-center gap-2 rounded-md border border-border bg-bg px-3 py-2"
              >
                <span className="truncate font-mono text-sm text-text">{p}</span>
                <button
                  type="button"
                  onClick={() => onRemovePath(p)}
                  className="ml-auto shrink-0 text-text-subtle hover:text-danger"
                  aria-label={`Quitar ${p}`}
                  disabled={removePath.isPending}
                >
                  <Trash2 className="h-4 w-4" />
                </button>
              </li>
            ))}
          </ul>
        )}
      </div>

      {/* Intervalo de escaneo */}
      <div className="flex flex-col gap-1.5">
        <Label htmlFor="ls-interval">Intervalo de escaneo (segundos)</Label>
        <Input
          id="ls-interval"
          type="number"
          min={30}
          max={86400}
          value={intervalSeconds}
          onChange={(e) => setIntervalSeconds(Number(e.target.value))}
          className="max-w-xs"
        />
        <p className="text-xs text-text-subtle">Entre 30 y 86400.</p>
      </div>

      {/* Toggle metadatos */}
      <div className="flex items-center justify-between rounded-md border border-border bg-bg px-3 py-2">
        <div>
          <p className="text-sm font-medium text-text">Autocompletar metadatos</p>
          <p className="text-xs text-text-muted">
            Usa Last.fm para clasificar géneros automáticamente.
          </p>
          {!lastfmEnabled && (
            <p className="mt-1 text-xs text-text-subtle">
              Requiere una API key de Last.fm en <code>LASTFM_API_KEY</code>{" "}
              (.env).
            </p>
          )}
        </div>
        <Switch
          ariaLabel="Autocompletar metadatos"
          checked={lastfmEnabled && autoMeta}
          onChange={setAutoMeta}
          disabled={!lastfmEnabled}
        />
      </div>

      {/* Toggle carátulas */}
      <div className="flex items-center justify-between rounded-md border border-border bg-bg px-3 py-2">
        <div>
          <p className="text-sm font-medium text-text">Autocompletar carátulas</p>
          <p className="text-xs text-text-muted">
            Descarga carátulas de álbumes y fotos de artistas durante el escaneo.
          </p>
          {!coverArtEnabled && (
            <p className="mt-1 text-xs text-text-subtle">
              Desactivado en el servidor. Actívalo en{" "}
              <code>COVER_ART_ENABLED</code> (.env).
            </p>
          )}
        </div>
        <Switch
          ariaLabel="Autocompletar carátulas"
          checked={coverArtEnabled && autoCover}
          onChange={setAutoCover}
          disabled={!coverArtEnabled}
        />
      </div>

      <div>
        <Button
          onClick={onSaveConfig}
          loading={saveConfig.isPending}
          leftIcon={<Save className="h-4 w-4" />}
        >
          Guardar configuración
        </Button>
      </div>
    </div>
  );
}
