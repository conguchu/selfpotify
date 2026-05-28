package anton.davila.selfpotify;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import anton.davila.selfpotify.controllers.dto.AlbumDTO;
import anton.davila.selfpotify.controllers.dto.ArtistDTO;
import anton.davila.selfpotify.controllers.dto.GenreResultDTO;
import anton.davila.selfpotify.controllers.dto.PlaylistDTO;
import anton.davila.selfpotify.controllers.dto.SearchResponseDTO;
import anton.davila.selfpotify.controllers.dto.SongDTO;
import anton.davila.selfpotify.controllers.dto.UserSummaryDTO;
import anton.davila.selfpotify.music.entity.Album;
import anton.davila.selfpotify.music.entity.Artist;
import anton.davila.selfpotify.music.entity.Playlist;
import anton.davila.selfpotify.music.entity.Song;
import anton.davila.selfpotify.music.repository.AlbumRepository;
import anton.davila.selfpotify.music.repository.ArtistRepository;
import anton.davila.selfpotify.music.repository.PlaylistRepository;
import anton.davila.selfpotify.music.repository.SongRepository;
import anton.davila.selfpotify.music.service.SearchService;
import anton.davila.selfpotify.music.service.SongService;
import anton.davila.selfpotify.user.entity.Admin;
import anton.davila.selfpotify.user.entity.User;
import anton.davila.selfpotify.user.profile.entity.Profile;
import anton.davila.selfpotify.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Tests del {@link SearchService}.
 *
 * <p>El servicio carga listas de repositorio en memoria y filtra/score allí,
 * así que se mockean los repos para inyectar catálogos controlados; cada test
 * verifica una propiedad concreta de la búsqueda (insensibilidad a
 * acentos/mayúsculas, scoring, paginación, etc.).
 */
@SpringBootTest
class SearchServiceTest {

    @Autowired
    private SearchService searchService;

    @MockitoBean
    private SongRepository songRepository;
    @MockitoBean
    private ArtistRepository artistRepository;
    @MockitoBean
    private AlbumRepository albumRepository;
    @MockitoBean
    private PlaylistRepository playlistRepository;
    @MockitoBean
    private UserRepository userRepository;
    @MockitoBean
    private SongService songService;

    private Artist rosalia;
    private Artist queen;
    private Album elMalQuerer;
    private Album opera;
    private Song malamente;
    private Song bohemian;
    private Song stairway;
    private Playlist publicMix;
    private Playlist privateMix;
    private User user;
    private User otherUser;

    @BeforeEach
    void setUp() {
        rosalia = artist(1L, "Rosalía");
        queen = artist(2L, "Queen");

        elMalQuerer = album(10L, "El Mal Querer", List.of(rosalia));
        opera = album(11L, "A Night at the Opera", List.of(queen));

        malamente = song(100L, "Malamente", "Flamenco Pop", List.of(rosalia), elMalQuerer);
        bohemian = song(101L, "Bohemian Rhapsody", "Rock", List.of(queen), opera);
        stairway = song(102L, "Stairway to Heaven", "Rock", List.of(queen), null);

        // El catálogo queda enlazado en ambos sentidos para que los haystacks de
        // artistas y álbumes contengan también sus canciones cuando proceda.
        rosalia.setSongs(new ArrayList<>(List.of(malamente)));
        rosalia.setAlbums(new ArrayList<>(List.of(elMalQuerer)));
        queen.setSongs(new ArrayList<>(List.of(bohemian, stairway)));
        queen.setAlbums(new ArrayList<>(List.of(opera)));
        elMalQuerer.setSongs(new ArrayList<>(List.of(malamente)));
        opera.setSongs(new ArrayList<>(List.of(bohemian)));

        user = user(1000L, "anton", "Anton Davila");
        otherUser = user(1001L, "maria", "María López");

        publicMix = playlist(200L, "Rock Mix", "best rock songs", true, otherUser, List.of(bohemian, stairway));
        privateMix = playlist(201L, "Mi secreta", null, false, otherUser, List.of(malamente));

        when(songRepository.findAll()).thenReturn(List.of(malamente, bohemian, stairway));
        when(artistRepository.findAll()).thenReturn(List.of(rosalia, queen));
        when(albumRepository.findAll()).thenReturn(List.of(elMalQuerer, opera));
        when(playlistRepository.findAll()).thenReturn(List.of(publicMix, privateMix));
        when(userRepository.findAll()).thenReturn(List.of(user, otherUser));
        when(songRepository.findDistinctGenres()).thenReturn(List.of("Flamenco Pop", "Rock"));

        // Sin escuchas reales — el mapa vacío se traduce en listeners=0 sin N+1.
        when(songService.getListenCountsBySong()).thenReturn(new HashMap<>());
    }

    // =====================================
    // ----- Normalización (acentos / mayúsculas / espacios)
    // =====================================

    @Test
    void normalize_stripsDiacriticsAndLowercases() {
        assertEquals("rosalia", SearchService.normalize("Rosalía"));
        assertEquals("rosalia", SearchService.normalize("ROSALÍA"));
        assertEquals("nino bravo", SearchService.normalize("  Niño   BRAVO  "));
        assertEquals("", SearchService.normalize(null));
    }

    @Test
    void search_byAccent_findsArtist() {
        // "rosalia" sin acento debe encontrar a "Rosalía".
        SearchResponseDTO res = searchService.search("rosalia", "artists", 0, 10, user);
        assertNotNull(res.getArtists());
        assertEquals(1, res.getArtists().getContent().size());
        assertEquals("Rosalía", res.getArtists().getContent().get(0).getName());
    }

    @Test
    void search_byCaseAndAccent_findsArtist() {
        // Acento en la query y diferente caja: simétrico, debe encontrar también.
        SearchResponseDTO res = searchService.search("ROSALÍA", "artists", 0, 10, user);
        assertEquals(1, res.getArtists().getContent().size());
    }

    // =====================================
    // ----- Modo "all" (vista previa multi-categoría)
    // =====================================

    @Test
    void search_allMode_populatesAllCategories() {
        // "rock" matchea: bohemian/stairway (canciones), Queen (artista por sus
        // canciones), A Night at the Opera (álbum por sus canciones), "Rock"
        // (género), Rock Mix (playlist).
        SearchResponseDTO res = searchService.search("rock", "all", 0, 20, user);

        assertEquals("all", res.getType());
        assertNotNull(res.getSongs());
        assertNotNull(res.getArtists());
        assertNotNull(res.getAlbums());
        assertNotNull(res.getPlaylists());
        assertNotNull(res.getUsers());
        assertNotNull(res.getGenres());

        // Canciones: las dos de Queen contienen "rock" en el haystack (género).
        assertEquals(2, res.getSongs().getTotalElements());
        // Género "Rock" presente.
        assertTrue(res.getGenres().getContent().stream().anyMatch(g -> "Rock".equals(g.getName())));
        // Playlist pública "Rock Mix" presente.
        assertTrue(res.getPlaylists().getContent().stream().anyMatch(p -> "Rock Mix".equals(p.getName())));
    }

    @Test
    void search_allMode_blankQuery_returnsEmptyShape() {
        SearchResponseDTO res = searchService.search("", "all", 0, 20, user);
        assertNotNull(res.getSongs());
        assertEquals(0, res.getSongs().getTotalElements());
        assertEquals(0, res.getArtists().getTotalElements());
        assertEquals(0, res.getAlbums().getTotalElements());
        assertEquals(0, res.getPlaylists().getTotalElements());
        assertEquals(0, res.getUsers().getTotalElements());
        assertEquals(0, res.getGenres().getTotalElements());
    }

    // =====================================
    // ----- Tokens (todas las palabras deben aparecer)
    // =====================================

    @Test
    void search_multiToken_requiresAllTokens() {
        // "stairway heaven" debe matchear "Stairway to Heaven" aunque "to" no esté.
        SearchResponseDTO res = searchService.search("stairway heaven", "songs", 0, 10, user);
        assertEquals(1, res.getSongs().getTotalElements());
        assertEquals("Stairway to Heaven", res.getSongs().getContent().get(0).getTitle());
    }

    @Test
    void search_multiToken_failsIfOneTokenMissing() {
        // Ningún haystack contiene "xyz", así que no hay matches.
        SearchResponseDTO res = searchService.search("stairway xyz", "songs", 0, 10, user);
        assertEquals(0, res.getSongs().getTotalElements());
    }

    // =====================================
    // ----- Scoring (exact > startsWith > word > substring)
    // =====================================

    @Test
    void search_scoring_exactMatchFirst() {
        // Añadimos una canción cuyo título es exactamente "rock" para forzar
        // el caso de score=0 vs score=3 de las dos de Queen.
        Song exactRock = song(103L, "Rock", "Rock", List.of(queen), null);
        when(songRepository.findAll()).thenReturn(List.of(malamente, bohemian, stairway, exactRock));

        SearchResponseDTO res = searchService.search("rock", "songs", 0, 10, user);
        List<SongDTO> content = res.getSongs().getContent();
        assertFalse(content.isEmpty());
        // El primero debe ser el de título exactamente "Rock".
        assertEquals("Rock", content.get(0).getTitle());
    }

    // =====================================
    // ----- Visibilidad de playlists privadas
    // =====================================

    @Test
    void search_playlists_excludesOtherUsersPrivate() {
        // Buscando "secreta" como `user` (que no es el creador), la privada NO aparece.
        SearchResponseDTO res = searchService.search("secreta", "playlists", 0, 10, user);
        assertEquals(0, res.getPlaylists().getTotalElements());
    }

    @Test
    void search_playlists_includesOwnPrivate() {
        // Si la consulta la hace el dueño (otherUser), la playlist privada sí aparece.
        SearchResponseDTO res = searchService.search("secreta", "playlists", 0, 10, otherUser);
        assertEquals(1, res.getPlaylists().getTotalElements());
        assertEquals("Mi secreta", res.getPlaylists().getContent().get(0).getName());
    }

    // =====================================
    // ----- Usuarios
    // =====================================

    @Test
    void search_users_byProfileNameWithAccent() {
        // "maria" sin acento debe localizar al usuario cuyo perfil es "María López".
        SearchResponseDTO res = searchService.search("maria", "users", 0, 10, user);
        assertEquals(1, res.getUsers().getTotalElements());
        UserSummaryDTO found = res.getUsers().getContent().get(0);
        assertEquals("maria", found.getUsername());
        assertEquals("María López", found.getDisplayName());
    }

    // =====================================
    // ----- Géneros
    // =====================================

    @Test
    void search_genres_countsSongsPerGenre() {
        SearchResponseDTO res = searchService.search("rock", "genres", 0, 10, user);
        assertEquals(1, res.getGenres().getTotalElements());
        GenreResultDTO g = res.getGenres().getContent().get(0);
        assertEquals("Rock", g.getName());
        assertEquals(2L, g.getSongCount());
    }

    // =====================================
    // ----- Paginación
    // =====================================

    @Test
    void search_specificMode_paginatesCorrectly() {
        // 5 canciones que matchean "a" en su título: forzamos un tamaño 2 para
        // verificar que totalPages y los slices se calculan bien.
        List<Song> many = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            many.add(song(500L + i, "alpha " + i, "Pop", List.of(rosalia), null));
        }
        when(songRepository.findAll()).thenReturn(many);

        SearchResponseDTO p0 = searchService.search("alpha", "songs", 0, 2, user);
        assertEquals(5, p0.getSongs().getTotalElements());
        assertEquals(3, p0.getSongs().getTotalPages());
        assertEquals(2, p0.getSongs().getContent().size());

        SearchResponseDTO p2 = searchService.search("alpha", "songs", 2, 2, user);
        assertEquals(1, p2.getSongs().getContent().size());
    }

    @Test
    void search_specificMode_otherCategoriesAreNull() {
        SearchResponseDTO res = searchService.search("rock", "songs", 0, 5, user);
        assertNotNull(res.getSongs());
        assertNull(res.getArtists());
        assertNull(res.getAlbums());
        assertNull(res.getPlaylists());
        assertNull(res.getUsers());
        assertNull(res.getGenres());
    }

    // =====================================
    // ----- Validación de tipo
    // =====================================

    @Test
    void search_invalidType_throws() {
        try {
            searchService.search("rock", "invented-type", 0, 5, user);
            assertTrue(false, "Debería haber lanzado IllegalArgumentException");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage().contains("invented-type"));
        }
    }

    // =====================================
    // ----- Helpers de fabricación
    // =====================================

    private static Artist artist(Long id, String name) {
        Artist a = new Artist();
        a.setId(id);
        a.setName(name);
        a.setSongs(new ArrayList<>());
        a.setAlbums(new ArrayList<>());
        return a;
    }

    private static Album album(Long id, String name, List<Artist> artists) {
        Album a = new Album();
        a.setId(id);
        a.setName(name);
        a.setArtists(artists);
        a.setSongs(new ArrayList<>());
        return a;
    }

    private static Song song(Long id, String title, String genre, List<Artist> artists, Album album) {
        Song s = new Song();
        s.setId(id);
        s.setTitle(title);
        s.setGenre(genre);
        s.setArtists(artists);
        s.setAlbum(album);
        return s;
    }

    private static Playlist playlist(Long id, String name, String description, boolean isPublic, User creator, List<Song> songs) {
        Playlist p = new Playlist();
        p.setId(id);
        p.setName(name);
        p.setDescription(description);
        p.setPublic(isPublic);
        p.setCreator(creator);
        p.setSongs(songs);
        return p;
    }

    private static User user(Long id, String username, String profileName) {
        User u = new User();
        u.setId(id);
        u.setUsername(username);
        Profile profile = new Profile();
        profile.setName(profileName);
        u.setProfile(profile);
        return u;
    }
}
