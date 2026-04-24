'use client';

import { createContext, useCallback, useContext, useEffect, useMemo, useState } from 'react';
import { useRouter } from 'next/navigation';
import type { JwtResponse } from './types';
import { getStoredAuth, setStoredAuth, login as apiLogin } from './api';

type AuthState = {
  token: string | null;
  username: string | null;
  roles: string[];
  isAdmin: boolean;
  login: (username: string, password: string) => Promise<void>;
  logout: () => void;
  ready: boolean;
};

const AuthContext = createContext<AuthState | null>(null);

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const [auth, setAuth] = useState<JwtResponse | null>(null);
  const [ready, setReady] = useState(false);

  useEffect(() => {
    setAuth(getStoredAuth());
    setReady(true);
  }, []);

  const login = useCallback(async (username: string, password: string) => {
    const res = await apiLogin(username, password);
    setStoredAuth(res);
    setAuth(res);
  }, []);

  const logout = useCallback(() => {
    setStoredAuth(null);
    setAuth(null);
  }, []);

  const value = useMemo<AuthState>(
    () => ({
      token: auth?.token ?? null,
      username: auth?.username ?? null,
      roles: auth?.roles ?? [],
      isAdmin: (auth?.roles ?? []).includes('ROLE_ADMIN'),
      login,
      logout,
      ready,
    }),
    [auth, login, logout, ready],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth must be used inside AuthProvider');
  return ctx;
}

export function useRequireAuth() {
  const auth = useAuth();
  const router = useRouter();
  useEffect(() => {
    if (auth.ready && !auth.token) router.replace('/login');
  }, [auth.ready, auth.token, router]);
  return auth;
}

export function useRequireAdmin() {
  const auth = useAuth();
  const router = useRouter();
  useEffect(() => {
    if (!auth.ready) return;
    if (!auth.token) router.replace('/login');
    else if (!auth.isAdmin) router.replace('/dashboard');
  }, [auth.ready, auth.token, auth.isAdmin, router]);
  return auth;
}
