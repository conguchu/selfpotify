import { apiFetch } from "./client";
import type { SearchResponseDTO, SearchType } from "@/lib/types";

/**
 * Llama al endpoint global `GET /api/search`.
 *
 * El backend normaliza la query (lowercase + strip de diacríticos) y devuelve
 * siempre la misma forma `SearchResponseDTO`; en modo `all` se rellenan todas
 * las categorías recortadas a 5, en modo específico solo la pedida paginada.
 */
export function search(params: {
  q: string;
  type?: SearchType;
  page?: number;
  size?: number;
}) {
  const search = new URLSearchParams();
  search.set("q", params.q);
  if (params.type) search.set("type", params.type);
  if (typeof params.page === "number") search.set("page", String(params.page));
  if (typeof params.size === "number") search.set("size", String(params.size));
  return apiFetch<SearchResponseDTO>(`/api/search?${search.toString()}`);
}
