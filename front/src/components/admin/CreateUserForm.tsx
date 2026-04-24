'use client';

import { useState } from 'react';
import { createUser } from '@/lib/api';

export function CreateUserForm({ onCreated }: { onCreated: () => void }) {
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [isAdmin, setIsAdmin] = useState(false);
  const [status, setStatus] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [saving, setSaving] = useState(false);

  async function submit(e: React.FormEvent) {
    e.preventDefault();
    setError(null);
    setStatus(null);
    setSaving(true);
    try {
      await createUser({ username, password, isAdmin });
      setStatus(`Creado ${isAdmin ? 'admin' : 'usuario'} "${username}".`);
      setUsername('');
      setPassword('');
      setIsAdmin(false);
      onCreated();
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Error');
    } finally {
      setSaving(false);
    }
  }

  return (
    <section className="rounded-lg border border-neutral-800 bg-neutral-900 p-4">
      <h2 className="mb-3 text-lg font-semibold">Crear usuario</h2>
      <form onSubmit={submit} className="flex flex-wrap items-end gap-3">
        <label className="text-sm">
          <span className="block text-neutral-400">Username</span>
          <input
            value={username}
            onChange={(e) => setUsername(e.target.value)}
            className="mt-1 rounded border border-neutral-700 bg-neutral-950 px-2 py-1.5"
            required
          />
        </label>
        <label className="text-sm">
          <span className="block text-neutral-400">Password</span>
          <input
            type="password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            className="mt-1 rounded border border-neutral-700 bg-neutral-950 px-2 py-1.5"
            required
          />
        </label>
        <label className="flex items-center gap-2 text-sm">
          <input
            type="checkbox"
            checked={isAdmin}
            onChange={(e) => setIsAdmin(e.target.checked)}
          />
          <span>Es admin</span>
        </label>
        <button
          type="submit"
          disabled={saving}
          className="rounded bg-green-500 px-3 py-1.5 text-sm font-medium text-neutral-900 disabled:opacity-50"
        >
          Crear
        </button>
      </form>
      {status && <p className="mt-2 text-sm text-green-400">{status}</p>}
      {error && <p className="mt-2 text-sm text-red-400">{error}</p>}
    </section>
  );
}
