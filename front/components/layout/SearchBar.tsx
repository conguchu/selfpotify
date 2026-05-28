"use client";

import * as React from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { Disc3, ListMusic, Loader2, Search, X } from "lucide-react";
import { Avatar } from "@/components/ui/Avatar";
import { CoverArt } from "@/components/music/CoverArt";
import { useSearch } from "@/lib/query/hooks";
import { useDebouncedValue } from "@/lib/use-debounced-value";
import { usePlayerStore } from "@/lib/player/store";
import { cn } from "@/lib/utils";

/**
 * Barra de búsqueda global del topbar.
 *
 * Comportamiento, en línea con cómo lo hace Spotify:
 * <ul>
 *   <li>Mientras el usuario teclea, se debouncean 250 ms y se lanza
 *       <code>GET /api/search?q=...&type=all</code> (preview multi-categoría).</li>
 *   <li>Mientras hay foco y hay texto, se despliega un dropdown con hasta
 *       5 elementos por categoría enlazados directamente a la entidad.</li>
 *   <li>Al pulsar <kbd>Enter</kbd> (o "Ver todos los resultados") se navega a
 *       <code>/search?q=...</code>, donde se muestra la página dedicada con
 *       pestañas y paginación.</li>
 *   <li><kbd>Esc</kbd> cierra el dropdown sin perder el texto.</li>
 * </ul>
 *
 * El input es controlado y solo el valor debouncado se pasa al hook de query;
 * así el tipado se siente instantáneo aunque la red sea más lenta.
 */
export function SearchBar() {
  const router = useRouter();
  const [value, setValue] = React.useState("");
  const [focused, setFocused] = React.useState(false);
  const containerRef = React.useRef<HTMLDivElement>(null);
  const inputRef = React.useRef<HTMLInputElement>(null);
  const debounced = useDebouncedValue(value, 250);
  const playSong = usePlayerStore((s) => s.playSong);
  const trimmed = debounced.trim();
  // Preview compacto: 5 elementos por categoría (lo que devuelve el modo "all").
  const query = useSearch(trimmed, "all", 0, 5);

  // Click fuera → cierra el dropdown.
  React.useEffect(() => {
    if (!focused) return;
    const onMouseDown = (e: MouseEvent) => {
      if (
        containerRef.current &&
        !containerRef.current.contains(e.target as Node)
      ) {
        setFocused(false);
      }
    };
    document.addEventListener("mousedown", onMouseDown);
    return () => document.removeEventListener("mousedown", onMouseDown);
  }, [focused]);

  // Cierra el dropdown automáticamente cuando el usuario navega a otra ruta.
  const navigate = (href: string) => {
    setFocused(false);
    router.push(href);
  };

  const onSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    const q = value.trim();
    if (!q) return;
    navigate(`/search?q=${encodeURIComponent(q)}`);
  };

  const onKeyDown = (e: React.KeyboardEvent<HTMLInputElement>) => {
    if (e.key === "Escape") {
      if (value) {
        setValue("");
      } else {
        setFocused(false);
        inputRef.current?.blur();
      }
    }
  };

  const data = query.data;
  const isOpen = focused && trimmed.length > 0;
  const isLoading = query.isFetching && !data;
  const hasAnyResult =
    !!data &&
    [
      data.songs,
      data.artists,
      data.albums,
      data.playlists,
      data.users,
      data.genres,
    ].some((c) => c && c.content.length > 0);

  return (
    <div ref={containerRef} className="relative w-full max-w-md">
      <form onSubmit={onSubmit} role="search" className="relative">
        <Search
          aria-hidden
          className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-text-subtle"
        />
        <input
          ref={inputRef}
          type="search"
          value={value}
          onChange={(e) => setValue(e.target.value)}
          onFocus={() => setFocused(true)}
          onKeyDown={onKeyDown}
          placeholder="Buscar canciones, artistas, álbumes…"
          aria-label="Buscar"
          className={cn(
            "h-10 w-full rounded-full border border-border bg-bg-card pl-9 pr-9 text-sm text-text",
            "placeholder:text-text-subtle",
            "focus-visible:outline-none focus-visible:border-accent focus-visible:ring-2 focus-visible:ring-accent/40",
            // Oculta la X nativa del input[type=search] (la "azul" de WebKit/Chromium)
            // para que solo quede el botón blanco propio.
            "[&::-webkit-search-cancel-button]:appearance-none [&::-webkit-search-decoration]:appearance-none",
          )}
        />
        {value ? (
          <button
            type="button"
            aria-label="Limpiar búsqueda"
            onClick={() => {
              setValue("");
              inputRef.current?.focus();
            }}
            className="absolute right-2 top-1/2 inline-flex h-7 w-7 -translate-y-1/2 items-center justify-center rounded-full text-text-muted transition-colors hover:bg-bg-hover hover:text-text"
          >
            <X className="h-4 w-4" />
          </button>
        ) : null}
      </form>

      {isOpen ? (
        <div
          role="listbox"
          aria-label="Resultados de búsqueda"
          className="absolute left-0 right-0 top-full z-30 mt-2 max-h-[70vh] overflow-y-auto rounded-md border border-border bg-bg-elevated p-2 shadow-2xl"
        >
          {isLoading ? (
            <div className="flex items-center gap-2 px-3 py-6 text-sm text-text-muted">
              <Loader2 className="h-4 w-4 animate-spin" />
              Buscando…
            </div>
          ) : query.isError ? (
            <p className="px-3 py-6 text-sm text-danger">
              Error al buscar:{" "}
              {query.error instanceof Error ? query.error.message : "?"}
            </p>
          ) : !hasAnyResult ? (
            <p className="px-3 py-6 text-sm text-text-muted">
              Sin resultados para "{trimmed}".
            </p>
          ) : (
            <>
              {data?.artists?.content.length ? (
                <PreviewSection title="Artistas">
                  {data.artists.content.map((a) => (
                    <button
                      key={`artist-${a.id}`}
                      role="option"
                      onClick={() => navigate(`/artist/${a.id}`)}
                      className="flex w-full items-center gap-3 rounded-md px-2 py-2 text-left transition-colors hover:bg-bg-hover"
                    >
                      <Avatar
                        src={a.photoUrl}
                        alt={a.name}
                        size="md"
                        className="h-10 w-10"
                      />
                      <div className="min-w-0 flex-1">
                        <p className="truncate text-sm font-medium">{a.name}</p>
                        <p className="text-xs text-text-subtle">Artista</p>
                      </div>
                    </button>
                  ))}
                </PreviewSection>
              ) : null}

              {data?.songs?.content.length ? (
                <PreviewSection title="Canciones">
                  {data.songs.content.map((s) => (
                    <button
                      key={`song-${s.id}`}
                      role="option"
                      onClick={() => {
                        // El click en una canción del dropdown la reproduce
                        // directamente, usando el resto de canciones del
                        // preview como cola para que el "siguiente" encadene
                        // resultados afines a la búsqueda.
                        playSong(s, data.songs!.content);
                        setFocused(false);
                      }}
                      className="flex w-full items-center gap-3 rounded-md px-2 py-2 text-left transition-colors hover:bg-bg-hover"
                    >
                      <CoverArt
                        src={s.picture_url}
                        alt={s.title}
                        size="sm"
                        rounded="md"
                      />
                      <div className="min-w-0 flex-1">
                        <p className="truncate text-sm font-medium">
                          {s.title}
                        </p>
                        <p className="truncate text-xs text-text-subtle">
                          {s.artistNames?.join(", ") || "Canción"}
                        </p>
                      </div>
                    </button>
                  ))}
                </PreviewSection>
              ) : null}

              {data?.albums?.content.length ? (
                <PreviewSection title="Álbumes">
                  {data.albums.content.map((a) => (
                    <button
                      key={`album-${a.id}`}
                      role="option"
                      onClick={() => navigate(`/album/${a.id}`)}
                      className="flex w-full items-center gap-3 rounded-md px-2 py-2 text-left transition-colors hover:bg-bg-hover"
                    >
                      <CoverArt
                        src={a.pictureUrl}
                        alt={a.name}
                        size="sm"
                        rounded="md"
                      />
                      <div className="min-w-0 flex-1">
                        <p className="truncate text-sm font-medium">{a.name}</p>
                        <p className="text-xs text-text-subtle">Álbum</p>
                      </div>
                    </button>
                  ))}
                </PreviewSection>
              ) : null}

              {data?.playlists?.content.length ? (
                <PreviewSection title="Playlists">
                  {data.playlists.content.map((p) => (
                    <button
                      key={`playlist-${p.id}`}
                      role="option"
                      onClick={() => navigate(`/playlist/${p.id}`)}
                      className="flex w-full items-center gap-3 rounded-md px-2 py-2 text-left transition-colors hover:bg-bg-hover"
                    >
                      <PreviewIcon>
                        <ListMusic className="h-5 w-5" />
                      </PreviewIcon>
                      <div className="min-w-0 flex-1">
                        <p className="truncate text-sm font-medium">{p.name}</p>
                        <p className="truncate text-xs text-text-subtle">
                          {p.isPublic ? "Playlist pública" : "Playlist privada"}
                        </p>
                      </div>
                    </button>
                  ))}
                </PreviewSection>
              ) : null}

              {data?.genres?.content.length ? (
                <PreviewSection title="Géneros">
                  {data.genres.content.map((g) => (
                    <button
                      key={`genre-${g.name}`}
                      role="option"
                      onClick={() =>
                        navigate(`/genre/${encodeURIComponent(g.name)}`)
                      }
                      className="flex w-full items-center gap-3 rounded-md px-2 py-2 text-left transition-colors hover:bg-bg-hover"
                    >
                      <PreviewIcon>
                        <Disc3 className="h-5 w-5" />
                      </PreviewIcon>
                      <div className="min-w-0 flex-1">
                        <p className="truncate text-sm font-medium capitalize">
                          {g.name}
                        </p>
                        <p className="text-xs text-text-subtle">
                          {g.songCount}{" "}
                          {g.songCount === 1 ? "canción" : "canciones"}
                        </p>
                      </div>
                    </button>
                  ))}
                </PreviewSection>
              ) : null}

              {data?.users?.content.length ? (
                <PreviewSection title="Usuarios">
                  {data.users.content.map((u) => (
                    <div
                      key={`user-${u.id}`}
                      className="flex w-full items-center gap-3 rounded-md px-2 py-2"
                    >
                      <Avatar
                        src={u.avatarUrl}
                        alt={u.displayName ?? u.username}
                        size="md"
                        className="h-10 w-10"
                      />
                      <div className="min-w-0 flex-1">
                        <p className="truncate text-sm font-medium">
                          {u.displayName?.trim() || u.username}
                        </p>
                        <p className="text-xs text-text-subtle">
                          {u.type === "ADMIN" ? "Administrador" : "Usuario"}
                          {u.displayName ? ` · @${u.username}` : ""}
                        </p>
                      </div>
                    </div>
                  ))}
                </PreviewSection>
              ) : null}

              <div className="border-t border-border px-2 pb-1 pt-2">
                <Link
                  href={`/search?q=${encodeURIComponent(trimmed)}`}
                  onClick={() => setFocused(false)}
                  className="block rounded-md px-2 py-2 text-center text-xs font-medium text-accent-hover transition-colors hover:bg-bg-hover"
                >
                  Ver todos los resultados para "{trimmed}"
                </Link>
              </div>
            </>
          )}
        </div>
      ) : null}
    </div>
  );
}

function PreviewSection({
  title,
  children,
}: {
  title: string;
  children: React.ReactNode;
}) {
  return (
    <div className="px-1 py-1">
      <p className="px-2 pb-1 text-[10px] font-semibold uppercase tracking-wide text-text-subtle">
        {title}
      </p>
      {children}
    </div>
  );
}

/** Cuadrado neutro para entidades sin imagen (playlists sin portada, géneros). */
function PreviewIcon({ children }: { children: React.ReactNode }) {
  return (
    <div className="flex h-10 w-10 shrink-0 items-center justify-center rounded-md bg-bg-hover text-accent-hover">
      {children}
    </div>
  );
}

