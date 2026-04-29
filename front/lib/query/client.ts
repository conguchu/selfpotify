"use client";

import { QueryClient } from "@tanstack/react-query";

let client: QueryClient | null = null;

export function getQueryClient(): QueryClient {
  if (!client) {
    client = new QueryClient({
      defaultOptions: {
        queries: {
          staleTime: 1000 * 60 * 5,
          retry: (failureCount, error) => {
            const status =
              error && typeof error === "object" && "status" in error
                ? (error as { status: number }).status
                : 0;
            if (status === 401 || status === 403 || status === 404) return false;
            return failureCount < 1;
          },
        },
      },
    });
  }
  return client;
}
