import { cn } from "@/lib/utils";

type Variant = "neutral" | "accent" | "success" | "danger";

const VARIANT: Record<Variant, string> = {
  neutral: "bg-bg-hover text-text-muted border-border",
  accent: "bg-accent-soft text-accent-hover border-accent-active",
  success: "bg-success/10 text-success border-success/30",
  danger: "bg-danger/10 text-danger border-danger/30",
};

export function Badge({
  variant = "neutral",
  className,
  children,
}: {
  variant?: Variant;
  className?: string;
  children: React.ReactNode;
}) {
  return (
    <span
      className={cn(
        "inline-flex items-center rounded-full border px-2 py-0.5 text-xs font-medium",
        VARIANT[variant],
        className,
      )}
    >
      {children}
    </span>
  );
}
