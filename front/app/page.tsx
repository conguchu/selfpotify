"use client";

import { useEffect } from "react";
import { useRouter } from "next/navigation";
import { useAuthStore } from "@/lib/auth/store";
import { Spinner } from "@/components/ui/Spinner";

export default function Home() {
  const router = useRouter();
  const hydrated = useAuthStore((s) => s.hydrated);
  const token = useAuthStore((s) => s.token);
  const roles = useAuthStore((s) => s.roles);

  useEffect(() => {
    if (!hydrated) return;
    if (!token) {
      router.replace("/login");
    } else if (roles.includes("ROLE_ADMIN")) {
      router.replace("/admin");
    } else {
      router.replace("/home");
    }
  }, [hydrated, token, roles, router]);

  return (
    <div className="flex min-h-screen items-center justify-center">
      <Spinner size="lg" />
    </div>
  );
}
