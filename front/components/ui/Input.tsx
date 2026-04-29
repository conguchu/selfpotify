"use client";

import * as React from "react";
import { cn } from "@/lib/utils";

export type InputProps = React.InputHTMLAttributes<HTMLInputElement> & {
  invalid?: boolean;
};

export const Input = React.forwardRef<HTMLInputElement, InputProps>(
  function Input({ className, invalid, ...rest }, ref) {
    return (
      <input
        ref={ref}
        className={cn(
          "h-10 w-full rounded-md border border-border bg-bg-card px-3 text-sm text-text placeholder:text-text-subtle",
          "focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-accent focus-visible:border-accent",
          "disabled:cursor-not-allowed disabled:opacity-60",
          invalid && "border-danger focus-visible:ring-danger focus-visible:border-danger",
          className,
        )}
        {...rest}
      />
    );
  },
);
