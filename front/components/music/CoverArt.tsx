"use client";

import * as React from "react";
import Image from "next/image";
import { Music } from "lucide-react";
import { cn } from "@/lib/utils";

type Size = "sm" | "md" | "lg" | "xl";

const SIZE: Record<Size, { box: string; px: number; icon: string }> = {
  sm: { box: "h-10 w-10", px: 40, icon: "h-5 w-5" },
  md: { box: "h-14 w-14", px: 56, icon: "h-6 w-6" },
  lg: { box: "h-40 w-40", px: 160, icon: "h-14 w-14" },
  xl: { box: "h-56 w-56", px: 224, icon: "h-20 w-20" },
};

export function CoverArt({
  src,
  alt,
  size = "md",
  rounded = "md",
  className,
}: {
  src?: string | null;
  alt: string;
  size?: Size;
  rounded?: "md" | "lg" | "full";
  className?: string;
}) {
  const [errored, setErrored] = React.useState(false);
  const cfg = SIZE[size];
  const radius =
    rounded === "lg" ? "rounded-lg" : rounded === "full" ? "rounded-full" : "rounded-md";
  const showImg = src && !errored;
  return (
    <div
      className={cn(
        "relative shrink-0 overflow-hidden border border-border bg-bg-card",
        cfg.box,
        radius,
        className,
      )}
    >
      {showImg ? (
        <Image
          src={src}
          alt={alt}
          fill
          sizes={`${cfg.px}px`}
          className="object-cover"
          onError={() => setErrored(true)}
          unoptimized
        />
      ) : (
        <div className="flex h-full w-full items-center justify-center text-text-muted">
          <Music className={cfg.icon} aria-hidden="true" />
        </div>
      )}
    </div>
  );
}
