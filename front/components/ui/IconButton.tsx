"use client";

import * as React from "react";
import { cn } from "@/lib/utils";

type Variant = "ghost" | "solid" | "accent";
type Size = "sm" | "md" | "lg";

export interface IconButtonProps
  extends React.ButtonHTMLAttributes<HTMLButtonElement> {
  variant?: Variant;
  size?: Size;
  label: string;
}

const VARIANT: Record<Variant, string> = {
  ghost: "bg-transparent text-text-muted hover:text-text hover:bg-bg-hover",
  solid: "bg-bg-card text-text hover:bg-bg-hover border border-border",
  accent: "bg-accent text-text hover:bg-accent-hover active:bg-accent-active",
};

const SIZE: Record<Size, string> = {
  sm: "h-8 w-8 [&>svg]:h-4 [&>svg]:w-4",
  md: "h-10 w-10 [&>svg]:h-5 [&>svg]:w-5",
  lg: "h-12 w-12 [&>svg]:h-6 [&>svg]:w-6",
};

export const IconButton = React.forwardRef<HTMLButtonElement, IconButtonProps>(
  function IconButton(
    { variant = "ghost", size = "md", label, className, children, ...rest },
    ref,
  ) {
    return (
      <button
        ref={ref}
        type="button"
        aria-label={label}
        title={label}
        className={cn(
          "inline-flex items-center justify-center rounded-full transition-colors",
          "focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-accent focus-visible:ring-offset-2 focus-visible:ring-offset-bg",
          "disabled:cursor-not-allowed disabled:opacity-50",
          VARIANT[variant],
          SIZE[size],
          className,
        )}
        {...rest}
      >
        {children}
      </button>
    );
  },
);
