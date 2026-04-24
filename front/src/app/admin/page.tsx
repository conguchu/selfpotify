'use client';

import { useRequireAdmin } from '@/lib/auth';
import { UsersAdmin } from '@/components/admin/UsersAdmin';

export default function AdminPage() {
  const auth = useRequireAdmin();
  if (!auth.ready || !auth.isAdmin) return <p className="text-neutral-400">Cargando…</p>;
  return (
    <div>
      <h1 className="mb-4 text-2xl font-bold">Administración</h1>
      <UsersAdmin />
    </div>
  );
}
