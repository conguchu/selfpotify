'use client';

import { useEffect } from 'react';
import { useRouter } from 'next/navigation';
import { useAuth } from '@/lib/auth';

export default function Home() {
  const auth = useAuth();
  const router = useRouter();
  useEffect(() => {
    if (!auth.ready) return;
    router.replace(auth.token ? '/dashboard' : '/login');
  }, [auth.ready, auth.token, router]);
  return <p className="text-neutral-400">Cargando…</p>;
}
