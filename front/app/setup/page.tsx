"use client";

import { useEffect, useMemo, useState } from "react";
import { useRouter } from "next/navigation";
import { toast } from "sonner";
import { useQueryClient } from "@tanstack/react-query";
import { Plus, Trash2, Check } from "lucide-react";
import { Button } from "@/components/ui/Button";
import { Input } from "@/components/ui/Input";
import { Label } from "@/components/ui/Label";
import { Switch } from "@/components/ui/Switch";
import { Spinner } from "@/components/ui/Spinner";
import Image from "next/image";
import { usePublicConfig, useUsers, queryKeys } from "@/lib/query/hooks";
import { updateBranding, uploadLogo, setupServer } from "@/lib/api/config";
import { createUser } from "@/lib/api/users";
import { derivePalette, onAccentFor } from "@/lib/palette";
import { API_BASE } from "@/lib/api/client";
import { resizeImageToFit, formatBytes } from "@/lib/image";

interface NewUser {
  username: string;
  password: string;
  isAdmin: boolean;
}

const STEPS = ["Branding", "Biblioteca", "Usuarios", "Confirmar"] as const;

function colorLabel(key: string): string {
  return key.replace(/^--color-/, "").replace(/-/g, " ");
}

export default function SetupWizard() {
  const router = useRouter();
  const qc = useQueryClient();
  const { data, isLoading } = usePublicConfig();
  const existingUsers = useUsers();

  const lastfmEnabled = data?.lastfmEnabled ?? false;
  const autoLibraryPath = data?.musicLibraryPath ?? null;

  const [step, setStep] = useState(0);
  const [submitting, setSubmitting] = useState(false);

  // Paso 1 — branding
  const [appName, setAppName] = useState("");
  const [colors, setColors] = useState<Record<string, string>>({});
  const [primary, setPrimary] = useState("#b91c1c");
  const [secondary, setSecondary] = useState("#0a0a0a");
  const [advanced, setAdvanced] = useState(false);
  const [logoFile, setLogoFile] = useState<File | null>(null);
  const [seeded, setSeeded] = useState(false);

  // Paso 2 — biblioteca
  const [scanPaths, setScanPaths] = useState<string[]>([]);
  const [pathInput, setPathInput] = useState("");
  const [intervalSeconds, setIntervalSeconds] = useState(3600);
  const [autoCompleteMetadata, setAutoCompleteMetadata] = useState(false);

  // Paso 3 — usuarios
  const [users, setUsers] = useState<NewUser[]>([]);
  const [uUser, setUUser] = useState("");
  const [uPass, setUPass] = useState("");
  const [uAdmin, setUAdmin] = useState(false);

  // Sembrar valores iniciales con la config pública la primera vez.
  if (!seeded && data) {
    setAppName(data.branding.appName ?? "");
    const seedColors = { ...data.branding.colors };
    setColors(seedColors);
    if (seedColors["--color-accent"]) setPrimary(seedColors["--color-accent"]);
    if (seedColors["--color-bg"]) setSecondary(seedColors["--color-bg"]);
    setSeeded(true);
  }

  const colorKeys = useMemo(() => Object.keys(colors), [colors]);

  // Preview en vivo: aplica la paleta editada a toda la pantalla (WYSIWYG).
  useEffect(() => {
    const root = document.documentElement;
    for (const [key, value] of Object.entries(colors)) {
      if (key.startsWith("--color-") && value) {
        root.style.setProperty(key, value);
      }
    }
    const accent = colors["--color-accent"];
    if (accent) root.style.setProperty("--color-on-accent", onAccentFor(accent));
  }, [colors]);

  // Preview del logo: el archivo recién elegido o, si no, el ya guardado.
  const [logoPreview, setLogoPreview] = useState<string | null>(null);
  useEffect(() => {
    if (!logoFile) {
      setLogoPreview(null);
      return;
    }
    const url = URL.createObjectURL(logoFile);
    setLogoPreview(url);
    return () => URL.revokeObjectURL(url);
  }, [logoFile]);

  // Al elegir logo: si excede el máximo del backend, se redimensiona en cliente.
  const onLogoSelected = async (file: File | null) => {
    if (!file) {
      setLogoFile(null);
      return;
    }
    const maxBytes = data?.logoMaxBytes ?? 0;
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

  // Recalcula la paleta completa desde primario+secundario (modo básico).
  const applySeed = (next: { primary?: string; secondary?: string }) => {
    const p = next.primary ?? primary;
    const s = next.secondary ?? secondary;
    if (next.primary !== undefined) setPrimary(p);
    if (next.secondary !== undefined) setSecondary(s);
    setColors((prev) => derivePalette(p, s, prev));
  };

  if (isLoading || !data) {
    return (
      <div className="flex min-h-screen items-center justify-center">
        <Spinner size="lg" />
      </div>
    );
  }

  const addPath = () => {
    const p = pathInput.trim();
    if (!p) return;
    if (scanPaths.includes(p)) {
      toast.error("Esa ruta ya está en la lista");
      return;
    }
    setScanPaths((prev) => [...prev, p]);
    setPathInput("");
  };

  const addUser = () => {
    const u = uUser.trim();
    if (!u || !uPass) {
      toast.error("Rellena usuario y contraseña");
      return;
    }
    if (users.some((x) => x.username === u)) {
      toast.error("Ese usuario ya está en la lista");
      return;
    }
    if ((existingUsers.data ?? []).some((x) => x.username === u)) {
      toast.error("Ese usuario ya existe en el servidor");
      return;
    }
    setUsers((prev) => [...prev, { username: u, password: uPass, isAdmin: uAdmin }]);
    setUUser("");
    setUPass("");
    setUAdmin(false);
  };

  const finish = async () => {
    if (intervalSeconds < 30 || intervalSeconds > 86400) {
      toast.error("El intervalo debe estar entre 30 y 86400 segundos");
      setStep(1);
      return;
    }
    setSubmitting(true);
    try {
      // 1) Branding (nombre + colores) — antes del commit final.
      await updateBranding({ appName: appName.trim() || undefined, colors });
      // 2) Logo opcional.
      if (logoFile) {
        await uploadLogo(logoFile);
      }
      // 3) Usuarios — se crean mientras el gate sigue abierto.
      for (const u of users) {
        await createUser(u);
      }
      // 4) Commit final: persiste biblioteca y marca setupComplete=true.
      await setupServer({
        appName: appName.trim() || undefined,
        scanPaths,
        scanIntervalSeconds: intervalSeconds,
        autoCompleteMetadata: lastfmEnabled && autoCompleteMetadata,
      });

      await qc.invalidateQueries({ queryKey: queryKeys.publicConfig });
      toast.success("Configuración completada");
      router.replace("/login");
    } catch (err) {
      toast.error(err instanceof Error ? err.message : "Error al completar el setup");
      setSubmitting(false);
    }
  };

  return (
    <div className="mx-auto flex min-h-screen w-full max-w-2xl flex-col gap-6 px-4 py-10">
      <header className="flex flex-col gap-1">
        <h1 className="text-2xl font-semibold text-text">
          Configuración inicial
        </h1>
        <p className="text-sm text-text-muted">
          Deja el servidor operativo. Este asistente solo aparece en el primer
          arranque.
        </p>
      </header>

      {/* Stepper */}
      <ol className="flex items-center gap-2">
        {STEPS.map((s, i) => (
          <li key={s} className="flex flex-1 items-center gap-2">
            <span
              className={`flex h-7 w-7 shrink-0 items-center justify-center rounded-full text-xs font-semibold ${
                i < step
                  ? "bg-accent text-on-accent"
                  : i === step
                    ? "bg-accent/30 text-text ring-2 ring-accent"
                    : "bg-bg-hover text-text-subtle"
              }`}
            >
              {i < step ? <Check className="h-4 w-4" /> : i + 1}
            </span>
            <span
              className={`hidden text-sm sm:block ${
                i === step ? "text-text" : "text-text-muted"
              }`}
            >
              {s}
            </span>
          </li>
        ))}
      </ol>

      <div className="rounded-lg border border-border bg-bg-card p-5">
        {step === 0 && (
          <div className="flex flex-col gap-5">
            <div className="flex flex-col gap-1.5">
              <Label htmlFor="appName">Nombre de la app</Label>
              <Input
                id="appName"
                value={appName}
                onChange={(e) => setAppName(e.target.value)}
                placeholder="selfpotify"
                maxLength={64}
              />
            </div>

            <div className="flex flex-col gap-1.5">
              <Label htmlFor="logo">Logo (opcional)</Label>
              <div className="flex items-center gap-3">
                {(() => {
                  const existing = data.branding.logoUrl
                    ? data.branding.logoUrl.startsWith("http")
                      ? data.branding.logoUrl
                      : `${API_BASE}${data.branding.logoUrl}`
                    : null;
                  const shown = logoPreview ?? existing;
                  return shown ? (
                    <span className="relative h-14 w-14 shrink-0 overflow-hidden rounded-md border border-border bg-bg">
                      <Image
                        src={shown}
                        alt="Logo"
                        fill
                        sizes="56px"
                        className="object-contain"
                        unoptimized
                      />
                    </span>
                  ) : null;
                })()}
                <input
                  id="logo"
                  type="file"
                  accept="image/png,image/jpeg,image/svg+xml,image/webp"
                  onChange={(e) => onLogoSelected(e.target.files?.[0] ?? null)}
                  className="text-sm text-text-muted file:mr-3 file:rounded-md file:border-0 file:bg-bg-hover file:px-3 file:py-1.5 file:text-text"
                />
              </div>
              <p className="text-xs text-text-subtle">
                PNG, JPEG, SVG o WebP. Máximo {formatBytes(data.logoMaxBytes)}. Si
                lo superas, la imagen se redimensiona automáticamente.
              </p>
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

              {/* Modo básico: solo primario + secundario, el resto se autocompleta. */}
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

              {/* Previsualización de la paleta derivada. */}
              <div className="flex flex-wrap gap-1.5">
                {colorKeys.map((key) => (
                  <span
                    key={key}
                    title={`${colorLabel(key)} ${colors[key]}`}
                    className="h-6 w-6 rounded border border-border"
                    style={{ backgroundColor: colors[key] }}
                  />
                ))}
              </div>

              {/* Modo avanzado: los 14 colores, ya rellenados y editables. */}
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
          </div>
        )}

        {step === 1 && (
          <div className="flex flex-col gap-5">
            <div className="flex flex-col gap-1.5">
              <Label htmlFor="path">Carpetas de música</Label>
              <div className="flex gap-2">
                <Input
                  id="path"
                  value={pathInput}
                  onChange={(e) => setPathInput(e.target.value)}
                  onKeyDown={(e) => {
                    if (e.key === "Enter") {
                      e.preventDefault();
                      addPath();
                    }
                  }}
                  placeholder="/ruta/absoluta/a/tu/musica"
                />
                <Button
                  type="button"
                  variant="secondary"
                  onClick={addPath}
                  leftIcon={<Plus className="h-4 w-4" />}
                >
                  Añadir
                </Button>
              </div>
              <p className="text-xs text-text-subtle">
                Rutas absolutas en el servidor. Se escanearán recursivamente.
              </p>
            </div>

            {autoLibraryPath && (
              <div className="flex items-center gap-2 rounded-md border border-border bg-bg px-3 py-2">
                <span className="truncate font-mono text-sm text-text">
                  {autoLibraryPath}
                </span>
                <span className="ml-auto shrink-0 rounded bg-bg-hover px-1.5 py-0.5 text-xs text-text-muted">
                  auto
                </span>
              </div>
            )}
            {autoLibraryPath && (
              <p className="-mt-2 text-xs text-text-subtle">
                Librería detectada automáticamente desde el <code>.env</code>; ya
                está incluida en el escaneo.
              </p>
            )}

            {scanPaths.length > 0 && (
              <ul className="flex flex-col gap-1.5">
                {scanPaths.map((p) => (
                  <li
                    key={p}
                    className="flex items-center gap-2 rounded-md border border-border bg-bg px-3 py-2"
                  >
                    <span className="truncate font-mono text-sm text-text">{p}</span>
                    <button
                      type="button"
                      onClick={() =>
                        setScanPaths((prev) => prev.filter((x) => x !== p))
                      }
                      className="ml-auto text-text-subtle hover:text-danger"
                      aria-label={`Quitar ${p}`}
                    >
                      <Trash2 className="h-4 w-4" />
                    </button>
                  </li>
                ))}
              </ul>
            )}

            <div className="flex flex-col gap-1.5">
              <Label htmlFor="interval">Intervalo de escaneo (segundos)</Label>
              <Input
                id="interval"
                type="number"
                min={30}
                max={86400}
                value={intervalSeconds}
                onChange={(e) => setIntervalSeconds(Number(e.target.value))}
              />
              <p className="text-xs text-text-subtle">Entre 30 y 86400.</p>
            </div>

            <div className="flex items-center justify-between rounded-md border border-border bg-bg px-3 py-2">
              <div>
                <p className="text-sm font-medium text-text">
                  Autocompletar metadatos
                </p>
                <p className="text-xs text-text-muted">
                  Usa Last.fm para clasificar géneros automáticamente.
                </p>
                {!lastfmEnabled && (
                  <p className="mt-1 text-xs text-text-subtle">
                    Requiere una API key de Last.fm. Regístrala en{" "}
                    <code>LASTFM_API_KEY</code> (.env) — obtén una en{" "}
                    <a
                      href="https://www.last.fm/api/account/create"
                      target="_blank"
                      rel="noreferrer"
                      className="text-accent hover:underline"
                    >
                      last.fm/api
                    </a>
                    .
                  </p>
                )}
              </div>
              <Switch
                ariaLabel="Autocompletar metadatos"
                checked={lastfmEnabled && autoCompleteMetadata}
                onChange={setAutoCompleteMetadata}
                disabled={!lastfmEnabled}
              />
            </div>
          </div>
        )}

        {step === 2 && (
          <div className="flex flex-col gap-5">
            <p className="text-sm text-text-muted">
              Crea las cuentas iniciales. Ya existe un administrador por defecto
              (<code>admin/admin</code>); aquí puedes añadir cuentas adicionales.
            </p>
            <div className="flex flex-col gap-3 rounded-md border border-border bg-bg p-3">
              <div className="flex flex-col gap-1.5">
                <Label htmlFor="u-user">Usuario</Label>
                <Input
                  id="u-user"
                  value={uUser}
                  onChange={(e) => setUUser(e.target.value)}
                  placeholder="nuevo_usuario"
                />
              </div>
              <div className="flex flex-col gap-1.5">
                <Label htmlFor="u-pass">Contraseña</Label>
                <Input
                  id="u-pass"
                  type="password"
                  value={uPass}
                  onChange={(e) => setUPass(e.target.value)}
                  placeholder="Contraseña inicial"
                />
              </div>
              <div className="flex items-center justify-between">
                <p className="text-sm text-text">Administrador</p>
                <Switch
                  ariaLabel="Crear como administrador"
                  checked={uAdmin}
                  onChange={setUAdmin}
                />
              </div>
              <Button
                type="button"
                variant="secondary"
                onClick={addUser}
                leftIcon={<Plus className="h-4 w-4" />}
                className="self-start"
              >
                Añadir usuario
              </Button>
            </div>

            {users.length > 0 && (
              <ul className="flex flex-col gap-1.5">
                {users.map((u) => (
                  <li
                    key={u.username}
                    className="flex items-center gap-2 rounded-md border border-border bg-bg px-3 py-2"
                  >
                    <span className="text-sm font-medium text-text">
                      {u.username}
                    </span>
                    {u.isAdmin && (
                      <span className="rounded bg-accent/30 px-1.5 py-0.5 text-xs text-text">
                        admin
                      </span>
                    )}
                    <button
                      type="button"
                      onClick={() =>
                        setUsers((prev) =>
                          prev.filter((x) => x.username !== u.username),
                        )
                      }
                      className="ml-auto text-text-subtle hover:text-danger"
                      aria-label={`Quitar ${u.username}`}
                    >
                      <Trash2 className="h-4 w-4" />
                    </button>
                  </li>
                ))}
              </ul>
            )}
          </div>
        )}

        {step === 3 && (
          <div className="flex flex-col gap-4 text-sm">
            <h2 className="text-base font-semibold text-text">Resumen</h2>
            <dl className="flex flex-col gap-2">
              <div className="flex justify-between gap-4">
                <dt className="text-text-muted">Nombre</dt>
                <dd className="text-text">{appName || "selfpotify"}</dd>
              </div>
              <div className="flex justify-between gap-4">
                <dt className="text-text-muted">Logo</dt>
                <dd className="text-text">{logoFile ? logoFile.name : "—"}</dd>
              </div>
              <div className="flex justify-between gap-4">
                <dt className="text-text-muted">Carpetas</dt>
                <dd className="text-right text-text">
                  {[autoLibraryPath, ...scanPaths].filter(Boolean).join(", ") ||
                    "ninguna"}
                </dd>
              </div>
              <div className="flex justify-between gap-4">
                <dt className="text-text-muted">Intervalo</dt>
                <dd className="text-text">{intervalSeconds}s</dd>
              </div>
              <div className="flex justify-between gap-4">
                <dt className="text-text-muted">Autocompletar</dt>
                <dd className="text-text">{autoCompleteMetadata ? "sí" : "no"}</dd>
              </div>
              <div className="flex justify-between gap-4">
                <dt className="text-text-muted">Usuarios nuevos</dt>
                <dd className="text-text">{users.length}</dd>
              </div>
            </dl>
            <p className="text-xs text-text-subtle">
              Al finalizar, el asistente quedará inaccesible y la configuración
              volverá a requerir un administrador.
            </p>
          </div>
        )}
      </div>

      <div className="flex items-center justify-between">
        <Button
          type="button"
          variant="ghost"
          onClick={() => setStep((s) => Math.max(0, s - 1))}
          disabled={step === 0 || submitting}
        >
          Atrás
        </Button>
        {step < STEPS.length - 1 ? (
          <Button type="button" onClick={() => setStep((s) => s + 1)}>
            Siguiente
          </Button>
        ) : (
          <Button type="button" onClick={finish} loading={submitting}>
            Finalizar configuración
          </Button>
        )}
      </div>
    </div>
  );
}
