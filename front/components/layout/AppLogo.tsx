"use client";

import { useState } from "react";
import Image from "next/image";
import { usePublicConfig, useAppName } from "@/lib/query/hooks";
import { API_BASE } from "@/lib/api/client";
import { cn } from "@/lib/utils";

/** Logo por defecto de la app, servido desde `front/public`. */
const DEFAULT_LOGO = "/selfpotify-logo.png";

/**
 * Logo de la app del branding. Muestra el logo subido (`branding.logoUrl`, que
 * el backend sirve en `/assets/...`) y cae al logo por defecto (`DEFAULT_LOGO`)
 * si no hay logo subido o si la imagen falla al cargar. `className` define el
 * tamaño de la caja.
 */
export function AppLogo({ className }: { className?: string }) {
  const { data } = usePublicConfig();
  const appName = useAppName();
  const [errored, setErrored] = useState(false);

  const raw = data?.branding.logoUrl;
  const uploaded = raw
    ? raw.startsWith("http")
      ? raw
      : `${API_BASE}${raw}`
    : null;
  const src = !errored && uploaded ? uploaded : DEFAULT_LOGO;

  return (
    <span
      className={cn(
        "relative inline-flex shrink-0 items-center justify-center",
        className,
      )}
    >
      <Image
        src={src}
        alt={appName}
        fill
        sizes="64px"
        className="object-contain"
        onError={() => setErrored(true)}
        unoptimized
      />
    </span>
  );
}
