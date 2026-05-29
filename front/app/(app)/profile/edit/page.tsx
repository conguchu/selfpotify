"use client";

import Link from "next/link";
import { useEffect, useRef, useState } from "react";
import {
  ArrowLeft,
  Camera,
  ImageOff,
  Save,
  Shield,
  User as UserIcon,
} from "lucide-react";
import { toast } from "sonner";
import { Avatar } from "@/components/ui/Avatar";
import { Button } from "@/components/ui/Button";
import { EmptyState } from "@/components/ui/EmptyState";
import { Input } from "@/components/ui/Input";
import { Label } from "@/components/ui/Label";
import { Spinner } from "@/components/ui/Spinner";
import {
  useDeleteMyAvatar,
  useMe,
  useUpdateMyProfile,
  useUploadMyAvatar,
} from "@/lib/query/hooks";
import { useAuthStore } from "@/lib/auth/store";

/**
 * Página de edición del perfil propio (`/profile/edit`). Es la única forma
 * desde la UI de tocar los campos del {@code Profile} del backend (nombre
 * visible y foto). La password sigue gestionándose desde el panel admin.
 *
 * <p>Se llega aquí desde el icono de lápiz que el dueño ve en
 * {@code /profile} (su propia vista pública). El nombre se persiste con
 * {@code PUT /api/me/profile}; el avatar viaja por un endpoint multipart
 * aparte ({@code POST /api/me/profile/picture}), mismo patrón que la carátula
 * de playlist. "Quitar foto" envía {@code DELETE /api/me/profile/picture} sin
 * tocar el nombre. La caché de React Query (key {@code me}) se invalida en
 * cada mutación para que el topbar y la vista pública reflejen el cambio sin
 * recargar.
 */
export default function EditProfilePage() {
  const meQuery = useMe();
  const updateProfile = useUpdateMyProfile();
  const uploadAvatar = useUploadMyAvatar();
  const deleteAvatar = useDeleteMyAvatar();

  // Username y rol los seguimos leyendo del auth store (idénticos al JWT
  // cacheado en localStorage); /api/me solo dice lo mismo, así que evitamos
  // hacer ruido si el endpoint tarda.
  const username = useAuthStore((s) => s.username) ?? "";
  const isAdmin = useAuthStore((s) => s.roles.includes("ROLE_ADMIN"));

  const [name, setName] = useState("");
  const [previewUrl, setPreviewUrl] = useState<string | null>(null);
  const fileInputRef = useRef<HTMLInputElement>(null);

  // Sincroniza el nombre del input con el último valor del servidor cada vez
  // que se carga el perfil (montaje inicial o invalidación). No usamos un
  // estado derivado para no bloquear al usuario en mitad de la edición.
  const me = meQuery.data;
  useEffect(() => {
    if (me) setName(me.displayName ?? "");
  }, [me?.displayName]);

  // Limpia las blob URLs del preview al desmontar para no filtrar memoria.
  useEffect(() => {
    return () => {
      if (previewUrl && previewUrl.startsWith("blob:")) {
        URL.revokeObjectURL(previewUrl);
      }
    };
  }, [previewUrl]);

  const handleFile = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;
    // Limpia el preview anterior y pinta el nuevo. La subida se dispara aquí
    // mismo: el botón de "guardar" se reserva para el nombre, así el flujo
    // habitual ("clic en avatar, elijo foto") no necesita un commit extra.
    if (previewUrl && previewUrl.startsWith("blob:")) {
      URL.revokeObjectURL(previewUrl);
    }
    setPreviewUrl(URL.createObjectURL(file));
    uploadAvatar.mutate(file, {
      onSuccess: () => {
        toast.success("Foto de perfil actualizada");
        // El preview se queda hasta que el siguiente render lea la URL real
        // del servidor; entonces la quitamos para evitar el flash blob → red.
        setTimeout(() => setPreviewUrl(null), 300);
      },
      onError: (err) => {
        toast.error(err instanceof Error ? err.message : "No se pudo subir la foto");
        if (previewUrl && previewUrl.startsWith("blob:")) {
          URL.revokeObjectURL(previewUrl);
        }
        setPreviewUrl(null);
      },
    });
    // Permite reseleccionar el mismo archivo después de cancelar/reintentar.
    e.target.value = "";
  };

  const handleRemoveAvatar = () => {
    deleteAvatar.mutate(undefined, {
      onSuccess: () => toast.success("Foto eliminada"),
      onError: (err) =>
        toast.error(err instanceof Error ? err.message : "No se pudo eliminar la foto"),
    });
  };

  const handleSave = () => {
    const trimmed = name.trim();
    updateProfile.mutate(
      { name: trimmed === "" ? null : trimmed },
      {
        onSuccess: () => toast.success("Perfil guardado"),
        onError: (err) =>
          toast.error(err instanceof Error ? err.message : "No se pudo guardar"),
      },
    );
  };

  if (meQuery.isLoading) {
    return (
      <div className="flex h-64 items-center justify-center">
        <Spinner size="lg" />
      </div>
    );
  }

  if (meQuery.isError || !me) {
    return (
      <EmptyState
        icon={<UserIcon />}
        title="No se pudo cargar el perfil"
        description={
          meQuery.error instanceof Error
            ? meQuery.error.message
            : "Vuelve a intentarlo en un momento."
        }
      />
    );
  }

  const visibleName = (name.trim() || username) ?? "";
  // Para el avatar pintamos: (1) el preview local si está, (2) la URL del
  // servidor cacheada en `me`, o (3) la inicial vía fallback de <Avatar>.
  const avatarSrc = previewUrl ?? me.avatarUrl;
  const isUploading = uploadAvatar.isPending;
  const isSavingName = updateProfile.isPending;

  return (
    <div className="mx-auto flex w-full max-w-2xl flex-col gap-8">
      <header className="flex flex-col gap-2">
        <Link
          href="/profile"
          className="inline-flex w-fit items-center gap-1 text-xs font-medium text-text-muted transition-colors hover:text-text"
        >
          <ArrowLeft className="h-3.5 w-3.5" />
          Volver a tu perfil
        </Link>
        <h1 className="text-3xl font-bold tracking-tight">Editar perfil</h1>
        <p className="text-sm text-text-muted">
          Cambia tu nombre visible y tu foto. Tu nombre de usuario (
          <span className="font-mono">@{username}</span>) no se puede modificar.
        </p>
      </header>

      <section className="flex flex-col items-center gap-4 rounded-lg border border-border bg-bg-card/40 p-6">
        <div className="relative">
          <Avatar
            src={previewUrl ? previewUrl : avatarSrc}
            alt={visibleName}
            size="lg"
            className="h-36 w-36 shadow-xl"
          />
          {/* Botón flotante de cámara: clic = abrir selector de archivo. */}
          <button
            type="button"
            onClick={() => fileInputRef.current?.click()}
            disabled={isUploading}
            aria-label="Cambiar foto de perfil"
            className="absolute bottom-1 right-1 inline-flex h-10 w-10 items-center justify-center rounded-full bg-accent text-on-accent shadow-lg transition-colors hover:bg-accent-hover disabled:opacity-60"
          >
            {isUploading ? <Spinner size="sm" /> : <Camera className="h-5 w-5" />}
          </button>
          <input
            ref={fileInputRef}
            type="file"
            accept="image/jpeg,image/png,image/webp"
            hidden
            onChange={handleFile}
          />
        </div>
        <p className="flex items-center gap-1.5 text-xs text-text-muted">
          {isAdmin ? (
            <Shield className="h-3.5 w-3.5 text-accent" aria-hidden />
          ) : (
            <UserIcon className="h-3.5 w-3.5" aria-hidden />
          )}
          {isAdmin ? "Administrador" : "Usuario"}
        </p>
        {me.avatarUrl ? (
          <Button
            variant="ghost"
            size="sm"
            leftIcon={<ImageOff className="h-4 w-4" />}
            onClick={handleRemoveAvatar}
            loading={deleteAvatar.isPending}
          >
            Quitar foto
          </Button>
        ) : null}
        <p className="text-center text-xs text-text-subtle">
          JPEG, PNG o WebP. Máx. 5 MB. La imagen se recorta al cuadrado por el
          centro.
        </p>
      </section>

      <section className="flex flex-col gap-3 rounded-lg border border-border bg-bg-card/40 p-6">
        <div className="flex flex-col gap-1.5">
          <Label htmlFor="profile-name">Nombre visible</Label>
          <Input
            id="profile-name"
            value={name}
            onChange={(e) => setName(e.target.value)}
            placeholder={username}
            maxLength={120}
          />
          <p className="text-xs text-text-muted">
            Es el que verán los demás usuarios. Déjalo vacío para mostrar solo
            tu nombre de usuario.
          </p>
        </div>
        <div className="flex justify-end">
          <Button
            leftIcon={<Save className="h-4 w-4" />}
            onClick={handleSave}
            loading={isSavingName}
          >
            Guardar cambios
          </Button>
        </div>
      </section>
    </div>
  );
}
