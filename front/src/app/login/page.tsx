'use client';

import { useState } from 'react';
import { useRouter } from 'next/navigation';
import { useAuth } from '@/lib/auth';
import { signup } from '@/lib/api';

export default function LoginPage() {
  const auth = useAuth();
  const router = useRouter();
  const [mode, setMode] = useState<'login' | 'signup'>('login');
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  async function onSubmit(e: React.FormEvent) {
    e.preventDefault();
    setError(null);
    setLoading(true);
    try {
      if (mode === 'signup') {
        await signup(username, password);
      }
      await auth.login(username, password);
      router.replace('/dashboard');
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Error');
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="mx-auto mt-12 max-w-sm rounded-lg border border-neutral-800 bg-neutral-900 p-6">
      <div className="mb-4 flex gap-2">
        <button
          onClick={() => setMode('login')}
          className={`flex-1 rounded px-3 py-1.5 text-sm ${
            mode === 'login' ? 'bg-green-500 text-neutral-900' : 'bg-neutral-800'
          }`}
        >
          Login
        </button>
        <button
          onClick={() => setMode('signup')}
          className={`flex-1 rounded px-3 py-1.5 text-sm ${
            mode === 'signup' ? 'bg-green-500 text-neutral-900' : 'bg-neutral-800'
          }`}
        >
          Signup
        </button>
      </div>
      <form onSubmit={onSubmit} className="space-y-3">
        <label className="block text-sm">
          <span className="text-neutral-400">Usuario</span>
          <input
            value={username}
            onChange={(e) => setUsername(e.target.value)}
            className="mt-1 w-full rounded border border-neutral-700 bg-neutral-950 px-2 py-1.5"
            required
          />
        </label>
        <label className="block text-sm">
          <span className="text-neutral-400">Contraseña</span>
          <input
            type="password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            className="mt-1 w-full rounded border border-neutral-700 bg-neutral-950 px-2 py-1.5"
            required
          />
        </label>
        {error && <p className="text-sm text-red-400">{error}</p>}
        <button
          type="submit"
          disabled={loading}
          className="w-full rounded bg-green-500 px-3 py-2 font-medium text-neutral-900 disabled:opacity-50"
        >
          {loading ? '…' : mode === 'login' ? 'Entrar' : 'Crear cuenta'}
        </button>
      </form>
    </div>
  );
}
