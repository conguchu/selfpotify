"use client";

import { useEffect } from "react";
import { usePathname, useRouter } from "next/navigation";
import { usePublicConfig } from "@/lib/query/hooks";
import { Spinner } from "@/components/ui/Spinner";

/**
 * Gate global de setup. Mientras `setupComplete=false`, cualquier ruta (con o sin
 * sesión) redirige al wizard `/setup`. Una vez completado, `/setup` queda
 * inaccesible (redirige a `/`). El resto de rutas siguen su lógica de auth normal.
 */
export function SetupGate({ children }: { children: React.ReactNode }) {
  const router = useRouter();
  const pathname = usePathname();
  const { data, isLoading, isError } = usePublicConfig();

  const setupComplete = data?.setupComplete;
  const onSetup = pathname === "/setup";

  useEffect(() => {
    if (setupComplete === undefined) return;
    if (!setupComplete && !onSetup) {
      router.replace("/setup");
    } else if (setupComplete && onSetup) {
      router.replace("/");
    }
  }, [setupComplete, onSetup, router]);

  // Si el backend no responde, dejamos pasar para no bloquear toda la app.
  if (isError) return <>{children}</>;

  // Mientras cargamos, o durante una redirección pendiente, mostramos spinner.
  if (
    isLoading ||
    setupComplete === undefined ||
    (!setupComplete && !onSetup) ||
    (setupComplete && onSetup)
  ) {
    return (
      <div className="flex min-h-screen items-center justify-center">
        <Spinner size="lg" />
      </div>
    );
  }

  return <>{children}</>;
}
