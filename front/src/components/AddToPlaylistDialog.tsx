'use client';

import { useEffect, useState } from 'react';
import { getMyPlaylists, getPlaylist, updatePlaylist } from '@/lib/api';
import type { PlaylistDTO, SongDTO } from '@/lib/types';

type Props = {
  song: SongDTO;
  onClose: () => void;
};

export function AddToPlaylistDialog({ song, onClose }: Props) {
  const [playlists, setPlaylists] = useState<PlaylistDTO[]>([]);
  const [loading, setLoading] = useState(true);
  const [status, setStatus] = useState<string | null>(null);

  useEffect(() => {
    getMyPlaylists()
      .then(setPlaylists)
      .finally(() => setLoading(false));
  }, []);

  async function addTo(p: PlaylistDTO) {
    if (!p.id) return;
    setStatus('Guardando…');
    try {
      const full = await getPlaylist(p.id);
      if (full.songIds.includes(song.id)) {
        setStatus('La canción ya está en esta playlist.');
        return;
      }
      await updatePlaylist(p.id, {
        ...full,
        songIds: [...full.songIds, song.id],
      });
      setStatus(`Añadida a "${p.name}".`);
    } catch (e) {
      setStatus(e instanceof Error ? e.message : 'Error');
    }
  }

  return (
    <div className="fixed inset-0 z-10 flex items-center justify-center bg-black/70 p-4">
      <div className="w-full max-w-md rounded-lg border border-neutral-800 bg-neutral-900 p-5">
        <div className="mb-3 flex items-center justify-between">
          <h3 className="text-lg font-semibold">Añadir a playlist</h3>
          <button onClick={onClose} className="text-neutral-400 hover:text-neutral-200">
            ✕
          </button>
        </div>
        <p className="mb-3 text-sm text-neutral-400">
          Canción: <span className="text-neutral-200">{song.title}</span>
        </p>
        {loading ? (
          <p className="text-neutral-400">Cargando…</p>
        ) : playlists.length === 0 ? (
          <p className="text-neutral-400">
            No tienes playlists. Crea una desde la pestaña "Mis playlists".
          </p>
        ) : (
          <ul className="space-y-1">
            {playlists.map((p) => (
              <li key={p.id}>
                <button
                  onClick={() => addTo(p)}
                  className="w-full rounded border border-neutral-800 bg-neutral-950 px-3 py-2 text-left text-sm hover:bg-neutral-800"
                >
                  {p.name}
                  <span className="ml-2 text-xs text-neutral-500">
                    {p.isPublic ? 'pública' : 'privada'}
                  </span>
                </button>
              </li>
            ))}
          </ul>
        )}
        {status && <p className="mt-3 text-sm text-green-400">{status}</p>}
      </div>
    </div>
  );
}
