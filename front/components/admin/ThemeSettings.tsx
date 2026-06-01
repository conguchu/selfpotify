"use client";

import { useEffect, useMemo, useRef, useState } from "react";
import { useQueryClient } from "@tanstack/react-query";
import { toast } from "sonner";
import Image from "next/image";
import { Save } from "lucide-react";
import { Button } from "@/components/ui/Button";
import { Input } from "@/components/ui/Input";
import { Label } from "@/components/ui/Label";
import { Spinner } from "@/components/ui/Spinner";
import {
  usePublicConfig,
  useServerConfig,
  useUpdateServerConfig,
  queryKeys,
} from "@/lib/query/hooks";
import { uploadLogo } from "@/lib/api/config";
import {
  derivePalette,
  onAccentFor,
  accentTextFor,
  enforceContrast,
} from "@/lib/palette";
import { THEME_PRESETS } from "@/lib/presets";
import { API_BASE } from "@/lib/api/client";
import { resizeImageToFit, formatBytes } from "@/lib/image";

function colorLabel(key: string): string {
  return key.replace(/^--color-/, "").replace(/-/g, " ");
}

/**
 * Aplica un mapa de colores a :root pasando por el mismo guard de contraste y
 * los mismos derivados (on-accent, accent-text) que la app real, para que el
 * preview sea fiel a lo que se verá tras guardar.
 */
function applyColors(colors: Record<string, string>) {
  const root = document.documentElement;
  const safe = enforceContrast(colors);
  for (const [key, value] of Object.entries(safe)) {
    if (key.startsWith("--color-") && value) root.style.setProperty(key, value);
  }
  const accent = safe["--color-accent"];
  const bg = safe["--color-bg"];
  if (accent) root.style.setProperty("--color-on-accent", onAccentFor(accent));
  if (accent && bg)
    root.style.setProperty("--color-accent-text", accentTextFor(accent, bg));
}

/**
 * Editor de branding del panel: nombre, logo y colores del tema, igual que el
 * wizard (paleta Material derivada de dos semillas, con modo avanzado). El
 * preview se aplica en vivo a toda la app; si se abandona sin guardar, el efecto
 * de limpieza restaura los colores guardados.
 */
export function ThemeSettings() {
  const qc = useQueryClient();
  const config = useServerConfig();
  const publicConfig = usePublicConfig();
  const save = useUpdateServerConfig();

  const [seeded, setSeeded] = useState(false);
  const [appName, setAppName] = useState("");
  const [colors, setColors] = useState<Record<string, string>>({});
  const [primary, setPrimary] = useState("#b91c1c");
  const [secondary, setSecondary] = useState("#0a0a0a");
  const [advanced, setAdvanced] = useState(false);
  const [logoFile, setLogoFile] = useState<File | null>(null);
  const [savingLogo, setSavingLogo] = useState(false);

  // Colores guardados, para restaurar el preview si se sale sin guardar.
  const savedColors = config.data?.branding.colors;

  if (!seeded && config.data) {
    setAppName(config.data.branding.appName ?? "");
    const seed = { ...config.data.branding.colors };
    setColors(seed);
    if (seed["--color-accent"]) setPrimary(seed["--color-accent"]);
    if (seed["--color-bg"]) setSecondary(seed["--color-bg"]);
    setSeeded(true);
  }

  const colorKeys = useMemo(() => Object.keys(colors), [colors]);
  // Muestras: el resultado real tras el guard, para que el admin vea lo que se
  // aplicará (no el valor crudo que pudo quedar ilegible).
  const previewColors = useMemo(() => enforceContrast(colors), [colors]);

  // Preview en vivo.
  useEffect(() => {
    if (Object.keys(colors).length) applyColors(colors);
  }, [colors]);

  // Al desmontar, restaura los colores realmente guardados (descarta el preview).
  const savedRef = useRef(savedColors);
  savedRef.current = savedColors;
  useEffect(() => {
    return () => {
      if (savedRef.current) applyColors(savedRef.current);
    };
  }, []);

  const applySeed = (next: { primary?: string; secondary?: string }) => {
    const p = next.primary ?? primary;
    const s = next.secondary ?? secondary;
    if (next.primary !== undefined) setPrimary(p);
    if (next.secondary !== undefined) setSecondary(s);
    setColors((prev) => derivePalette(p, s, prev));
  };

  const onLogoSelected = async (file: File | null) => {
    if (!file) {
      setLogoFile(null);
      return;
    }
    const maxBytes = publicConfig.data?.logoMaxBytes ?? 0;
    if (maxBytes > 0 && file.size > maxBytes) {
      const resized = await resizeImageToFit(file, maxBytes);
      if (resized.size <= maxBytes && resized !== file) {
        toast.info(
          `La imagen superaba ${formatBytes(maxBytes)} y se redimensionó a ${formatBytes(resized.size)}.`,
        );
        setLogoFile(resized);
        return;
      }
      if (resized.size > maxBytes) {
        toast.error(
          `No se pudo reducir la imagen por debajo de ${formatBytes(maxBytes)}. Prueba con otra.`,
        );
        return;
      }
    }
    setLogoFile(file);
  };

  const onSave = async () => {
    try {
      if (logoFile) {
        setSavingLogo(true);
        await uploadLogo(logoFile);
        setSavingLogo(false);
        setLogoFile(null);
      }
      await save.mutateAsync({
        branding: { appName: appName.trim() || undefined, colors },
      });
      await qc.invalidateQueries({ queryKey: queryKeys.publicConfig });
      toast.success("Apariencia guardada");
    } catch (err) {
      setSavingLogo(false);
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

  const logoPreviewExisting = config.data.branding.logoUrl
    ? config.data.branding.logoUrl.startsWith("http")
      ? config.data.branding.logoUrl
      : `${API_BASE}${config.data.branding.logoUrl}`
    : null;

  return (
    <div className="flex flex-col gap-5">
      <div className="flex flex-col gap-1.5">
        <Label htmlFor="ts-appname">Nombre de la app</Label>
        <Input
          id="ts-appname"
          value={appName}
          onChange={(e) => setAppName(e.target.value)}
          placeholder="selfpotify"
          maxLength={64}
        />
      </div>

      <div className="flex flex-col gap-1.5">
        <Label htmlFor="ts-logo">Logo</Label>
        <div className="flex items-center gap-3">
          {logoPreviewExisting && !logoFile ? (
            <span className="relative h-14 w-14 shrink-0 overflow-hidden rounded-md border border-border bg-bg">
              <Image
                src={logoPreviewExisting}
                alt="Logo"
                fill
                sizes="56px"
                className="object-contain"
                unoptimized
              />
            </span>
          ) : null}
          <input
            id="ts-logo"
            type="file"
            accept="image/png,image/jpeg,image/svg+xml,image/webp"
            onChange={(e) => onLogoSelected(e.target.files?.[0] ?? null)}
            className="text-sm text-text-muted file:mr-3 file:rounded-md file:border-0 file:bg-bg-hover file:px-3 file:py-1.5 file:text-text"
          />
        </div>
        {logoFile ? (
          <p className="text-xs text-text-subtle">
            Nuevo logo seleccionado: {logoFile.name}. Se subirá al guardar.
          </p>
        ) : (
          <p className="text-xs text-text-subtle">
            PNG, JPEG, SVG o WebP. Máximo{" "}
            {formatBytes(publicConfig.data?.logoMaxBytes ?? 0)}.
          </p>
        )}
      </div>

      <div className="flex flex-col gap-3">
        <div className="flex items-center justify-between">
          <Label>Colores del tema</Label>
          <Button
            type="button"
            variant="ghost"
            size="sm"
            onClick={() => setAdvanced((v) => !v)}
          >
            {advanced ? "Modo simple" : "Avanzado"}
          </Button>
        </div>

        <div className="flex flex-col gap-1.5">
          <span className="text-xs text-text-subtle">
            Temas accesibles (un clic para partir de uno):
          </span>
          <div className="flex flex-wrap gap-1.5">
            {THEME_PRESETS.map((preset) => {
              const active =
                primary.toLowerCase() === preset.primary.toLowerCase() &&
                secondary.toLowerCase() === preset.secondary.toLowerCase();
              return (
                <button
                  key={preset.name}
                  type="button"
                  onClick={() =>
                    applySeed({
                      primary: preset.primary,
                      secondary: preset.secondary,
                    })
                  }
                  title={preset.name}
                  className={`flex items-center gap-1.5 rounded-full border px-2 py-1 text-xs transition-colors ${
                    active
                      ? "border-accent text-text"
                      : "border-border text-text-muted hover:bg-bg-hover"
                  }`}
                >
                  <span className="flex">
                    <span
                      className="h-3.5 w-3.5 rounded-full border border-border"
                      style={{ backgroundColor: preset.secondary }}
                    />
                    <span
                      className="-ml-1 h-3.5 w-3.5 rounded-full border border-border"
                      style={{ backgroundColor: preset.primary }}
                    />
                  </span>
                  {preset.name}
                </button>
              );
            })}
          </div>
        </div>

        <div className="grid grid-cols-1 gap-2 sm:grid-cols-2">
          <div className="flex items-center gap-2 rounded-md border border-border bg-bg px-2 py-1.5">
            <input
              type="color"
              value={primary}
              onChange={(e) => applySeed({ primary: e.target.value })}
              className="h-7 w-9 shrink-0 cursor-pointer rounded border border-border bg-transparent"
              aria-label="Color primario"
            />
            <span className="truncate text-xs text-text-muted">
              Primario (acento)
            </span>
            <span className="ml-auto font-mono text-xs text-text-subtle">
              {primary}
            </span>
          </div>
          <div className="flex items-center gap-2 rounded-md border border-border bg-bg px-2 py-1.5">
            <input
              type="color"
              value={secondary}
              onChange={(e) => applySeed({ secondary: e.target.value })}
              className="h-7 w-9 shrink-0 cursor-pointer rounded border border-border bg-transparent"
              aria-label="Color secundario"
            />
            <span className="truncate text-xs text-text-muted">
              Secundario (fondo)
            </span>
            <span className="ml-auto font-mono text-xs text-text-subtle">
              {secondary}
            </span>
          </div>
        </div>

        <div className="flex flex-wrap gap-1.5">
          {colorKeys.map((key) => (
            <span
              key={key}
              title={`${colorLabel(key)} ${previewColors[key]}`}
              className="h-6 w-6 rounded border border-border"
              style={{ backgroundColor: previewColors[key] }}
            />
          ))}
        </div>

        {advanced && (
          <div className="grid max-h-72 grid-cols-1 gap-2 overflow-y-auto pr-1 sm:grid-cols-2">
            {colorKeys.map((key) => (
              <div
                key={key}
                className="flex items-center gap-2 rounded-md border border-border bg-bg px-2 py-1.5"
              >
                <input
                  type="color"
                  value={colors[key]}
                  onChange={(e) =>
                    setColors((prev) => ({ ...prev, [key]: e.target.value }))
                  }
                  className="h-7 w-9 shrink-0 cursor-pointer rounded border border-border bg-transparent"
                  aria-label={colorLabel(key)}
                />
                <span className="truncate text-xs capitalize text-text-muted">
                  {colorLabel(key)}
                </span>
                <span className="ml-auto font-mono text-xs text-text-subtle">
                  {colors[key]}
                </span>
              </div>
            ))}
          </div>
        )}
      </div>

      <div>
        <Button
          onClick={onSave}
          loading={save.isPending || savingLogo}
          leftIcon={<Save className="h-4 w-4" />}
        >
          Guardar apariencia
        </Button>
      </div>
    </div>
  );
}
