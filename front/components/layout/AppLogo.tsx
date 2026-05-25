"use client";

import { useState } from "react";
import Image from "next/image";
import { Disc3 } from "lucide-react";
import { usePublicConfig, useAppName } from "@/lib/query/hooks";
import { API_BASE } from "@/lib/api/client";
import { cn } from "@/lib/utils";

/**
 * Logo de la app del branding. Muestra el logo subido (`branding.logoUrl`, que
 * el backend sirve en `/assets/...`) y cae al icono `Disc3` si no hay logo o si
 * la imagen falla al cargar. `className` define el tamaño de la caja.
 */
export function AppLogo({
  className,
  iconClassName,
}: {
  className?: string;
  iconClassName?: string;
}) {
  const { data } = usePublicConfig();
  const appName = useAppName();
  const [errored, setErrored] = useState(false);

  const raw = data?.branding.logoUrl;
  const src = raw
    ? raw.startsWith("http")
      ? raw
      : `${API_BASE}${raw}`
    : null;

  return (
    <span
      className={cn(
        "relative inline-flex shrink-0 items-center justify-center",
        className,
      )}
    >
      {src && !errored ? (
        <Image
          src={src}
          alt={appName}
          fill
          sizes="64px"
          className="object-contain"
          onError={() => setErrored(true)}
          unoptimized
        />
      ) : (
        <Disc3
          className={cn("text-accent", iconClassName ?? "h-full w-full")}
          aria-hidden="true"
        />
      )}
    </span>
  );
}
