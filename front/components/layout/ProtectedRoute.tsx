"use client";

import { useEffect } from "react";
import { useRouter } from "next/navigation";
import { useAuthStore } from "@/lib/auth/store";
import { Spinner } from "@/components/ui/Spinner";

export function ProtectedRoute({
  children,
  requireAdmin = false,
}: {
  children: React.ReactNode;
  requireAdmin?: boolean;
}) {
  const router = useRouter();
  const hydrated = useAuthStore((s) => s.hydrated);
  const token = useAuthStore((s) => s.token);
  const isAdmin = useAuthStore((s) => s.roles.includes("ROLE_ADMIN"));

  useEffect(() => {
    if (!hydrated) return;
    if (!token) {
      router.replace("/login");
      return;
    }
    if (requireAdmin && !isAdmin) {
      router.replace("/home");
    }
  }, [hydrated, token, isAdmin, requireAdmin, router]);

  if (!hydrated || !token || (requireAdmin && !isAdmin)) {
    return (
      <div className="flex min-h-screen items-center justify-center">
        <Spinner size="lg" />
      </div>
    );
  }

  return <>{children}</>;
}
