"use client";

import * as React from "react";
import { cn } from "@/lib/utils";

interface CtxValue {
  open: boolean;
  setOpen: (v: boolean) => void;
}

const Ctx = React.createContext<CtxValue | null>(null);

export function DropdownMenu({ children }: { children: React.ReactNode }) {
  const [open, setOpen] = React.useState(false);
  const ref = React.useRef<HTMLDivElement>(null);

  React.useEffect(() => {
    if (!open) return;
    const onClick = (e: MouseEvent) => {
      if (ref.current && !ref.current.contains(e.target as Node)) {
        setOpen(false);
      }
    };
    const onKey = (e: KeyboardEvent) => {
      if (e.key === "Escape") setOpen(false);
    };
    document.addEventListener("mousedown", onClick);
    document.addEventListener("keydown", onKey);
    return () => {
      document.removeEventListener("mousedown", onClick);
      document.removeEventListener("keydown", onKey);
    };
  }, [open]);

  return (
    <Ctx.Provider value={{ open, setOpen }}>
      <div ref={ref} className="relative">
        {children}
      </div>
    </Ctx.Provider>
  );
}

export function DropdownTrigger({
  children,
  className,
}: {
  children: React.ReactNode;
  className?: string;
}) {
  const ctx = React.useContext(Ctx);
  if (!ctx) throw new Error("DropdownTrigger must be inside DropdownMenu");
  return (
    <button
      type="button"
      onClick={() => ctx.setOpen(!ctx.open)}
      className={cn("inline-flex items-center", className)}
      aria-haspopup="menu"
      aria-expanded={ctx.open}
    >
      {children}
    </button>
  );
}

export function DropdownContent({
  children,
  align = "end",
  className,
}: {
  children: React.ReactNode;
  align?: "start" | "end";
  className?: string;
}) {
  const ctx = React.useContext(Ctx);
  if (!ctx) throw new Error("DropdownContent must be inside DropdownMenu");
  if (!ctx.open) return null;
  return (
    <div
      role="menu"
      className={cn(
        "absolute z-40 mt-2 min-w-48 overflow-hidden rounded-md border border-border bg-bg-elevated p-1 shadow-xl",
        align === "end" ? "right-0" : "left-0",
        className,
      )}
    >
      {children}
    </div>
  );
}

export function DropdownItem({
  children,
  onClick,
  disabled,
  variant = "neutral",
  className,
}: {
  children: React.ReactNode;
  onClick?: () => void;
  disabled?: boolean;
  variant?: "neutral" | "danger";
  className?: string;
}) {
  const ctx = React.useContext(Ctx);
  return (
    <button
      type="button"
      role="menuitem"
      disabled={disabled}
      onClick={() => {
        onClick?.();
        ctx?.setOpen(false);
      }}
      className={cn(
        "flex w-full items-center gap-2 rounded px-3 py-2 text-left text-sm transition-colors",
        "disabled:cursor-not-allowed disabled:opacity-50",
        variant === "danger"
          ? "text-danger hover:bg-danger/10"
          : "text-text hover:bg-bg-hover",
        className,
      )}
    >
      {children}
    </button>
  );
}

export function DropdownLabel({
  children,
  className,
}: {
  children: React.ReactNode;
  className?: string;
}) {
  return (
    <div
      className={cn(
        "px-3 py-2 text-xs uppercase tracking-wide text-text-subtle",
        className,
      )}
    >
      {children}
    </div>
  );
}

export function DropdownSeparator() {
  return <div className="my-1 h-px bg-border" />;
}
