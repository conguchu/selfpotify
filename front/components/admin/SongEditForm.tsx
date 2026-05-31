"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import { Save, User as UserIcon, Check } from "lucide-react";
import { toast } from "sonner";
import { Button } from "@/components/ui/Button";
import { Input } from "@/components/ui/Input";
import { Label } from "@/components/ui/Label";
import { ArtistPickerModal } from "@/components/admin/ArtistPickerModal";
import { CoverDropzone } from "@/components/admin/CoverDropzone";
import { useUpdateSong, useSetSongArtists } from "@/lib/query/hooks";
import type { SongDTO } from "@/lib/types";

/**
 * Edita los metadatos de una canción. No expone ni cambia la ruta física del
 * audio (songPath): el backend la conserva al recibir un PUT sin ese campo. El
 * artista se cambia con un modal de búsqueda/creación y la carátula por
 * drag&drop (se guarda en /assets/covers, como las carátulas normales).
 */
export function SongEditForm({ song }: { song: SongDTO }) {
  const router = useRouter();
  const update = useUpdateSong();
  const setArtists = useSetSongArtists();

  const [title, setTitle] = useState(song.title ?? "");
  const [genre, setGenre] = useState(song.genre ?? "");
  const [bpm, setBpm] = useState(song.bpm ? String(song.bpm) : "");
  const [durationMs, setDurationMs] = useState(
    song.duration_ms ? String(song.duration_ms) : "",
  );
  const [pictureUrl, setPictureUrl] = useState<string | null>(
    song.picture_url ?? null,
  );

  // Artista actual (primero de la lista) y posible cambio.
  const initialArtistId = song.artistIds?.[0] ?? null;
  const initialArtistName = song.artistNames?.[0] ?? "";
  const [artistId, setArtistId] = useState<number | null>(initialArtistId);
  const [artistName, setArtistName] = useState(initialArtistName);
  const [pickerOpen, setPickerOpen] = useState(false);

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
          picture_url: pictureUrl || null,
        },
      });
      // Solo reasignamos artista si cambió respecto al original.
      if (artistId !== initialArtistId) {
        await setArtists.mutateAsync({
          id: song.id,
          artistIds: artistId ? [artistId] : [],
        });
      }
      toast.success("Canción actualizada");
      router.push("/admin/songs");
    } catch (err) {
      toast.error(err instanceof Error ? err.message : "Error al guardar");
    }
  };

  const saving = update.isPending || setArtists.isPending;

  return (
    <form onSubmit={submit} className="flex flex-col gap-4">
      <div className="flex flex-col gap-1.5">
        <Label>Carátula</Label>
        <CoverDropzone value={pictureUrl} onChange={setPictureUrl} />
      </div>

      <div className="grid grid-cols-1 gap-4 md:grid-cols-2">
        <div className="flex flex-col gap-1.5 md:col-span-2">
          <Label htmlFor="se-title">Título *</Label>
          <Input
            id="se-title"
            value={title}
            onChange={(e) => setTitle(e.target.value)}
          />
        </div>

        <div className="flex flex-col gap-1.5 md:col-span-2">
          <Label>Artista</Label>
          <Button
            type="button"
            variant="secondary"
            onClick={() => setPickerOpen(true)}
            leftIcon={<UserIcon className="h-4 w-4" />}
            className="justify-start"
          >
            {artistName || "Sin artista — elegir"}
            {artistId ? <Check className="ml-auto h-4 w-4 text-accent" /> : null}
          </Button>
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
        <div className="flex flex-col gap-1.5 md:col-span-2">
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
      </div>

      <div className="flex items-center gap-3">
        <Button type="submit" leftIcon={<Save className="h-4 w-4" />} loading={saving}>
          Guardar cambios
        </Button>
        <Button
          type="button"
          variant="ghost"
          onClick={() => router.push("/admin/songs")}
          disabled={saving}
        >
          Cancelar
        </Button>
      </div>

      <ArtistPickerModal
        open={pickerOpen}
        onClose={() => setPickerOpen(false)}
        currentArtistId={artistId}
        onSelect={(a) => {
          setArtistId(a.id);
          setArtistName(a.name);
        }}
      />
    </form>
  );
}
