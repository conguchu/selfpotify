"use client";

import { use } from "react";
import { FollowListPage } from "@/components/user/FollowListPage";

/**
 * Lista de seguidores de cualquier usuario. La detección de "lista mía"
 * (que activa los botones por fila) se hace dentro de {@link FollowListPage}
 * comparando el id de la URL con el de {@code /api/me}.
 */
export default function UserFollowersPage({
  params,
}: {
  params: Promise<{ id: string }>;
}) {
  const { id } = use(params);
  return <FollowListPage userId={Number.parseInt(id, 10)} mode="followers" />;
}
