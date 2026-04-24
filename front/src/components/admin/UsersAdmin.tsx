'use client';

import { useEffect, useState } from 'react';
import { deleteUser, getUsers, updateUser } from '@/lib/api';
import type { User } from '@/lib/types';
import { CreateUserForm } from './CreateUserForm';

export function UsersAdmin() {
  const [users, setUsers] = useState<User[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [editingId, setEditingId] = useState<number | null>(null);
  const [editUsername, setEditUsername] = useState('');
  const [editPassword, setEditPassword] = useState('');

  async function refresh() {
    setLoading(true);
    try {
      setUsers(await getUsers());
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Error');
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    refresh();
  }, []);

  function startEdit(u: User) {
    setEditingId(u.id);
    setEditUsername(u.username);
    setEditPassword('');
  }

  async function saveEdit(id: number) {
    const patch: Record<string, unknown> = { id, username: editUsername };
    if (editPassword) patch.password = editPassword;
    await updateUser(id, patch as never);
    setEditingId(null);
    refresh();
  }

  async function remove(id: number) {
    if (!confirm('¿Eliminar usuario?')) return;
    await deleteUser(id);
    refresh();
  }

  return (
    <div className="space-y-6">
      <CreateUserForm onCreated={refresh} />

      <section>
        <h2 className="mb-3 text-lg font-semibold">Usuarios</h2>
        {error && <p className="text-red-400">{error}</p>}
        {loading ? (
          <p className="text-neutral-400">Cargando…</p>
        ) : (
          <div className="overflow-x-auto rounded border border-neutral-800">
            <table className="w-full text-left text-sm">
              <thead className="bg-neutral-900 text-neutral-400">
                <tr>
                  <th className="px-3 py-2">ID</th>
                  <th className="px-3 py-2">Username</th>
                  <th className="px-3 py-2"></th>
                </tr>
              </thead>
              <tbody>
                {users.map((u) => (
                  <tr key={u.id} className="border-t border-neutral-800">
                    <td className="px-3 py-2 text-neutral-400">{u.id}</td>
                    <td className="px-3 py-2">
                      {editingId === u.id ? (
                        <div className="flex flex-wrap gap-2">
                          <input
                            value={editUsername}
                            onChange={(e) => setEditUsername(e.target.value)}
                            className="rounded border border-neutral-700 bg-neutral-950 px-2 py-1"
                          />
                          <input
                            type="password"
                            placeholder="nueva password (opcional)"
                            value={editPassword}
                            onChange={(e) => setEditPassword(e.target.value)}
                            className="rounded border border-neutral-700 bg-neutral-950 px-2 py-1"
                          />
                        </div>
                      ) : (
                        u.username
                      )}
                    </td>
                    <td className="px-3 py-2">
                      <div className="flex gap-2">
                        {editingId === u.id ? (
                          <>
                            <button
                              onClick={() => saveEdit(u.id)}
                              className="rounded bg-green-500 px-2 py-1 text-xs font-medium text-neutral-900"
                            >
                              Guardar
                            </button>
                            <button
                              onClick={() => setEditingId(null)}
                              className="rounded bg-neutral-800 px-2 py-1 text-xs"
                            >
                              Cancelar
                            </button>
                          </>
                        ) : (
                          <>
                            <button
                              onClick={() => startEdit(u)}
                              className="rounded bg-neutral-800 px-2 py-1 text-xs hover:bg-neutral-700"
                            >
                              Editar
                            </button>
                            <button
                              onClick={() => remove(u.id)}
                              className="rounded bg-red-900 px-2 py-1 text-xs hover:bg-red-800"
                            >
                              Borrar
                            </button>
                          </>
                        )}
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </section>
    </div>
  );
}
