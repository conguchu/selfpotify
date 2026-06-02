package davila.anton.selfpotify.ui.search

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Group
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.SubcomposeAsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import davila.anton.selfpotify.R
import davila.anton.selfpotify.data.model.AlbumDTO
import davila.anton.selfpotify.data.model.GenreResultDTO
import davila.anton.selfpotify.data.model.PlaylistDTO
import davila.anton.selfpotify.data.model.SearchResponseDTO
import davila.anton.selfpotify.data.model.UserSummaryDTO
import davila.anton.selfpotify.ui.common.CoverImage
import davila.anton.selfpotify.ui.discover.ArtistCarousel
import davila.anton.selfpotify.ui.discover.SectionHeader
import davila.anton.selfpotify.ui.discover.SongCarousel
import davila.anton.selfpotify.ui.theme.Spacing
import davila.anton.selfpotify.util.ServerUrl

private val CARD_WIDTH = 140.dp
private val USER_WIDTH = 120.dp

/**
 * Pestaña Búsqueda: una barra de texto que busca en vivo (debounce) sobre `GET /api/search` en modo
 * `all`, y debajo una vista previa multi-categoría (canciones, artistas, álbumes, playlists,
 * usuarios y géneros) como columna de carruseles horizontales, al estilo de Descubrir. Solo las
 * canciones son interactivas (reproducen al pulsar); el resto de categorías aún no tienen pantalla
 * de detalle a la que navegar.
 */
@Composable
fun SearchScreen(
    contentPadding: PaddingValues,
    onOpenArtist: (Long) -> Unit,
    onOpenAlbum: (Long) -> Unit,
    onOpenPlaylist: (Long) -> Unit,
    onOpenUser: (Long) -> Unit,
    vm: SearchViewModel = viewModel(),
) {
    val state by vm.state.collectAsStateWithLifecycle()

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(modifier = Modifier.fillMaxSize().padding(top = contentPadding.calculateTopPadding())) {
            SearchField(
                query = state.query,
                onQueryChange = vm::onQueryChange,
                onClear = vm::clear,
            )

            val response = state.response
            when {
                state.loading && response == null -> CenterBox { CircularProgressIndicator() }

                state.error -> CenterMessage(stringResource(R.string.search_error))

                state.query.isBlank() -> CenterMessage(stringResource(R.string.search_prompt))

                response != null && response.isEmpty ->
                    CenterMessage(stringResource(R.string.search_no_results, state.query.trim()))

                response != null -> Results(
                    response = response,
                    serverUrl = state.serverUrl,
                    bottomPadding = contentPadding.calculateBottomPadding(),
                    onPlaySong = { queue, index -> vm.play(queue, index) },
                    onOpenArtist = onOpenArtist,
                    onOpenAlbum = onOpenAlbum,
                    onOpenPlaylist = onOpenPlaylist,
                    onOpenUser = onOpenUser,
                )
            }
        }
    }
}

@Composable
private fun SearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    onClear: () -> Unit,
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.page, vertical = Spacing.s),
        placeholder = { Text(stringResource(R.string.search_hint)) },
        singleLine = true,
        leadingIcon = {
            Icon(Icons.Rounded.Search, contentDescription = stringResource(R.string.search_title))
        },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = onClear) {
                    Icon(Icons.Rounded.Close, contentDescription = stringResource(R.string.search_clear))
                }
            }
        },
        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(imeAction = ImeAction.Search),
    )
}

@Composable
private fun Results(
    response: SearchResponseDTO,
    serverUrl: String?,
    bottomPadding: androidx.compose.ui.unit.Dp,
    onPlaySong: (List<davila.anton.selfpotify.data.model.SongDTO>, Int) -> Unit,
    onOpenArtist: (Long) -> Unit,
    onOpenAlbum: (Long) -> Unit,
    onOpenPlaylist: (Long) -> Unit,
    onOpenUser: (Long) -> Unit,
) {
    LazyColumn(
        contentPadding = PaddingValues(top = Spacing.s, bottom = bottomPadding),
        verticalArrangement = Arrangement.spacedBy(Spacing.l),
        modifier = Modifier.fillMaxSize(),
    ) {
        response.songs?.content?.takeIf { it.isNotEmpty() }?.let { songs ->
            item {
                SongCarousel(
                    icon = Icons.Rounded.MusicNote,
                    title = stringResource(R.string.search_section_songs),
                    songs = songs,
                    serverUrl = serverUrl,
                    onPlay = { index -> onPlaySong(songs, index) },
                )
            }
        }
        response.artists?.content?.takeIf { it.isNotEmpty() }?.let { artists ->
            item {
                ArtistCarousel(
                    title = stringResource(R.string.search_section_artists),
                    artists = artists,
                    serverUrl = serverUrl,
                    onArtistClick = onOpenArtist,
                )
            }
        }
        response.albums?.content?.takeIf { it.isNotEmpty() }?.let { albums ->
            item { AlbumCarousel(albums = albums, serverUrl = serverUrl, onAlbumClick = onOpenAlbum) }
        }
        response.playlists?.content?.takeIf { it.isNotEmpty() }?.let { playlists ->
            item { PlaylistCarousel(playlists = playlists, onPlaylistClick = onOpenPlaylist) }
        }
        response.users?.content?.takeIf { it.isNotEmpty() }?.let { users ->
            item { UserCarousel(users = users, serverUrl = serverUrl, onUserClick = onOpenUser) }
        }
        response.genres?.content?.takeIf { it.isNotEmpty() }?.let { genres ->
            item { GenreCarousel(genres = genres) }
        }
    }
}

/** Carrusel de álbumes (carátula + nombre). Pulsar abre el detalle del álbum. */
@Composable
private fun AlbumCarousel(albums: List<AlbumDTO>, serverUrl: String?, onAlbumClick: (Long) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.m)) {
        SectionHeader(Icons.Rounded.Album, stringResource(R.string.search_section_albums))
        LazyRow(
            contentPadding = PaddingValues(horizontal = Spacing.page),
            horizontalArrangement = Arrangement.spacedBy(Spacing.m),
        ) {
            items(albums, key = { it.id }) { album ->
                Column(
                    modifier = Modifier
                        .width(CARD_WIDTH)
                        .clickable { onAlbumClick(album.id) },
                    verticalArrangement = Arrangement.spacedBy(Spacing.s),
                ) {
                    CoverImage(
                        url = ServerUrl.asset(serverUrl, album.pictureUrl),
                        modifier = Modifier.size(CARD_WIDTH),
                    )
                    Text(
                        text = album.name.orEmpty(),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onBackground,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

/** Carrusel de playlists (icono + nombre + nº de canciones). Pulsar abre el detalle de la playlist. */
@Composable
private fun PlaylistCarousel(playlists: List<PlaylistDTO>, onPlaylistClick: (Long) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.m)) {
        SectionHeader(Icons.AutoMirrored.Rounded.QueueMusic, stringResource(R.string.search_section_playlists))
        LazyRow(
            contentPadding = PaddingValues(horizontal = Spacing.page),
            horizontalArrangement = Arrangement.spacedBy(Spacing.m),
        ) {
            items(playlists, key = { it.id }) { playlist ->
                Column(
                    modifier = Modifier
                        .width(CARD_WIDTH)
                        .clickable { onPlaylistClick(playlist.id) },
                    verticalArrangement = Arrangement.spacedBy(Spacing.s),
                ) {
                    // PlaylistDTO de la app no expone carátula: se pinta el placeholder de CoverImage.
                    CoverImage(url = null, modifier = Modifier.size(CARD_WIDTH))
                    Text(
                        text = playlist.name.orEmpty(),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onBackground,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = stringResource(R.string.playlist_song_count, playlist.songIds?.size ?: 0),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

/** Carrusel de usuarios (avatar circular + nombre visible). Pulsar abre el detalle del usuario. */
@Composable
private fun UserCarousel(users: List<UserSummaryDTO>, serverUrl: String?, onUserClick: (Long) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.m)) {
        SectionHeader(Icons.Rounded.Group, stringResource(R.string.search_section_users))
        LazyRow(
            contentPadding = PaddingValues(horizontal = Spacing.page),
            horizontalArrangement = Arrangement.spacedBy(Spacing.m),
        ) {
            items(users, key = { it.id }) { user ->
                Column(
                    modifier = Modifier
                        .width(USER_WIDTH)
                        .clickable { onUserClick(user.id) },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(Spacing.s),
                ) {
                    UserAvatar(
                        url = ServerUrl.asset(serverUrl, user.avatarUrl),
                        modifier = Modifier.size(USER_WIDTH),
                    )
                    Text(
                        text = user.displayName?.takeIf { it.isNotBlank() } ?: user.username.orEmpty(),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onBackground,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

/** Carrusel de géneros como chips (nombre + nº de canciones). No interactivo de momento. */
@Composable
private fun GenreCarousel(genres: List<GenreResultDTO>) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.m)) {
        SectionHeader(Icons.Rounded.Category, stringResource(R.string.search_section_genres))
        LazyRow(
            contentPadding = PaddingValues(horizontal = Spacing.page),
            horizontalArrangement = Arrangement.spacedBy(Spacing.s),
        ) {
            items(genres, key = { it.name.orEmpty() }) { genre ->
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = MaterialTheme.shapes.medium,
                ) {
                    Column(modifier = Modifier.padding(horizontal = Spacing.m, vertical = Spacing.s)) {
                        Text(
                            text = genre.name.orEmpty(),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = stringResource(R.string.playlist_song_count, genre.songCount),
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

/** Avatar circular del usuario; mientras carga o si falta/falla pinta un icono de persona. */
@Composable
private fun UserAvatar(url: String?, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center,
    ) {
        if (url.isNullOrBlank()) {
            PersonPlaceholder()
        } else {
            SubcomposeAsyncImage(
                model = ImageRequest.Builder(LocalContext.current).data(url).crossfade(true).build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                loading = { PersonPlaceholder() },
                error = { PersonPlaceholder() },
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@Composable
private fun PersonPlaceholder() {
    Icon(
        imageVector = Icons.Rounded.Person,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.size(40.dp),
    )
}

@Composable
private fun CenterBox(content: @Composable () -> Unit) {
    Box(Modifier.fillMaxSize(), Alignment.Center) { content() }
}

@Composable
private fun CenterMessage(text: String) {
    Box(Modifier.fillMaxSize().padding(Spacing.xl), Alignment.Center) {
        Text(
            text = text,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}
