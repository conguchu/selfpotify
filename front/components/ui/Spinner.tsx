import { cn } from "@/lib/utils";

export function Spinner({
  size = "md",
  className,
}: {
  size?: "sm" | "md" | "lg";
  className?: string;
}) {
  const dim = size === "sm" ? "h-4 w-4" : size === "lg" ? "h-10 w-10" : "h-6 w-6";
  return (
    <span
      className={cn(
        "inline-block rounded-full border-2 border-text-subtle border-t-accent animate-spin",
        dim,
        className,
      )}
      aria-label="Cargando"
    />
  );
}
