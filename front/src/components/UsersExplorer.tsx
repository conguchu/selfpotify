'use client';

import { useEffect, useState } from 'react';
import { getUserPlaylists, getUsers } from '@/lib/api';
import { useAuth } from '@/lib/auth';
import type { PlaylistDTO, User } from '@/lib/types';

export function UsersExplorer() {
  const auth = useAuth();
  const [users, setUsers] = useState<User[] | null>(null);
  const [userIdInput, setUserIdInput] = useState('');
  const [selected, setSelected] = useState<{ id: number; name?: string } | null>(null);
  const [playlists, setPlaylists] = useState<PlaylistDTO[]>([]);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!auth.isAdmin) return;
    getUsers()
      .then(setUsers)
      .catch(() => setUsers(null));
  }, [auth.isAdmin]);

  async function openUser(id: number, name?: string) {
    setSelected({ id, name });
    setError(null);
    setPlaylists([]);
    try {
      setPlaylists(await getUserPlaylists(id));
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Error');
    }
  }

  return (
    <section>
      <h2 className="mb-4 text-xl font-semibold">Otros usuarios</h2>

      {users ? (
        <div className="mb-4 flex flex-wrap gap-2">
          {users.map((u) => (
            <button
              key={u.id}
              onClick={() => openUser(u.id, u.username)}
              className="rounded border border-neutral-800 bg-neutral-900 px-3 py-1.5 text-sm hover:border-green-500"
            >
              {u.username}{' '}
              <span className="text-xs text-neutral-500">#{u.id}</span>
            </button>
          ))}
        </div>
      ) : (
        <form
          onSubmit={(e) => {
            e.preventDefault();
            const id = Number(userIdInput);
            if (!Number.isFinite(id) || id <= 0) return;
            openUser(id);
          }}
          className="mb-4 flex gap-2"
        >
          <input
            type="number"
            min={1}
            placeholder="ID de usuario"
            value={userIdInput}
            onChange={(e) => setUserIdInput(e.target.value)}
            className="rounded border border-neutral-700 bg-neutral-950 px-2 py-1 text-sm"
          />
          <button className="rounded bg-green-500 px-3 py-1 text-sm font-medium text-neutral-900">
            Ver playlists públicas
          </button>
          <p className="self-center text-xs text-neutral-500">
            (El listado completo de usuarios requiere permisos de admin.)
          </p>
        </form>
      )}

      {selected && (
        <div>
          <h3 className="mb-2 text-lg font-medium">
            Playlists públicas de {selected.name ?? `usuario #${selected.id}`}
          </h3>
          {error && <p className="text-red-400">{error}</p>}
          {playlists.length === 0 && !error ? (
            <p className="text-neutral-400">No hay playlists públicas.</p>
          ) : (
            <ul className="space-y-2">
              {playlists.map((p) => (
                <li
                  key={p.id}
                  className="rounded border border-neutral-800 bg-neutral-900 p-3"
                >
                  <div className="font-medium">{p.name}</div>
                  {p.description && (
                    <div className="text-sm text-neutral-400">{p.description}</div>
                  )}
                  <div className="mt-1 text-xs text-neutral-500">
                    {p.songIds.length} canciones
                  </div>
                </li>
              ))}
            </ul>
          )}
        </div>
      )}
    </section>
  );
}
