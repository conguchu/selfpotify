"use client";

import { use } from "react";
import { UserProfileView } from "@/components/user/UserProfileView";

/**
 * Página pública del perfil de otro usuario (`/user/[id]`). Reusa el mismo
 * componente que {@code /profile} (mi perfil); el icono de lápiz solo se ve
 * cuando el username del perfil coincide con el del usuario en sesión, así
 * que un visitante nunca verá el botón de edición sobre el perfil de otro.
 *
 * <p>La vista mínima del usuario y sus playlists públicas se obtienen de
 * {@code GET /api/users/{id}/public} y {@code GET /api/playlists/user/{userId}}
 * respectivamente — ambos ya respetan la visibilidad de playlists privadas.
 */
export default function PublicUserPage({
  params,
}: {
  params: Promise<{ id: string }>;
}) {
  const { id } = use(params);
  const userId = Number.parseInt(id, 10);
  return <UserProfileView userId={userId} />;
}
