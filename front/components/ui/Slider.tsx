"use client";

import * as React from "react";
import { cn } from "@/lib/utils";

export interface SliderProps {
  value: number;
  max: number;
  min?: number;
  step?: number;
  onChange: (v: number) => void;
  onCommit?: (v: number) => void;
  className?: string;
  ariaLabel: string;
}

export function Slider({
  value,
  max,
  min = 0,
  step = 1,
  onChange,
  onCommit,
  className,
  ariaLabel,
}: SliderProps) {
  const safeMax = max > 0 ? max : 1;
  const pct = ((value - min) / (safeMax - min)) * 100;

  return (
    <div
      className={cn(
        "group relative flex h-4 w-full cursor-pointer items-center",
        className,
      )}
    >
      <div className="relative h-1 w-full overflow-hidden rounded-full bg-bg-hover">
        <div
          className="absolute inset-y-0 left-0 bg-text-muted group-hover:bg-accent transition-colors"
          style={{ width: `${pct}%` }}
        />
      </div>
      <input
        type="range"
        aria-label={ariaLabel}
        value={value}
        min={min}
        max={safeMax}
        step={step}
        onChange={(e) => onChange(Number(e.target.value))}
        onMouseUp={(e) => onCommit?.(Number((e.target as HTMLInputElement).value))}
        onTouchEnd={(e) => onCommit?.(Number((e.target as HTMLInputElement).value))}
        className="absolute inset-0 h-full w-full cursor-pointer appearance-none bg-transparent
          [&::-webkit-slider-thumb]:appearance-none [&::-webkit-slider-thumb]:h-3 [&::-webkit-slider-thumb]:w-3
          [&::-webkit-slider-thumb]:rounded-full [&::-webkit-slider-thumb]:bg-text [&::-webkit-slider-thumb]:opacity-0
          group-hover:[&::-webkit-slider-thumb]:opacity-100 [&::-webkit-slider-thumb]:transition-opacity"
      />
    </div>
  );
}
