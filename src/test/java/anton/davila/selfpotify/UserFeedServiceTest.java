package anton.davila.selfpotify;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import anton.davila.selfpotify.music.entity.Artist;
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
 * Verifica que el feed del home se personaliza con el historial propio del
 * usuario y que un usuario sin historial recae en la popularidad global
 * (cold-start). Los repositorios de escuchas/usuarios están mockeados, así que
 * no hay BBDD ni red.
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

    private User user;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);
        user.setUsername("alice");
        user.setUserFeed(new UserFeed());
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        // por defecto, el usuario no tiene historial ni hay popularidad global;
        // cada test configura lo que necesita.
        when(listenRepository.findTopArtistsByUserListens(anyLong(), any(Pageable.class)))
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
    void coldStart_userWithoutHistory_getsGlobalPopularity() {
        // sin historial propio; popularidad global con 10 artistas
        when(listenRepository.findArtistsByGlobalListensDesc(any(Pageable.class)))
                .thenReturn(new ArrayList<>(artists(100, 10)));

        List<Artist> rec = service.recommendArtistsForUser(1L);

        assertEquals(10, rec.size());
        assertEquals(100L, rec.get(0).getId());
        // no debía mirar nada que no fuese el ranking global como fallback
        verify(listenRepository).findArtistsByGlobalListensDesc(any(Pageable.class));
    }

    @Test
    void personalized_usesUserOwnTopArtistsFirst() {
        // el usuario tiene 10 artistas propios: el feed es exactamente ese, en orden
        List<Artist> own = new ArrayList<>(artists(1, 10));
        when(listenRepository.findTopArtistsByUserListens(eq(1L), any(Pageable.class)))
                .thenReturn(own);

        List<Artist> rec = service.recommendArtistsForUser(1L);

        assertEquals(10, rec.size());
        assertEquals(own.stream().map(Artist::getId).toList(),
                rec.stream().map(Artist::getId).toList());
        // con el feed ya lleno por historial, no hace falta el ranking global
        verify(listenRepository, never()).findArtistsByGlobalListensDesc(any(Pageable.class));
    }

    @Test
    void partialHistory_isPaddedWithGlobalPopularityWithoutDuplicates() {
        // 3 artistas propios (ids 1,2,3) + global (incluye 2 y 3, que no deben repetirse)
        when(listenRepository.findTopArtistsByUserListens(eq(1L), any(Pageable.class)))
                .thenReturn(new ArrayList<>(artists(1, 3)));
        // global: 1..3 solapan con el historial, 50..58 son nuevos
        List<Artist> global = new ArrayList<>(artists(1, 3));
        global.addAll(artists(50, 9));
        when(listenRepository.findArtistsByGlobalListensDesc(any(Pageable.class)))
                .thenReturn(global);

        List<Artist> rec = service.recommendArtistsForUser(1L);

        assertEquals(10, rec.size());
        // los 3 primeros son los del historial, en su orden
        assertEquals(List.of(1L, 2L, 3L),
                rec.subList(0, 3).stream().map(Artist::getId).toList());
        // sin ids repetidos
        long distinct = rec.stream().map(Artist::getId).distinct().count();
        assertEquals(rec.size(), distinct);
    }

    @Test
    void regenerateFeed_existingFeed_overwritesArtistsAndKeepsGenreStack() {
        UserFeed feed = user.getUserFeed();
        feed.pushGenero("Pop");
        feed.pushGenero("Reggaeton");
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
        user.setUserFeed(null);
        when(userFeedRepository.save(any(UserFeed.class))).thenAnswer(inv -> inv.getArgument(0));
        when(listenRepository.findArtistsByGlobalListensDesc(any(Pageable.class)))
                .thenReturn(new ArrayList<>(artists(100, 4)));

        UserFeed result = service.regenerateFeedForUser(1L);

        assertNotNull(result);
        assertEquals(4, result.getRecommendedArtists().size());
        assertSame(result, user.getUserFeed());
        verify(userFeedRepository).save(any(UserFeed.class));
    }
}
