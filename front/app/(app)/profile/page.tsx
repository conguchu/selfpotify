"use client";

import { User as UserIcon } from "lucide-react";
import { EmptyState } from "@/components/ui/EmptyState";
import { Spinner } from "@/components/ui/Spinner";
import { UserProfileView } from "@/components/user/UserProfileView";
import { useMe } from "@/lib/query/hooks";

/**
 * Vista pública del perfil <em>del usuario en sesión</em> (`/profile`). Es la
 * misma vista que ve un visitante en {@code /user/[id]}, así nada en la UI
 * del usuario delata si está mirándose a sí mismo o a otro: lo único que
 * cambia es que aquí aparece un icono de lápiz junto al nombre que lleva a
 * {@code /profile/edit}.
 *
 * <p>Se resuelve el id propio leyendo {@code /api/me} (en vez de redirigir),
 * de modo que la URL del navegador queda en {@code /profile} —más cómoda de
 * compartir y de marcar como inicio—. {@link UserProfileView} ya detecta al
 * dueño comparando el username, así que se reutiliza tal cual.
 */
export default function MyProfilePage() {
  const meQuery = useMe();

  if (meQuery.isLoading) {
    return (
      <div className="flex h-64 items-center justify-center">
        <Spinner size="lg" />
      </div>
    );
  }

  if (meQuery.isError || !meQuery.data) {
    return (
      <EmptyState
        icon={<UserIcon />}
        title="No se pudo cargar tu perfil"
        description={
          meQuery.error instanceof Error
            ? meQuery.error.message
            : "Vuelve a intentarlo en un momento."
        }
      />
    );
  }

  return <UserProfileView userId={meQuery.data.id} />;
}
