"use client";

import { useState } from "react";
import { Music } from "lucide-react";
import { toast } from "sonner";
import { Button } from "@/components/ui/Button";
import { Input } from "@/components/ui/Input";
import { Label } from "@/components/ui/Label";
import { useCreateSong } from "@/lib/query/hooks";

export function CreateSongForm() {
  const [title, setTitle] = useState("");
  const [songPath, setSongPath] = useState("");
  const [genre, setGenre] = useState("");
  const [bpm, setBpm] = useState("");
  const [durationMs, setDurationMs] = useState("");
  const [pictureUrl, setPictureUrl] = useState("");
  const create = useCreateSong();

  const submit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!title.trim() || !songPath.trim()) {
      toast.error("Título y ruta son obligatorios");
      return;
    }
    try {
      await create.mutateAsync({
        title: title.trim(),
        songPath: songPath.trim(),
        genre: genre.trim() || undefined,
        bpm: bpm ? Number(bpm) : undefined,
        duration_ms: durationMs ? Number(durationMs) : undefined,
        picture_url: pictureUrl.trim() || undefined,
        available: true,
      });
      toast.success("Canción creada");
      setTitle("");
      setSongPath("");
      setGenre("");
      setBpm("");
      setDurationMs("");
      setPictureUrl("");
    } catch (err) {
      toast.error(err instanceof Error ? err.message : "Error al crear");
    }
  };

  return (
    <form onSubmit={submit} className="grid grid-cols-1 gap-4 md:grid-cols-2">
      <div className="flex flex-col gap-1.5 md:col-span-2">
        <Label htmlFor="cs-title">Título *</Label>
        <Input
          id="cs-title"
          value={title}
          onChange={(e) => setTitle(e.target.value)}
        />
      </div>
      <div className="flex flex-col gap-1.5 md:col-span-2">
        <Label htmlFor="cs-path">Ruta absoluta del archivo *</Label>
        <Input
          id="cs-path"
          value={songPath}
          onChange={(e) => setSongPath(e.target.value)}
          placeholder="/Users/yo/Música/cancion.mp3"
        />
      </div>
      <div className="flex flex-col gap-1.5">
        <Label htmlFor="cs-genre">Género</Label>
        <Input
          id="cs-genre"
          value={genre}
          onChange={(e) => setGenre(e.target.value)}
        />
      </div>
      <div className="flex flex-col gap-1.5">
        <Label htmlFor="cs-bpm">BPM</Label>
        <Input
          id="cs-bpm"
          type="number"
          inputMode="numeric"
          min={0}
          value={bpm}
          onChange={(e) => setBpm(e.target.value)}
        />
      </div>
      <div className="flex flex-col gap-1.5">
        <Label htmlFor="cs-dur">Duración (ms)</Label>
        <Input
          id="cs-dur"
          type="number"
          inputMode="numeric"
          min={0}
          value={durationMs}
          onChange={(e) => setDurationMs(e.target.value)}
        />
      </div>
      <div className="flex flex-col gap-1.5">
        <Label htmlFor="cs-pic">URL carátula</Label>
        <Input
          id="cs-pic"
          value={pictureUrl}
          onChange={(e) => setPictureUrl(e.target.value)}
          placeholder="https://..."
        />
      </div>
      <Button
        type="submit"
        leftIcon={<Music className="h-4 w-4" />}
        loading={create.isPending}
        className="self-start md:col-span-2"
      >
        Crear canción
      </Button>
    </form>
  );
}
