"use client";

import { Suspense } from "react";
import { SearchPageContent } from "@/components/search/SearchPageContent";
import { Spinner } from "@/components/ui/Spinner";

/**
 * Página dedicada de resultados de búsqueda (`/search?q=...&type=...&page=...`).
 *
 * `useSearchParams` requiere un Suspense boundary porque obliga al subárbol a
 * renderizarse en cliente; envolvemos el contenido para que el frame del
 * AppShell pueda prerrenderizarse sin esperar al query string.
 */
export default function SearchPage() {
  return (
    <Suspense
      fallback={
        <div className="flex h-64 items-center justify-center">
          <Spinner size="lg" />
        </div>
      }
    >
      <SearchPageContent />
    </Suspense>
  );
}
