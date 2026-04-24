'use client';

import { useEffect, useMemo, useState } from 'react';
import { getAlbums, getArtists, getSongs } from '@/lib/api';
import type { AlbumDTO, ArtistDTO, SongDTO } from '@/lib/types';
import { AddToPlaylistDialog } from './AddToPlaylistDialog';

function formatDuration(ms: number) {
  if (!ms) return '—';
  const total = Math.floor(ms / 1000);
  const m = Math.floor(total / 60);
  const s = total % 60;
  return `${m}:${s.toString().padStart(2, '0')}`;
}

export function SongsBrowser() {
  const [songs, setSongs] = useState<SongDTO[]>([]);
  const [albums, setAlbums] = useState<AlbumDTO[]>([]);
  const [artists, setArtists] = useState<ArtistDTO[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [adding, setAdding] = useState<SongDTO | null>(null);
  const [q, setQ] = useState('');

  useEffect(() => {
    (async () => {
      try {
        const [s, a, ar] = await Promise.all([getSongs(), getAlbums(), getArtists()]);
        setSongs(s);
        setAlbums(a);
        setArtists(ar);
      } catch (e) {
        setError(e instanceof Error ? e.message : 'Error');
      } finally {
        setLoading(false);
      }
    })();
  }, []);

  const albumMap = useMemo(() => new Map(albums.map((a) => [a.id, a])), [albums]);
  const artistMap = useMemo(() => new Map(artists.map((a) => [a.id, a])), [artists]);

  const filtered = useMemo(
    () =>
      songs.filter((s) => s.title?.toLowerCase().includes(q.toLowerCase())),
    [songs, q],
  );

  if (loading) return <p className="text-neutral-400">Cargando…</p>;
  if (error) return <p className="text-red-400">{error}</p>;

  return (
    <section>
      <div className="mb-4 flex items-center justify-between gap-3">
        <h2 className="text-xl font-semibold">Canciones</h2>
        <input
          value={q}
          onChange={(e) => setQ(e.target.value)}
          placeholder="Buscar…"
          className="rounded border border-neutral-700 bg-neutral-950 px-2 py-1 text-sm"
        />
      </div>
      <div className="overflow-x-auto rounded border border-neutral-800">
        <table className="w-full text-left text-sm">
          <thead className="bg-neutral-900 text-neutral-400">
            <tr>
              <th className="px-3 py-2">Título</th>
              <th className="px-3 py-2">Artista(s)</th>
              <th className="px-3 py-2">Álbum</th>
              <th className="px-3 py-2">Género</th>
              <th className="px-3 py-2">Duración</th>
              <th className="px-3 py-2"></th>
            </tr>
          </thead>
          <tbody>
            {filtered.map((s) => (
              <tr key={s.id} className="border-t border-neutral-800">
                <td className="px-3 py-2 font-medium">{s.title}</td>
                <td className="px-3 py-2 text-neutral-300">
                  {s.artistIds.map((id) => artistMap.get(id)?.name ?? `#${id}`).join(', ') ||
                    '—'}
                </td>
                <td className="px-3 py-2 text-neutral-300">
                  {s.albumId ? albumMap.get(s.albumId)?.name ?? `#${s.albumId}` : '—'}
                </td>
                <td className="px-3 py-2 text-neutral-400">{s.genre ?? '—'}</td>
                <td className="px-3 py-2 text-neutral-400">
                  {formatDuration(s.duration_ms)}
                </td>
                <td className="px-3 py-2">
                  <button
                    onClick={() => setAdding(s)}
                    className="rounded bg-green-500 px-2 py-1 text-xs font-medium text-neutral-900"
                  >
                    + Playlist
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
      {adding && <AddToPlaylistDialog song={adding} onClose={() => setAdding(null)} />}
    </section>
  );
}
