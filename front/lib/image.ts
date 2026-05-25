/**
 * Redimensiona/recomprime una imagen en el cliente hasta que pese como máximo
 * `maxBytes`, para no superar el límite de subida del backend. Reduce calidad y,
 * si hace falta, dimensiones, re-codificando a WebP (admite transparencia, igual
 * que PNG, pero pesa mucho menos).
 *
 * Los SVG no se tocan (son vectoriales y ya pesan poco); si el archivo ya cabe,
 * se devuelve tal cual. Si el navegador no puede procesarlo, se devuelve el
 * original (el backend lo rechazará con su 413 si procede).
 */
export async function resizeImageToFit(
  file: File,
  maxBytes: number,
): Promise<File> {
  if (file.type === "image/svg+xml" || file.size <= maxBytes) {
    return file;
  }

  let bitmap: ImageBitmap;
  try {
    bitmap = await createImageBitmap(file);
  } catch {
    return file; // No procesable; que decida el backend.
  }

  let width = bitmap.width;
  let height = bitmap.height;

  // Intenta bajar calidad y, en cada vuelta, también las dimensiones.
  for (let attempt = 0; attempt < 12; attempt++) {
    for (const quality of [0.85, 0.7, 0.55]) {
      const blob = await encode(bitmap, width, height, quality);
      if (blob && blob.size <= maxBytes) {
        bitmap.close();
        return new File([blob], renameToWebp(file.name), {
          type: "image/webp",
        });
      }
    }
    // Reduce ~15% por iteración, sin bajar de 64px de ancho.
    if (width <= 64) break;
    width = Math.max(64, Math.round(width * 0.85));
    height = Math.max(64, Math.round(height * 0.85));
  }

  bitmap.close();
  return file; // No se pudo reducir lo suficiente; que decida el backend.
}

function encode(
  bitmap: ImageBitmap,
  width: number,
  height: number,
  quality: number,
): Promise<Blob | null> {
  const canvas = document.createElement("canvas");
  canvas.width = width;
  canvas.height = height;
  const ctx = canvas.getContext("2d");
  if (!ctx) return Promise.resolve(null);
  ctx.drawImage(bitmap, 0, 0, width, height);
  return new Promise((resolve) =>
    canvas.toBlob((b) => resolve(b), "image/webp", quality),
  );
}

function renameToWebp(name: string): string {
  return name.replace(/\.[^.]+$/, "") + ".webp";
}

/** Formatea bytes a una cadena legible (MB con un decimal, o KB). */
export function formatBytes(bytes: number): string {
  if (bytes >= 1024 * 1024) {
    const mb = bytes / (1024 * 1024);
    return `${Number.isInteger(mb) ? mb : mb.toFixed(1)} MB`;
  }
  return `${Math.round(bytes / 1024)} KB`;
}
