package anton.davila.selfpotify;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import anton.davila.selfpotify.music.entity.Artist;
import anton.davila.selfpotify.music.repository.ArtistRepository;
import anton.davila.selfpotify.user.entity.User;
import anton.davila.selfpotify.user.feed.entity.UserFeed;
import anton.davila.selfpotify.user.feed.repository.UserFeedRepository;
import anton.davila.selfpotify.user.feed.service.UserFeedService;
import anton.davila.selfpotify.user.listen.repository.UserSongListenRepository;
import anton.davila.selfpotify.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

/**
 * Verifica las reglas del feed del home:
 * <ul>
 *   <li>si no hay ninguna escucha en el servidor, se recomiendan todos los
 *       artistas a todos los usuarios;</li>
 *   <li>lo mismo si el usuario no tiene escuchas propias;</li>
 *   <li>con historial, se priorizan los artistas más escuchados (global) de los
 *       géneros que el usuario ha estado escuchando, rellenando con popularidad
 *       global.</li>
 * </ul>
 * Los repositorios están mockeados, así que no hay BBDD ni red.
 */
@SpringBootTest
public class UserFeedServiceTest {

    @Autowired
    private UserFeedService service;

    @MockitoBean
    private UserSongListenRepository listenRepository;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private UserFeedRepository userFeedRepository;

    @MockitoBean
    private ArtistRepository artistRepository;

    private User user;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);
        user.setUsername("alice");
        user.setUserFeed(new UserFeed());
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        // por defecto: no hay escuchas ni popularidad; cada test configura lo suyo.
        when(listenRepository.findArtistsByGenreOrderByGlobalListensDesc(anyString(), any(Pageable.class)))
                .thenReturn(new ArrayList<>());
        when(listenRepository.findArtistsByGlobalListensDesc(any(Pageable.class)))
                .thenReturn(new ArrayList<>());
    }

    private Artist artist(long id, String name) {
        Artist a = new Artist();
        a.setId(id);
        a.setName(name);
        return a;
    }

    private List<Artist> artists(int from, int count) {
        return IntStream.range(from, from + count)
                .mapToObj(i -> artist(i, "Artist " + i))
                .toList();
    }

    @Test
    void noListensInServer_recommendsAllArtists() {
        // ninguna escucha en todo el servidor (count() == 0 por defecto)
        List<Artist> all = new ArrayList<>(artists(1, 25));
        when(artistRepository.findAll()).thenReturn(all);

        List<Artist> rec = service.recommendArtistsForUser(1L);

        // se devuelven TODOS los artistas, sin el límite de FEED_SIZE
        assertEquals(25, rec.size());
        assertEquals(all.stream().map(Artist::getId).toList(),
                rec.stream().map(Artist::getId).toList());
        verify(artistRepository).findAll();
    }

    @Test
    void userWithoutListens_recommendsAllArtists() {
        // hay escuchas en el servidor, pero el usuario no tiene ninguna propia
        when(listenRepository.count()).thenReturn(42L);
        when(listenRepository.countByUser_Id(1L)).thenReturn(0L);
        List<Artist> all = new ArrayList<>(artists(1, 15));
        when(artistRepository.findAll()).thenReturn(all);

        List<Artist> rec = service.recommendArtistsForUser(1L);

        assertEquals(15, rec.size());
        verify(artistRepository).findAll();
        // no se mira la popularidad por género ni global en este caso
        verify(listenRepository, never())
                .findArtistsByGenreOrderByGlobalListensDesc(anyString(), any(Pageable.class));
    }

    @Test
    void withHistory_prioritizesTopArtistsOfRecentGenres() {
        when(listenRepository.count()).thenReturn(100L);
        when(listenRepository.countByUser_Id(1L)).thenReturn(20L);
        // géneros recientes (cabeza = más reciente): Reggaeton, Pop
        user.getUserFeed().pushGenero("Pop");
        user.getUserFeed().pushGenero("Reggaeton");
        // artistas más escuchados de cada género
        when(listenRepository.findArtistsByGenreOrderByGlobalListensDesc(eq("Reggaeton"), any(Pageable.class)))
                .thenReturn(new ArrayList<>(artists(1, 4)));
        when(listenRepository.findArtistsByGenreOrderByGlobalListensDesc(eq("Pop"), any(Pageable.class)))
                .thenReturn(new ArrayList<>(artists(5, 4)));
        // catálogo del que salen los 3 artistas aleatorios reservados (ids altos)
        when(artistRepository.findAll()).thenReturn(new ArrayList<>(artists(1, 60)));

        List<Artist> rec = service.recommendArtistsForUser(1L);

        // se reservan 3 huecos aleatorios, así que la parte personalizada llena
        // 7 (FEED_SIZE - 3): primero el género más reciente (Reggaeton 1..4),
        // luego Pop (5..7); el 8 ya no entra.
        assertEquals(List.of(1L, 2L, 3L, 4L, 5L, 6L, 7L),
                rec.subList(0, 7).stream().map(Artist::getId).toList());
        // y 3 artistas aleatorios adicionales del catálogo hasta FEED_SIZE, sin repetir
        assertEquals(10, rec.size());
        assertEquals(rec.size(), rec.stream().map(Artist::getId).distinct().count());
    }

    @Test
    void withHistory_padsWithGlobalPopularityWithoutDuplicates() {
        when(listenRepository.count()).thenReturn(100L);
        when(listenRepository.countByUser_Id(1L)).thenReturn(20L);
        user.getUserFeed().pushGenero("Pop");
        // género aporta 3 artistas (1,2,3)
        when(listenRepository.findArtistsByGenreOrderByGlobalListensDesc(eq("Pop"), any(Pageable.class)))
                .thenReturn(new ArrayList<>(artists(1, 3)));
        // global: 1..3 solapan, 50..58 son nuevos
        List<Artist> global = new ArrayList<>(artists(1, 3));
        global.addAll(artists(50, 9));
        when(listenRepository.findArtistsByGlobalListensDesc(any(Pageable.class)))
                .thenReturn(global);

        List<Artist> rec = service.recommendArtistsForUser(1L);

        assertEquals(10, rec.size());
        // los 3 primeros son los del género, en su orden
        assertEquals(List.of(1L, 2L, 3L),
                rec.subList(0, 3).stream().map(Artist::getId).toList());
        // sin ids repetidos
        long distinct = rec.stream().map(Artist::getId).distinct().count();
        assertEquals(rec.size(), distinct);
    }

    @Test
    void regenerateFeed_existingFeed_overwritesArtistsAndKeepsGenreStack() {
        when(listenRepository.count()).thenReturn(100L);
        when(listenRepository.countByUser_Id(1L)).thenReturn(20L);
        UserFeed feed = user.getUserFeed();
        feed.pushGenero("Pop");
        feed.pushGenero("Reggaeton");
        // sin artistas por género, el feed se rellena con popularidad global
        when(listenRepository.findArtistsByGlobalListensDesc(any(Pageable.class)))
                .thenReturn(new ArrayList<>(artists(100, 5)));

        UserFeed result = service.regenerateFeedForUser(1L);

        assertEquals(5, result.getRecommendedArtists().size());
        // la pila de géneros (historial) no se toca al regenerar
        assertEquals(List.of("Reggaeton", "Pop"), result.getLast20GenresListened());
        // no se crea un feed nuevo si ya existía
        verify(userFeedRepository, never()).save(any(UserFeed.class));
    }

    @Test
    void regenerateFeed_noFeedYet_createsAndAssignsOne() {
        // sin feed y sin escuchas: recae en "todos los artistas"
        user.setUserFeed(null);
        when(userFeedRepository.save(any(UserFeed.class))).thenAnswer(inv -> inv.getArgument(0));
        when(artistRepository.findAll()).thenReturn(new ArrayList<>(artists(100, 4)));

        UserFeed result = service.regenerateFeedForUser(1L);

        assertNotNull(result);
        assertEquals(4, result.getRecommendedArtists().size());
        assertSame(result, user.getUserFeed());
        verify(userFeedRepository).save(any(UserFeed.class));
    }
}
