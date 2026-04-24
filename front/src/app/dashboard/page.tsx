'use client';

import { useState } from 'react';
import { useRequireAuth } from '@/lib/auth';
import { MyPlaylists } from '@/components/MyPlaylists';
import { SongsBrowser } from '@/components/SongsBrowser';
import { UsersExplorer } from '@/components/UsersExplorer';

type Tab = 'playlists' | 'songs' | 'users';

export default function DashboardPage() {
  const auth = useRequireAuth();
  const [tab, setTab] = useState<Tab>('playlists');

  if (!auth.ready || !auth.token) return <p className="text-neutral-400">Cargando…</p>;

  const tabs: { id: Tab; label: string }[] = [
    { id: 'playlists', label: 'Mis playlists' },
    { id: 'songs', label: 'Canciones' },
    { id: 'users', label: 'Usuarios' },
  ];

  return (
    <div>
      <div className="mb-6 flex gap-2 border-b border-neutral-800">
        {tabs.map((t) => (
          <button
            key={t.id}
            onClick={() => setTab(t.id)}
            className={`px-4 py-2 text-sm ${
              tab === t.id
                ? 'border-b-2 border-green-400 text-green-400'
                : 'text-neutral-400 hover:text-neutral-200'
            }`}
          >
            {t.label}
          </button>
        ))}
      </div>
      {tab === 'playlists' && <MyPlaylists />}
      {tab === 'songs' && <SongsBrowser />}
      {tab === 'users' && <UsersExplorer />}
    </div>
  );
}
