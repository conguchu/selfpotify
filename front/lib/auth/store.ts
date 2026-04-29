"use client";

import { create } from "zustand";
import { persist, createJSONStorage } from "zustand/middleware";
import type { JwtResponse, Role } from "@/lib/types";

interface AuthState {
  token: string | null;
  username: string | null;
  roles: Role[];
  hydrated: boolean;
  isAdmin: () => boolean;
  isAuthenticated: () => boolean;
  loginWith: (jwt: JwtResponse) => void;
  logout: () => void;
  setHydrated: () => void;
}

export const useAuthStore = create<AuthState>()(
  persist(
    (set, get) => ({
      token: null,
      username: null,
      roles: [],
      hydrated: false,
      isAdmin: () => get().roles.includes("ROLE_ADMIN"),
      isAuthenticated: () => !!get().token,
      loginWith: (jwt) =>
        set({
          token: jwt.token,
          username: jwt.username,
          roles: jwt.roles,
        }),
      logout: () => {
        set({ token: null, username: null, roles: [] });
        if (typeof window !== "undefined") {
          window.location.href = "/login";
        }
      },
      setHydrated: () => set({ hydrated: true }),
    }),
    {
      name: "selfpotify.auth",
      storage: createJSONStorage(() => localStorage),
      partialize: (s) => ({
        token: s.token,
        username: s.username,
        roles: s.roles,
      }),
      onRehydrateStorage: () => (state) => {
        state?.setHydrated();
      },
    },
  ),
);
