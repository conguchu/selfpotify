'use client';

import { useState } from 'react';
import type { PlaylistDTO } from '@/lib/types';

type Props = {
  playlist: PlaylistDTO;
  onCancel: () => void;
  onSave: (p: PlaylistDTO) => Promise<void> | void;
};

export function PlaylistEditor({ playlist, onCancel, onSave }: Props) {
  const [name, setName] = useState(playlist.name);
  const [description, setDescription] = useState(playlist.description ?? '');
  const [isPublic, setIsPublic] = useState(playlist.isPublic);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);

  async function submit(e: React.FormEvent) {
    e.preventDefault();
    setSaving(true);
    setError(null);
    try {
      await onSave({ ...playlist, name, description, isPublic });
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Error');
    } finally {
      setSaving(false);
    }
  }

  return (
    <div className="fixed inset-0 z-10 flex items-center justify-center bg-black/70 p-4">
      <form
        onSubmit={submit}
        className="w-full max-w-md space-y-3 rounded-lg border border-neutral-800 bg-neutral-900 p-5"
      >
        <h3 className="text-lg font-semibold">
          {playlist.id ? 'Editar playlist' : 'Nueva playlist'}
        </h3>
        <label className="block text-sm">
          <span className="text-neutral-400">Nombre</span>
          <input
            value={name}
            onChange={(e) => setName(e.target.value)}
            className="mt-1 w-full rounded border border-neutral-700 bg-neutral-950 px-2 py-1.5"
            required
          />
        </label>
        <label className="block text-sm">
          <span className="text-neutral-400">Descripción</span>
          <textarea
            value={description}
            onChange={(e) => setDescription(e.target.value)}
            className="mt-1 w-full rounded border border-neutral-700 bg-neutral-950 px-2 py-1.5"
          />
        </label>
        <label className="flex items-center gap-2 text-sm">
          <input
            type="checkbox"
            checked={isPublic}
            onChange={(e) => setIsPublic(e.target.checked)}
          />
          <span>Pública</span>
        </label>
        {error && <p className="text-sm text-red-400">{error}</p>}
        <div className="flex justify-end gap-2">
          <button
            type="button"
            onClick={onCancel}
            className="rounded bg-neutral-800 px-3 py-1.5 text-sm hover:bg-neutral-700"
          >
            Cancelar
          </button>
          <button
            type="submit"
            disabled={saving}
            className="rounded bg-green-500 px-3 py-1.5 text-sm font-medium text-neutral-900 disabled:opacity-50"
          >
            Guardar
          </button>
        </div>
      </form>
    </div>
  );
}
