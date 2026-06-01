"use client";

import { useRef, useState } from "react";
import Image from "next/image";
import { ImagePlus, X } from "lucide-react";
import { toast } from "sonner";
import { Spinner } from "@/components/ui/Spinner";
import { useUploadSongCover } from "@/lib/query/hooks";
import { API_BASE } from "@/lib/api/client";
import { cn } from "@/lib/utils";

const ACCEPT = ".png,.jpg,.jpeg,.webp,image/png,image/jpeg,image/webp";

function isImage(file: File): boolean {
  return /\.(png|jpe?g|webp)$/i.test(file.name) || file.type.startsWith("image/");
}

/** Resuelve una URL de carátula (relativa /assets o absoluta) para <Image>. */
function resolveUrl(url: string): string {
  return url.startsWith("http") ? url : `${API_BASE}${url}`;
}

/**
 * Carátula por drag&drop: sube la imagen a /assets/covers (mismo almacén que las
 * carátulas normales) y reporta la URL resultante. Muestra una previsualización.
 */
export function CoverDropzone({
  value,
  onChange,
  className,
}: {
  value: string | null;
  onChange: (url: string | null) => void;
  className?: string;
}) {
  const upload = useUploadSongCover();
  const inputRef = useRef<HTMLInputElement | null>(null);
  const [dragging, setDragging] = useState(false);

  const handleFile = async (file: File) => {
    if (!isImage(file)) {
      toast.error("Solo se admiten imágenes (.png, .jpg, .webp)");
      return;
    }
    try {
      const { url } = await upload.mutateAsync(file);
      onChange(url);
    } catch (err) {
      toast.error(err instanceof Error ? err.message : "No se pudo subir la carátula");
    }
  };

  return (
    <div className={cn("flex items-center gap-3", className)}>
      <div
        role="button"
        tabIndex={0}
        onClick={() => inputRef.current?.click()}
        onKeyDown={(e) => {
          if (e.key === "Enter" || e.key === " ") {
            e.preventDefault();
            inputRef.current?.click();
          }
        }}
        onDragOver={(e) => {
          e.preventDefault();
          setDragging(true);
        }}
        onDragLeave={() => setDragging(false)}
        onDrop={(e) => {
          e.preventDefault();
          setDragging(false);
          const f = e.dataTransfer.files?.[0];
          if (f) handleFile(f);
        }}
        className={cn(
          "relative flex h-20 w-20 shrink-0 items-center justify-center overflow-hidden rounded-md border-2 border-dashed transition-colors",
          "focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-accent",
          dragging ? "border-accent bg-accent-soft/40" : "border-border bg-bg-card/40 hover:border-text-subtle",
        )}
        aria-label="Subir carátula"
      >
        {upload.isPending ? (
          <Spinner size="sm" />
        ) : value ? (
          <Image
            src={resolveUrl(value)}
            alt="Carátula"
            fill
            sizes="80px"
            className="object-cover"
            unoptimized
          />
        ) : (
          <ImagePlus className="h-6 w-6 text-text-subtle" />
        )}
        <input
          ref={inputRef}
          type="file"
          accept={ACCEPT}
          className="hidden"
          onChange={(e) => {
            const f = e.target.files?.[0];
            if (f) handleFile(f);
            e.target.value = "";
          }}
        />
      </div>
      <div className="flex flex-col gap-1 text-xs text-text-subtle">
        <span>Arrastra una imagen o haz clic.</span>
        <span>PNG, JPG o WebP.</span>
        {value && (
          <button
            type="button"
            onClick={() => onChange(null)}
            className="inline-flex items-center gap-1 self-start text-text-muted hover:text-danger"
          >
            <X className="h-3 w-3" /> Quitar
          </button>
        )}
      </div>
    </div>
  );
}
