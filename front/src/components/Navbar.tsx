'use client';

import Link from 'next/link';
import { useRouter } from 'next/navigation';
import { useAuth } from '@/lib/auth';

export function Navbar() {
  const auth = useAuth();
  const router = useRouter();

  function handleLogout() {
    auth.logout();
    router.replace('/login');
  }

  return (
    <nav className="border-b border-neutral-800 bg-neutral-900">
      <div className="mx-auto flex max-w-6xl items-center justify-between px-4 py-3">
        <Link href="/" className="text-lg font-bold text-green-400">
          Selfpotify
        </Link>
        <div className="flex items-center gap-4 text-sm">
          {auth.token ? (
            <>
              <Link href="/dashboard" className="hover:text-green-400">
                Dashboard
              </Link>
              {auth.isAdmin && (
                <Link href="/admin" className="hover:text-green-400">
                  Admin
                </Link>
              )}
              <span className="text-neutral-400">
                {auth.username}
                {auth.isAdmin ? ' (admin)' : ''}
              </span>
              <button
                onClick={handleLogout}
                className="rounded bg-neutral-800 px-3 py-1 hover:bg-neutral-700"
              >
                Salir
              </button>
            </>
          ) : (
            <Link href="/login" className="hover:text-green-400">
              Login
            </Link>
          )}
        </div>
      </div>
    </nav>
  );
}
