package anton.davila.selfpotify;

import static org.junit.jupiter.api.Assertions.*;

import anton.davila.selfpotify.music.entity.Album;
import anton.davila.selfpotify.music.entity.Artist;
import anton.davila.selfpotify.music.entity.Song;
import anton.davila.selfpotify.music.repository.AlbumRepository;
import anton.davila.selfpotify.music.repository.ArtistRepository;
import anton.davila.selfpotify.music.repository.SongRepository;
import anton.davila.selfpotify.user.entity.User;
import anton.davila.selfpotify.user.listen.entity.UserSongListen;
import anton.davila.selfpotify.user.listen.repository.UserSongListenRepository;
import anton.davila.selfpotify.user.repository.UserRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Verifica que la popularidad de canciones, álbumes, artistas y géneros se
 * deriva correctamente de la tabla de eventos {@code user_song_listen}, sin
 * contadores numéricos en las entidades. Es un test sobre H2 en memoria con
 * rollback por método ({@code @Transactional}): no hace ninguna llamada de red.
 */
@SpringBootTest
@Transactional
public class UserSongListenCountTest {

    @PersistenceContext
    private EntityManager em;

    @Autowired
    private UserSongListenRepository repository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private SongRepository songRepository;
    @Autowired
    private AlbumRepository albumRepository;
    @Autowired
    private ArtistRepository artistRepository;

    private Artist rosalia;
    private Artist badBunny;
    private Album motomami;
    private Song saoko;     // rosalia, motomami, genero "Pop"
    private Song chicken;   // rosalia + badBunny, motomami, genero "Pop"
    private Song titi;      // badBunny, sin album, genero "Reggaeton"
    private User alice;
    private User bob;

    @BeforeEach
    void setUp() {
        rosalia = persistArtist("Rosalía");
        badBunny = persistArtist("Bad Bunny");

        motomami = new Album();
        motomami.setName("Motomami");
        motomami = albumRepository.save(motomami);

        saoko = persistSong("Saoko", "Pop", motomami, rosalia);
        chicken = persistSong("Chicken Teriyaki", "Pop", motomami, rosalia, badBunny);
        titi = persistSong("Tití Me Preguntó", "Reggaeton", null, badBunny);

        alice = persistUser("alice");
        bob = persistUser("bob");

        // Escuchas:
        //  - saoko:   alice x3
        //  - chicken: alice x1, bob x2
        //  - titi:    bob x4
        listen(alice, saoko, 3);
        listen(alice, chicken, 1);
        listen(bob, chicken, 2);
        listen(bob, titi, 4);

        em.flush();
        em.clear();
    }

    // ------------------------------------------------------------------
    // Conteos GLOBALES por entidad
    // ------------------------------------------------------------------

    @Test
    void countBySong_countsAllUsersListens() {
        assertEquals(3, repository.countBySong_Id(saoko.getId()));
        assertEquals(3, repository.countBySong_Id(chicken.getId())); // 1 + 2
        assertEquals(4, repository.countBySong_Id(titi.getId()));
    }

    @Test
    void countByAlbum_sumsListensOfItsSongs() {
        // Motomami = saoko (3) + chicken (3) = 6; titi no es del álbum
        assertEquals(6, repository.countByAlbumId(motomami.getId()));
    }

    @Test
    void countByArtist_sumsListensOfEverySongTheArtistIsIn() {
        // Rosalía: saoko (3) + chicken (3) = 6
        assertEquals(6, repository.countByArtistId(rosalia.getId()));
        // Bad Bunny: chicken (3) + titi (4) = 7
        assertEquals(7, repository.countByArtistId(badBunny.getId()));
    }

    @Test
    void countByGenre_sumsListensOfSongsInThatGenre() {
        // Pop: saoko (3) + chicken (3) = 6 ; Reggaeton: titi (4)
        assertEquals(6, repository.countByGenre("Pop"));
        assertEquals(4, repository.countByGenre("Reggaeton"));
        assertEquals(0, repository.countByGenre("Jazz"));
    }

    @Test
    void countListensGroupedBySong_returnsMapForAllListenedSongs() {
        Map<Long, Long> counts = repository.countListensGroupedBySong().stream()
                .collect(Collectors.toMap(r -> (Long) r[0], r -> (Long) r[1]));

        assertEquals(3L, counts.get(saoko.getId()));
        assertEquals(3L, counts.get(chicken.getId()));
        assertEquals(4L, counts.get(titi.getId()));
        assertEquals(3, counts.size());
    }

    // ------------------------------------------------------------------
    // Rankings GLOBALES
    // ------------------------------------------------------------------

    @Test
    void findArtistsByGlobalListensDesc_ordersByDerivedCount() {
        List<Artist> ranked = repository.findArtistsByGlobalListensDesc(PageRequest.of(0, 10));
        // Bad Bunny (7) por delante de Rosalía (6)
        assertEquals(2, ranked.size());
        assertEquals(badBunny.getId(), ranked.get(0).getId());
        assertEquals(rosalia.getId(), ranked.get(1).getId());
    }

    @Test
    void findSongsByGenreOrderByGlobalListensDesc_ordersAndFiltersByGenre() {
        List<Song> pop = repository.findSongsByGenreOrderByGlobalListensDesc("Pop", PageRequest.of(0, 10));
        // saoko (3) y chicken (3) son los únicos "Pop"; titi (Reggaeton) queda fuera
        assertEquals(2, pop.size());
        assertTrue(pop.stream().allMatch(s -> "Pop".equals(s.getGenre())));
        assertTrue(pop.stream().anyMatch(s -> s.getId().equals(saoko.getId())));
        assertTrue(pop.stream().anyMatch(s -> s.getId().equals(chicken.getId())));
    }

    @Test
    void findSongsByArtistOrderByGlobalListensDesc_returnsOnlyThatArtistsSongs() {
        List<Song> byBadBunny = repository.findSongsByArtistOrderByGlobalListensDesc(
                badBunny.getId(), PageRequest.of(0, 10));
        // titi (4) por delante de chicken (3)
        assertEquals(2, byBadBunny.size());
        assertEquals(titi.getId(), byBadBunny.get(0).getId());
        assertEquals(chicken.getId(), byBadBunny.get(1).getId());
    }

    // ------------------------------------------------------------------
    // Rankings POR USUARIO (base de la recomendación personalizada)
    // ------------------------------------------------------------------

    @Test
    void findTopArtistsByUserListens_isPerUser() {
        // alice escuchó Rosalía 4 veces (saoko 3 + chicken 1) y Bad Bunny 1 (chicken)
        List<Artist> aliceTop = repository.findTopArtistsByUserListens(alice.getId(), PageRequest.of(0, 10));
        assertEquals(rosalia.getId(), aliceTop.get(0).getId());

        // bob escuchó Bad Bunny 6 veces (chicken 2 + titi 4) y Rosalía 2 (chicken)
        List<Artist> bobTop = repository.findTopArtistsByUserListens(bob.getId(), PageRequest.of(0, 10));
        assertEquals(badBunny.getId(), bobTop.get(0).getId());
    }

    @Test
    void findTopGenresByUserListens_isPerUserAndOrdered() {
        // alice: Pop 4 (saoko 3 + chicken 1), sin Reggaeton
        List<String> aliceGenres = repository.findTopGenresByUserListens(alice.getId(), PageRequest.of(0, 10));
        assertEquals(List.of("Pop"), aliceGenres);

        // bob: Reggaeton 4 (titi) por delante de Pop 2 (chicken)
        List<String> bobGenres = repository.findTopGenresByUserListens(bob.getId(), PageRequest.of(0, 10));
        assertEquals("Reggaeton", bobGenres.get(0));
        assertEquals("Pop", bobGenres.get(1));
    }

    @Test
    void usersWithoutListens_haveEmptyRankings() {
        User carol = persistUser("carol");
        em.flush();
        assertTrue(repository.findTopArtistsByUserListens(carol.getId(), PageRequest.of(0, 10)).isEmpty());
        assertTrue(repository.findTopGenresByUserListens(carol.getId(), PageRequest.of(0, 10)).isEmpty());
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private Artist persistArtist(String name) {
        Artist a = new Artist();
        a.setName(name);
        return artistRepository.save(a);
    }

    private Song persistSong(String title, String genre, Album album, Artist... artists) {
        Song s = new Song();
        s.setTitle(title);
        s.setGenre(genre);
        s.setAvailable(true);
        s.setAlbum(album);
        s.setArtists(List.of(artists));
        return songRepository.save(s);
    }

    private User persistUser(String username) {
        User u = new User();
        u.setUsername(username);
        u.setPassword("x");
        // @PrePersist le crea un UserFeed automáticamente
        return userRepository.save(u);
    }

    private void listen(User user, Song song, int times) {
        for (int i = 0; i < times; i++) {
            repository.save(new UserSongListen(user, song));
        }
    }
}
