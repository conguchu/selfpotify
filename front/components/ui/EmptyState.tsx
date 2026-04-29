import { cn } from "@/lib/utils";

export function EmptyState({
  icon,
  title,
  description,
  action,
  className,
}: {
  icon?: React.ReactNode;
  title: string;
  description?: string;
  action?: React.ReactNode;
  className?: string;
}) {
  return (
    <div
      className={cn(
        "flex flex-col items-center justify-center gap-3 rounded-lg border border-dashed border-border bg-bg-card/40 px-6 py-12 text-center",
        className,
      )}
    >
      {icon ? (
        <div className="text-text-subtle [&>svg]:h-10 [&>svg]:w-10">
          {icon}
        </div>
      ) : null}
      <h3 className="text-base font-semibold text-text">{title}</h3>
      {description ? (
        <p className="max-w-sm text-sm text-text-muted">{description}</p>
      ) : null}
      {action}
    </div>
  );
}
