import * as React from "react";
import { cn } from "@/lib/utils";

export function Table({
  className,
  ...rest
}: React.TableHTMLAttributes<HTMLTableElement>) {
  return (
    <div className="w-full overflow-x-auto rounded-lg border border-border">
      <table
        className={cn("w-full border-collapse text-sm", className)}
        {...rest}
      />
    </div>
  );
}

export function THead({
  className,
  ...rest
}: React.HTMLAttributes<HTMLTableSectionElement>) {
  return (
    <thead
      className={cn("bg-bg-elevated text-text-muted", className)}
      {...rest}
    />
  );
}

export function TBody(
  props: React.HTMLAttributes<HTMLTableSectionElement>,
) {
  return <tbody {...props} />;
}

export function TR({
  className,
  ...rest
}: React.HTMLAttributes<HTMLTableRowElement>) {
  return (
    <tr
      className={cn("border-t border-border hover:bg-bg-card/50", className)}
      {...rest}
    />
  );
}

export function TH({
  className,
  ...rest
}: React.ThHTMLAttributes<HTMLTableCellElement>) {
  return (
    <th
      className={cn("px-4 py-3 text-left text-xs uppercase tracking-wide font-semibold", className)}
      {...rest}
    />
  );
}

export function TD({
  className,
  ...rest
}: React.TdHTMLAttributes<HTMLTableCellElement>) {
  return <td className={cn("px-4 py-3 text-text", className)} {...rest} />;
}
