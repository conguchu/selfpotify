'use client';

import { useEffect, useState } from 'react';
import {
  createPlaylist,
  deletePlaylist,
  getMyPlaylists,
  getPlaylist,
  updatePlaylist,
} from '@/lib/api';
import type { PlaylistDTO } from '@/lib/types';
import { PlaylistEditor } from './PlaylistEditor';

export function MyPlaylists() {
  const [list, setList] = useState<PlaylistDTO[]>([]);
  const [loading, setLoading] = useState(true);
  const [editing, setEditing] = useState<PlaylistDTO | null>(null);
  const [expanded, setExpanded] = useState<Record<number, PlaylistDTO>>({});
  const [error, setError] = useState<string | null>(null);

  async function refresh() {
    setLoading(true);
    try {
      setList(await getMyPlaylists());
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Error');
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    refresh();
  }, []);

  async function toggle(p: PlaylistDTO) {
    if (!p.id) return;
    if (expanded[p.id]) {
      const next = { ...expanded };
      delete next[p.id];
      setExpanded(next);
      return;
    }
    const full = await getPlaylist(p.id);
    setExpanded({ ...expanded, [p.id]: full });
  }

  async function save(p: PlaylistDTO) {
    if (p.id) await updatePlaylist(p.id, p);
    else await createPlaylist(p);
    setEditing(null);
    refresh();
  }

  async function remove(id: number) {
    if (!confirm('¿Borrar playlist?')) return;
    await deletePlaylist(id);
    refresh();
  }

  return (
    <section>
      <div className="mb-4 flex items-center justify-between">
        <h2 className="text-xl font-semibold">Mis playlists</h2>
        <button
          onClick={() =>
            setEditing({ name: '', description: '', isPublic: false, songIds: [] })
          }
          className="rounded bg-green-500 px-3 py-1.5 text-sm font-medium text-neutral-900"
        >
          + Nueva
        </button>
      </div>
      {error && <p className="text-red-400">{error}</p>}
      {loading ? (
        <p className="text-neutral-400">Cargando…</p>
      ) : list.length === 0 ? (
        <p className="text-neutral-400">Aún no tienes playlists.</p>
      ) : (
        <ul className="space-y-2">
          {list.map((p) => (
            <li key={p.id} className="rounded border border-neutral-800 bg-neutral-900 p-3">
              <div className="flex items-center justify-between">
                <div>
                  <button onClick={() => toggle(p)} className="text-left">
                    <span className="font-medium">{p.name}</span>
                    <span className="ml-2 text-xs text-neutral-400">
                      {p.isPublic ? 'pública' : 'privada'}
                    </span>
                  </button>
                  {p.description && (
                    <p className="text-sm text-neutral-400">{p.description}</p>
                  )}
                </div>
                <div className="flex gap-2 text-sm">
                  <button
                    onClick={() => setEditing(p)}
                    className="rounded bg-neutral-800 px-2 py-1 hover:bg-neutral-700"
                  >
                    Editar
                  </button>
                  <button
                    onClick={() => p.id && remove(p.id)}
                    className="rounded bg-red-900 px-2 py-1 hover:bg-red-800"
                  >
                    Borrar
                  </button>
                </div>
              </div>
              {p.id && expanded[p.id] && (
                <div className="mt-2 text-sm text-neutral-300">
                  <span className="text-neutral-500">Canciones: </span>
                  {expanded[p.id].songIds.length === 0
                    ? '—'
                    : expanded[p.id].songIds.join(', ')}
                </div>
              )}
            </li>
          ))}
        </ul>
      )}
      {editing && (
        <PlaylistEditor
          playlist={editing}
          onCancel={() => setEditing(null)}
          onSave={save}
        />
      )}
    </section>
  );
}
