"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import { Save } from "lucide-react";
import { toast } from "sonner";
import { Button } from "@/components/ui/Button";
import { Input } from "@/components/ui/Input";
import { Label } from "@/components/ui/Label";
import { useUpdateSong } from "@/lib/query/hooks";
import type { SongDTO } from "@/lib/types";

/**
 * Edita los metadatos de una canción. No expone ni cambia la ruta física del
 * audio (songPath): el backend la conserva al recibir un PUT sin ese campo.
 */
export function SongEditForm({ song }: { song: SongDTO }) {
  const router = useRouter();
  const update = useUpdateSong();

  const [title, setTitle] = useState(song.title ?? "");
  const [genre, setGenre] = useState(song.genre ?? "");
  const [bpm, setBpm] = useState(song.bpm ? String(song.bpm) : "");
  const [durationMs, setDurationMs] = useState(
    song.duration_ms ? String(song.duration_ms) : "",
  );
  const [pictureUrl, setPictureUrl] = useState(song.picture_url ?? "");

  const submit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!title.trim()) {
      toast.error("El título es obligatorio");
      return;
    }
    try {
      await update.mutateAsync({
        id: song.id,
        payload: {
          title: title.trim(),
          genre: genre.trim() || null,
          bpm: bpm ? Number(bpm) : 0,
          duration_ms: durationMs ? Number(durationMs) : 0,
          picture_url: pictureUrl.trim() || null,
        },
      });
      toast.success("Canción actualizada");
      router.push("/admin/songs");
    } catch (err) {
      toast.error(err instanceof Error ? err.message : "Error al guardar");
    }
  };

  return (
    <form onSubmit={submit} className="grid grid-cols-1 gap-4 md:grid-cols-2">
      <div className="flex flex-col gap-1.5 md:col-span-2">
        <Label htmlFor="se-title">Título *</Label>
        <Input
          id="se-title"
          value={title}
          onChange={(e) => setTitle(e.target.value)}
        />
      </div>
      <div className="flex flex-col gap-1.5">
        <Label htmlFor="se-genre">Género</Label>
        <Input
          id="se-genre"
          value={genre}
          onChange={(e) => setGenre(e.target.value)}
        />
      </div>
      <div className="flex flex-col gap-1.5">
        <Label htmlFor="se-bpm">BPM</Label>
        <Input
          id="se-bpm"
          type="number"
          inputMode="numeric"
          min={0}
          value={bpm}
          onChange={(e) => setBpm(e.target.value)}
        />
      </div>
      <div className="flex flex-col gap-1.5">
        <Label htmlFor="se-dur">Duración (ms)</Label>
        <Input
          id="se-dur"
          type="number"
          inputMode="numeric"
          min={0}
          value={durationMs}
          onChange={(e) => setDurationMs(e.target.value)}
        />
      </div>
      <div className="flex flex-col gap-1.5">
        <Label htmlFor="se-pic">URL carátula</Label>
        <Input
          id="se-pic"
          value={pictureUrl}
          onChange={(e) => setPictureUrl(e.target.value)}
          placeholder="https://..."
        />
      </div>

      <div className="flex items-center gap-3 md:col-span-2">
        <Button
          type="submit"
          leftIcon={<Save className="h-4 w-4" />}
          loading={update.isPending}
        >
          Guardar cambios
        </Button>
        <Button
          type="button"
          variant="ghost"
          onClick={() => router.push("/admin/songs")}
          disabled={update.isPending}
        >
          Cancelar
        </Button>
      </div>
    </form>
  );
}
