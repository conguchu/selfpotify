"use client";

import * as React from "react";
import { cn } from "@/lib/utils";
import { Spinner } from "./Spinner";

type Variant = "primary" | "secondary" | "ghost" | "danger" | "outline";
type Size = "sm" | "md" | "lg";

export interface ButtonProps
  extends React.ButtonHTMLAttributes<HTMLButtonElement> {
  variant?: Variant;
  size?: Size;
  loading?: boolean;
  leftIcon?: React.ReactNode;
  rightIcon?: React.ReactNode;
}

const VARIANT: Record<Variant, string> = {
  primary:
    "bg-accent text-on-accent hover:bg-accent-hover active:bg-accent-active disabled:bg-accent/50",
  secondary:
    "bg-bg-card text-text hover:bg-bg-hover border border-border",
  ghost:
    "bg-transparent text-text hover:bg-bg-hover",
  outline:
    "bg-transparent text-text border border-border hover:bg-bg-hover hover:border-text-subtle",
  danger:
    "bg-danger text-text hover:bg-danger/90",
};

const SIZE: Record<Size, string> = {
  sm: "h-8 px-3 text-sm rounded",
  md: "h-10 px-4 text-sm rounded-md",
  lg: "h-12 px-6 text-base rounded-md",
};

export const Button = React.forwardRef<HTMLButtonElement, ButtonProps>(
  function Button(
    {
      variant = "primary",
      size = "md",
      loading,
      leftIcon,
      rightIcon,
      className,
      children,
      disabled,
      ...rest
    },
    ref,
  ) {
    return (
      <button
        ref={ref}
        className={cn(
          "inline-flex items-center justify-center gap-2 font-medium transition-colors",
          "focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-accent focus-visible:ring-offset-2 focus-visible:ring-offset-bg",
          "disabled:cursor-not-allowed disabled:opacity-60",
          VARIANT[variant],
          SIZE[size],
          className,
        )}
        disabled={disabled || loading}
        {...rest}
      >
        {loading ? <Spinner size="sm" /> : leftIcon}
        {children}
        {!loading && rightIcon}
      </button>
    );
  },
);
