package davila.anton.selfpotify.data.repository

import davila.anton.selfpotify.data.local.SessionStore
import davila.anton.selfpotify.data.model.SearchResponseDTO
import davila.anton.selfpotify.data.network.ApiProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Fuente de verdad de la pestaña Búsqueda. Consume `GET /api/search` en modo `all` (vista previa
 * multi-categoría, hasta 5 por categoría). Devuelve [Result] para propagar errores sin lanzar a la
 * UI (CLAUDE.md §2).
 */
class SearchRepository(private val session: SessionStore) {

    /** Busca [query] en todas las categorías a la vez (modo `all`). */
    suspend fun searchAll(query: String): Result<SearchResponseDTO> = withContext(Dispatchers.IO) {
        runCatching { api().search(query) }
    }

    private suspend fun api() =
        ApiProvider.api(session.current().serverUrl ?: error("No server configured"))
}
