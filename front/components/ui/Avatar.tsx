"use client";

import * as React from "react";
import Image from "next/image";
import { cn } from "@/lib/utils";

type Size = "sm" | "md" | "lg";

const SIZE: Record<Size, { box: string; px: number; text: string }> = {
  sm: { box: "h-8 w-8", px: 32, text: "text-xs" },
  md: { box: "h-10 w-10", px: 40, text: "text-sm" },
  lg: { box: "h-14 w-14", px: 56, text: "text-base" },
};

export function Avatar({
  src,
  alt,
  size = "md",
  className,
}: {
  src?: string | null;
  alt: string;
  size?: Size;
  className?: string;
}) {
  const [errored, setErrored] = React.useState(false);
  const cfg = SIZE[size];
  const initial = alt.trim().charAt(0).toUpperCase() || "?";
  const showImg = src && !errored;
  return (
    <div
      className={cn(
        "relative inline-flex shrink-0 items-center justify-center overflow-hidden rounded-full bg-bg-hover text-text-muted",
        cfg.box,
        cfg.text,
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
        <span className="font-semibold">{initial}</span>
      )}
    </div>
  );
}
