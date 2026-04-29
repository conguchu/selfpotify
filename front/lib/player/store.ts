"use client";

import { create } from "zustand";
import type { SongDTO } from "@/lib/types";

interface PlayerState {
  current: SongDTO | null;
  queue: SongDTO[];
  isPlaying: boolean;
  volume: number;
  positionMs: number;
  durationMs: number;

  playSong: (song: SongDTO, queue?: SongDTO[]) => void;
  togglePlay: () => void;
  pause: () => void;
  resume: () => void;
  next: () => void;
  prev: () => void;
  setVolume: (v: number) => void;
  setPosition: (ms: number) => void;
  setDuration: (ms: number) => void;
  stop: () => void;
}

export const usePlayerStore = create<PlayerState>((set, get) => ({
  current: null,
  queue: [],
  isPlaying: false,
  volume: 0.8,
  positionMs: 0,
  durationMs: 0,

  playSong: (song, queue) =>
    set({
      current: song,
      queue: queue ?? [song],
      isPlaying: true,
      positionMs: 0,
    }),
  togglePlay: () => set((s) => ({ isPlaying: !s.isPlaying })),
  pause: () => set({ isPlaying: false }),
  resume: () => set({ isPlaying: true }),
  next: () => {
    const { current, queue } = get();
    if (!current) return;
    const idx = queue.findIndex((s) => s.id === current.id);
    if (idx === -1 || idx === queue.length - 1) return;
    set({ current: queue[idx + 1], positionMs: 0, isPlaying: true });
  },
  prev: () => {
    const { current, queue, positionMs } = get();
    if (!current) return;
    if (positionMs > 3000) {
      set({ positionMs: 0 });
      return;
    }
    const idx = queue.findIndex((s) => s.id === current.id);
    if (idx <= 0) {
      set({ positionMs: 0 });
      return;
    }
    set({ current: queue[idx - 1], positionMs: 0, isPlaying: true });
  },
  setVolume: (v) => set({ volume: Math.max(0, Math.min(1, v)) }),
  setPosition: (ms) => set({ positionMs: ms }),
  setDuration: (ms) => set({ durationMs: ms }),
  stop: () => set({ current: null, isPlaying: false, positionMs: 0 }),
}));
