"use client";

import { use } from "react";
import { FollowListPage } from "@/components/user/FollowListPage";

/**
 * Lista de usuarios a los que sigue {@code [id]}. Misma vista que
 * {@code /user/[id]/followers} con el modo opuesto.
 */
export default function UserFollowingPage({
  params,
}: {
  params: Promise<{ id: string }>;
}) {
  const { id } = use(params);
  return <FollowListPage userId={Number.parseInt(id, 10)} mode="following" />;
}
