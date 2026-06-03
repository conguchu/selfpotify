package davila.anton.selfpotify.data.repository

import davila.anton.selfpotify.data.local.SessionStore
import davila.anton.selfpotify.data.model.ProfileUpdateRequest
import davila.anton.selfpotify.data.model.UserSummaryDTO
import davila.anton.selfpotify.data.network.ApiProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody

/**
 * Perfil del usuario autenticado: lectura (`GET /api/me`) y edición del nombre visible y la foto.
 * Devuelve [Result] para propagar errores sin lanzar a la UI (CLAUDE.md §2).
 */
class ProfileRepository(private val session: SessionStore) {

    /** Vista pública del usuario autenticado (incluye contadores de seguidores/seguidos). */
    suspend fun me(): Result<UserSummaryDTO> = withContext(Dispatchers.IO) {
        runCatching { api().me() }
    }

    /** Cambia el nombre visible (`null`/vacío lo borra y la UI cae al username). */
    suspend fun updateName(name: String?): Result<UserSummaryDTO> = withContext(Dispatchers.IO) {
        runCatching { api().updateProfile(ProfileUpdateRequest(name?.ifBlank { null })) }
    }

    /**
     * Sube la foto de perfil. Recibe los [bytes] ya leídos del `Uri` (la lectura con
     * `ContentResolver` se hace en el ViewModel) más su [mime] y [fileName]. Mismo patrón multipart
     * que [PlaylistRepository.uploadCover].
     */
    suspend fun uploadAvatar(
        bytes: ByteArray,
        mime: String,
        fileName: String,
    ): Result<UserSummaryDTO> = withContext(Dispatchers.IO) {
        runCatching {
            val part = MultipartBody.Part.createFormData(
                "file",
                fileName,
                bytes.toRequestBody(mime.toMediaTypeOrNull()),
            )
            api().uploadAvatar(part)
        }
    }

    /** Borra la foto de perfil. */
    suspend fun deleteAvatar(): Result<UserSummaryDTO> = withContext(Dispatchers.IO) {
        runCatching { api().deleteAvatar() }
    }

    private suspend fun api() =
        ApiProvider.api(session.current().serverUrl ?: error("No server configured"))
}
