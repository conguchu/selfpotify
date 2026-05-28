"use client";

import * as React from "react";
import { useRouter, useSearchParams } from "next/navigation";
import { Search as SearchIcon } from "lucide-react";
import { Spinner } from "@/components/ui/Spinner";
import { EmptyState } from "@/components/ui/EmptyState";
import { Button } from "@/components/ui/Button";
import { ArtistCard } from "@/components/music/ArtistCard";
import { AlbumCard } from "@/components/music/AlbumCard";
import { PlaylistCard } from "@/components/music/PlaylistCard";
import { GenreCard } from "@/components/music/GenreCard";
import { UserCard } from "@/components/music/UserCard";
import { SongRow } from "@/components/music/SongRow";
import { useSearch } from "@/lib/query/hooks";
import { usePlayerStore } from "@/lib/player/store";
import type { SearchType, SongDTO } from "@/lib/types";
import { cn } from "@/lib/utils";

/** Tamaño de página para el modo específico. Igual al default del backend. */
const PAGE_SIZE = 20;

/**
 * Pestañas y los valores que se mandan al backend. Se mapean 1-a-1 con
 * {@link SearchType}; "all" rellena las 6 categorías recortadas.
 */
const TABS: { value: SearchType; label: string }[] = [
  { value: "all", label: "Todo" },
  { value: "songs", label: "Canciones" },
  { value: "artists", label: "Artistas" },
  { value: "albums", label: "Álbumes" },
  { value: "playlists", label: "Playlists" },
  { value: "users", label: "Usuarios" },
  { value: "genres", label: "Géneros" },
];

/**
 * Contenido de la página `/search`. Vive como componente cliente separado
 * porque {@code useSearchParams} obliga a un Suspense boundary en su árbol.
 *
 * Estado en la URL (single source of truth):
 *  - `q`: la consulta. Sin ella, la página muestra un empty state.
 *  - `type`: la pestaña activa. Default `all`.
 *  - `page`: la página (0-based) del modo específico. Default `0`. Se ignora
 *    en el modo `all`.
 *
 * Cambiar de pestaña, paginar o buscar reescribe la URL con {@code router.replace},
 * de modo que el historial del navegador no se llena de estados intermedios.
 */
export function SearchPageContent() {
  const router = useRouter();
  const params = useSearchParams();
  const q = (params.get("q") ?? "").trim();
  const typeParam = params.get("type") as SearchType | null;
  const type: SearchType =
    typeParam && TABS.some((t) => t.value === typeParam) ? typeParam : "all";
  const page = Math.max(0, Number(params.get("page") ?? "0") || 0);

  const query = useSearch(q, type, page, PAGE_SIZE);
  const data = query.data;

  const setTab = (next: SearchType) => {
    const sp = new URLSearchParams();
    sp.set("q", q);
    if (next !== "all") sp.set("type", next);
    // Cambiar de pestaña reinicia la paginación.
    router.replace(`/search?${sp.toString()}`);
  };

  const setPage = (next: number) => {
    const sp = new URLSearchParams();
    sp.set("q", q);
    sp.set("type", type);
    sp.set("page", String(Math.max(0, next)));
    router.replace(`/search?${sp.toString()}`);
  };

  // Sin query: pantalla en blanco con CTA para volver a empezar.
  if (!q) {
    return (
      <div className="flex flex-col gap-6">
        <h1 className="text-3xl font-bold tracking-tight">Buscar</h1>
        <EmptyState
          icon={<SearchIcon />}
          title="Empieza a buscar"
          description="Usa la barra de arriba para encontrar canciones, artistas, álbumes, playlists, usuarios o géneros."
        />
      </div>
    );
  }

  // Total combinado para el badge de "Todo": suma de las 6 categorías cuando
  // estamos en modo all y el backend ha respondido.
  const allTotal =
    type === "all" && data
      ? (data.songs?.totalElements ?? 0) +
        (data.artists?.totalElements ?? 0) +
        (data.albums?.totalElements ?? 0) +
        (data.playlists?.totalElements ?? 0) +
        (data.users?.totalElements ?? 0) +
        (data.genres?.totalElements ?? 0)
      : 0;

  return (
    <div className="flex flex-col gap-6">
      <header className="flex flex-col gap-1">
        <p className="text-sm text-text-muted">Resultados para</p>
        <h1
          className="truncate text-3xl font-bold tracking-tight"
          title={q}
        >
          "{q}"
        </h1>
      </header>

      <nav
        role="tablist"
        aria-label="Categorías de búsqueda"
        className="flex flex-wrap gap-1 rounded-md border border-border bg-bg-card p-1"
      >
        {TABS.map((t) => {
          const active = t.value === type;
          return (
            <button
              key={t.value}
              type="button"
              role="tab"
              aria-selected={active}
              onClick={() => setTab(t.value)}
              className={cn(
                "rounded px-3 py-1.5 text-sm font-medium transition-colors",
                active
                  ? "bg-accent text-on-accent"
                  : "text-text-muted hover:bg-bg-hover hover:text-text",
              )}
            >
              {t.label}
            </button>
          );
        })}
      </nav>

      {query.isLoading ? (
        <div className="flex h-64 items-center justify-center">
          <Spinner size="lg" />
        </div>
      ) : query.isError ? (
        <p className="text-sm text-danger">
          Error al buscar:{" "}
          {query.error instanceof Error ? query.error.message : "?"}
        </p>
      ) : !data ? null : type === "all" ? (
        <AllResults data={data} q={q} totalCombined={allTotal} setTab={setTab} />
      ) : (
        <SpecificResults
          type={type}
          data={data}
          page={page}
          onPage={setPage}
          q={q}
        />
      )}
    </div>
  );
}

// =====================================
// ----- Modo "all" — secciones recortadas con "Ver todo"
// =====================================

function AllResults({
  data,
  q,
  totalCombined,
  setTab,
}: {
  data: NonNullable<ReturnType<typeof useSearch>["data"]>;
  q: string;
  totalCombined: number;
  setTab: (t: SearchType) => void;
}) {
  if (totalCombined === 0) {
    return (
      <EmptyState
        icon={<SearchIcon />}
        title={`Sin resultados para "${q}"`}
        description="Comprueba la ortografía o prueba con menos palabras."
      />
    );
  }

  return (
    <div className="flex flex-col gap-10">
      {data.artists?.content.length ? (
        <CategorySection
          title="Artistas"
          total={data.artists.totalElements}
          onSeeAll={() => setTab("artists")}
        >
          <Grid>
            {data.artists.content.map((a) => (
              <ArtistCard key={a.id} artist={a} />
            ))}
          </Grid>
        </CategorySection>
      ) : null}

      {data.songs?.content.length ? (
        <CategorySection
          title="Canciones"
          total={data.songs.totalElements}
          onSeeAll={() => setTab("songs")}
        >
          <SongList songs={data.songs.content} />
        </CategorySection>
      ) : null}

      {data.albums?.content.length ? (
        <CategorySection
          title="Álbumes"
          total={data.albums.totalElements}
          onSeeAll={() => setTab("albums")}
        >
          <Grid>
            {data.albums.content.map((a) => (
              <AlbumCard key={a.id} album={a} />
            ))}
          </Grid>
        </CategorySection>
      ) : null}

      {data.playlists?.content.length ? (
        <CategorySection
          title="Playlists"
          total={data.playlists.totalElements}
          onSeeAll={() => setTab("playlists")}
        >
          <Grid>
            {data.playlists.content.map((p) => (
              <PlaylistCard key={p.id} playlist={p} />
            ))}
          </Grid>
        </CategorySection>
      ) : null}

      {data.genres?.content.length ? (
        <CategorySection
          title="Géneros"
          total={data.genres.totalElements}
          onSeeAll={() => setTab("genres")}
        >
          <Grid>
            {data.genres.content.map((g) => (
              <GenreCard key={g.name} genre={g.name} />
            ))}
          </Grid>
        </CategorySection>
      ) : null}

      {data.users?.content.length ? (
        <CategorySection
          title="Usuarios"
          total={data.users.totalElements}
          onSeeAll={() => setTab("users")}
        >
          <Grid>
            {data.users.content.map((u) => (
              <UserCard key={u.id} user={u} />
            ))}
          </Grid>
        </CategorySection>
      ) : null}
    </div>
  );
}

// =====================================
// ----- Modo específico — categoría única paginada
// =====================================

function SpecificResults({
  type,
  data,
  page,
  onPage,
  q,
}: {
  type: SearchType;
  data: NonNullable<ReturnType<typeof useSearch>["data"]>;
  page: number;
  onPage: (n: number) => void;
  q: string;
}) {
  // Selecciona el slice correspondiente al tipo activo. Cada rama mapea
  // exactamente una categoría del DTO; las demás vienen como `undefined`.
  const sliceLabel = TABS.find((t) => t.value === type)?.label ?? "Resultados";
  let totalElements = 0;
  let totalPages = 1;
  let body: React.ReactNode = null;

  if (type === "songs" && data.songs) {
    totalElements = data.songs.totalElements;
    totalPages = data.songs.totalPages;
    body =
      data.songs.content.length > 0 ? (
        <SongList songs={data.songs.content} />
      ) : null;
  } else if (type === "artists" && data.artists) {
    totalElements = data.artists.totalElements;
    totalPages = data.artists.totalPages;
    body =
      data.artists.content.length > 0 ? (
        <Grid>
          {data.artists.content.map((a) => (
            <ArtistCard key={a.id} artist={a} />
          ))}
        </Grid>
      ) : null;
  } else if (type === "albums" && data.albums) {
    totalElements = data.albums.totalElements;
    totalPages = data.albums.totalPages;
    body =
      data.albums.content.length > 0 ? (
        <Grid>
          {data.albums.content.map((a) => (
            <AlbumCard key={a.id} album={a} />
          ))}
        </Grid>
      ) : null;
  } else if (type === "playlists" && data.playlists) {
    totalElements = data.playlists.totalElements;
    totalPages = data.playlists.totalPages;
    body =
      data.playlists.content.length > 0 ? (
        <Grid>
          {data.playlists.content.map((p) => (
            <PlaylistCard key={p.id} playlist={p} />
          ))}
        </Grid>
      ) : null;
  } else if (type === "users" && data.users) {
    totalElements = data.users.totalElements;
    totalPages = data.users.totalPages;
    body =
      data.users.content.length > 0 ? (
        <Grid>
          {data.users.content.map((u) => (
            <UserCard key={u.id} user={u} />
          ))}
        </Grid>
      ) : null;
  } else if (type === "genres" && data.genres) {
    totalElements = data.genres.totalElements;
    totalPages = data.genres.totalPages;
    body =
      data.genres.content.length > 0 ? (
        <Grid>
          {data.genres.content.map((g) => (
            <GenreCard key={g.name} genre={g.name} />
          ))}
        </Grid>
      ) : null;
  }

  if (totalElements === 0) {
    return (
      <EmptyState
        icon={<SearchIcon />}
        title={`Sin ${sliceLabel.toLowerCase()} para "${q}"`}
        description="Prueba con otra palabra o cambia de pestaña."
      />
    );
  }

  return (
    <div className="flex flex-col gap-6">
      <p className="text-sm text-text-muted">
        {totalElements} {sliceLabel.toLowerCase()}
      </p>
      {body}
      <Pagination
        page={page}
        totalPages={totalPages}
        onChange={onPage}
      />
    </div>
  );
}

// =====================================
// ----- Pequeños subcomponentes
// =====================================

function CategorySection({
  title,
  total,
  onSeeAll,
  children,
}: {
  title: string;
  total: number;
  onSeeAll: () => void;
  children: React.ReactNode;
}) {
  return (
    <section className="flex flex-col gap-3">
      <header className="flex items-baseline justify-between gap-3">
        <h2 className="text-xl font-bold tracking-tight">{title}</h2>
        {total > 0 ? (
          <button
            type="button"
            onClick={onSeeAll}
            className="text-xs font-medium text-text-muted transition-colors hover:text-text"
          >
            Ver todo ({total})
          </button>
        ) : null}
      </header>
      {children}
    </section>
  );
}

function Grid({ children }: { children: React.ReactNode }) {
  return (
    <div className="grid grid-cols-2 gap-4 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-5">
      {children}
    </div>
  );
}

/**
 * Renderiza una lista de canciones reusando {@code SongRow}. El callback de
 * reproducción usa el slice completo como cola para que pulsar play encadene
 * canciones del resultado, no del catálogo entero.
 */
function SongList({ songs }: { songs: SongDTO[] }) {
  const playSong = usePlayerStore((s) => s.playSong);
  return (
    <div className="flex flex-col gap-1">
      {songs.map((s, i) => (
        <SongRow
          key={s.id}
          index={i}
          song={s}
          onPlay={() => playSong(s, songs)}
        />
      ))}
    </div>
  );
}

/**
 * Paginador minimal: anterior/siguiente con indicador de página actual. La
 * mayoría de búsquedas no pasarán de un par de páginas, así que no merece la
 * pena pintar todos los números.
 */
function Pagination({
  page,
  totalPages,
  onChange,
}: {
  page: number;
  totalPages: number;
  onChange: (n: number) => void;
}) {
  if (totalPages <= 1) return null;
  return (
    <div className="flex items-center justify-center gap-3">
      <Button
        variant="outline"
        size="sm"
        disabled={page <= 0}
        onClick={() => onChange(page - 1)}
      >
        Anterior
      </Button>
      <span className="text-sm text-text-muted tabular-nums">
        Página {page + 1} de {totalPages}
      </span>
      <Button
        variant="outline"
        size="sm"
        disabled={page >= totalPages - 1}
        onClick={() => onChange(page + 1)}
      >
        Siguiente
      </Button>
    </div>
  );
}

