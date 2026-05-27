package anton.davila.selfpotify;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import anton.davila.selfpotify.music.entity.Song;
import anton.davila.selfpotify.music.repository.SongRepository;
import anton.davila.selfpotify.user.entity.User;
import anton.davila.selfpotify.user.feed.entity.UserFeed;
import anton.davila.selfpotify.user.feed.service.DailyDiscoveryService;
import anton.davila.selfpotify.user.listen.repository.UserSongListenRepository;
import anton.davila.selfpotify.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.Pageable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.StreamSupport;

/**
 * Verifica la composición 3+3+3 de los descubrimientos diarios, su estabilidad
 * dentro del mismo día y los fallbacks de los bloques de género. Es un test
 * unitario puro (sin contexto Spring): el servicio es un POJO y sus
 * repositorios están mockeados con Mockito, así que no hay BBDD ni red.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class DailyDiscoveryServiceTest {

    @InjectMocks
    private DailyDiscoveryService service;

    @Mock
    private SongRepository songRepository;

    @Mock
    private UserSongListenRepository listenRepository;

    @Mock
    private UserRepository userRepository;

    private User user;
    private UserFeed feed;

    @BeforeEach
    void setUp() {
        feed = new UserFeed();
        user = new User();
        user.setId(1L);
        user.setUsername("alice");
        user.setUserFeed(feed);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        // Por defecto todo vacío; cada test configura lo que necesita.
        when(songRepository.findAvailableSongIds()).thenReturn(new ArrayList<>());
        when(songRepository.findAvailableSongIdsByGenre(anyString())).thenReturn(new ArrayList<>());
        when(songRepository.findUnheardSongIdsByGenre(anyLong(), anyString())).thenReturn(new ArrayList<>());
        when(songRepository.findDistinctAvailableGenres()).thenReturn(new ArrayList<>());
        when(listenRepository.findTopGenresByUserListens(anyLong(), any(Pageable.class)))
                .thenReturn(new ArrayList<>());

        // findAllById construye una canción por cada id solicitado.
        when(songRepository.findAllById(anyIterable())).thenAnswer(inv -> {
            Iterable<Long> requested = inv.getArgument(0);
            return StreamSupport.stream(requested.spliterator(), false)
                    .map(DailyDiscoveryServiceTest::song)
                    .toList();
        });
    }

    private static Song song(long id) {
        Song s = new Song();
        s.setId(id);
        return s;
    }

    private static List<Long> ids(long from, int count) {
        List<Long> list = new ArrayList<>();
        for (long i = from; i < from + count; i++) {
            list.add(i);
        }
        return list;
    }

    private static List<Long> idsOf(List<Song> songs) {
        return songs.stream().map(Song::getId).toList();
    }

    @Test
    void composition_threeBuckets_returnNineDistinctSongs() {
        // pila: último género = Pop; el usuario sólo escucha Pop
        feed.setLast20GenresListened(new ArrayList<>(List.of("Pop")));
        when(listenRepository.findTopGenresByUserListens(eq(1L), any(Pageable.class)))
                .thenReturn(new ArrayList<>(List.of("Pop")));
        // catálogo
        when(songRepository.findAvailableSongIds()).thenReturn(ids(1, 20));
        when(songRepository.findUnheardSongIdsByGenre(1L, "Pop")).thenReturn(ids(100, 5));
        when(songRepository.findDistinctAvailableGenres())
                .thenReturn(new ArrayList<>(List.of("Pop", "Rock", "Jazz")));
        when(songRepository.findAvailableSongIdsByGenre("Rock")).thenReturn(ids(200, 5));
        when(songRepository.findAvailableSongIdsByGenre("Jazz")).thenReturn(ids(300, 5));

        List<Song> result = service.getDailyDiscoveries(1L);

        assertEquals(9, result.size());
        assertEquals(9, idsOf(result).stream().distinct().count(), "sin duplicados entre bloques");
    }

    @Test
    void stableWithinDay_twoCallsReturnSameList() {
        feed.setLast20GenresListened(new ArrayList<>(List.of("Pop")));
        when(listenRepository.findTopGenresByUserListens(eq(1L), any(Pageable.class)))
                .thenReturn(new ArrayList<>(List.of("Pop")));
        when(songRepository.findAvailableSongIds()).thenReturn(ids(1, 20));
        when(songRepository.findUnheardSongIdsByGenre(1L, "Pop")).thenReturn(ids(100, 5));
        when(songRepository.findDistinctAvailableGenres())
                .thenReturn(new ArrayList<>(List.of("Pop", "Rock")));
        when(songRepository.findAvailableSongIdsByGenre("Rock")).thenReturn(ids(200, 5));

        List<Long> first = idsOf(service.getDailyDiscoveries(1L));
        List<Long> second = idsOf(service.getDailyDiscoveries(1L));

        assertEquals(first, second, "misma lista y mismo orden el mismo día");
    }

    @Test
    void bucketB_fallsBackToNextGenreInStackWhenHeadHasNoUnheard() {
        // cabeza Salsa sin canciones nuevas; debe caer al siguiente, Reggaeton
        feed.setLast20GenresListened(new ArrayList<>(List.of("Salsa", "Reggaeton")));
        when(listenRepository.findTopGenresByUserListens(eq(1L), any(Pageable.class)))
                .thenReturn(new ArrayList<>(List.of("Salsa", "Reggaeton")));
        when(songRepository.findUnheardSongIdsByGenre(1L, "Salsa")).thenReturn(new ArrayList<>());
        when(songRepository.findUnheardSongIdsByGenre(1L, "Reggaeton")).thenReturn(ids(110, 5));

        List<Song> result = service.getDailyDiscoveries(1L);

        // se consultó el siguiente género de la pila (fallback)
        verify(songRepository).findUnheardSongIdsByGenre(1L, "Reggaeton");
        // y el resultado incluye canciones del género de fallback
        assertTrue(idsOf(result).stream().anyMatch(id -> id >= 110 && id < 115),
                "el bloque del último género se rellenó con el siguiente de la pila");
    }

    @Test
    void bucketC_fallsBackToOldestStackGenreWhenNoUnlistenedGenre() {
        // el usuario escucha todos los géneros del catálogo => no hay "no escuchado"
        feed.setLast20GenresListened(new ArrayList<>(List.of("Pop", "Rock")));
        when(listenRepository.findTopGenresByUserListens(eq(1L), any(Pageable.class)))
                .thenReturn(new ArrayList<>(List.of("Pop", "Rock")));
        when(songRepository.findDistinctAvailableGenres())
                .thenReturn(new ArrayList<>(List.of("Pop", "Rock")));
        // "Rock" es el más antiguo de la pila (último elemento) => fallback de bloque C
        when(songRepository.findAvailableSongIdsByGenre("Rock")).thenReturn(ids(200, 5));

        service.getDailyDiscoveries(1L);

        verify(songRepository).findAvailableSongIdsByGenre("Rock");
    }

    @Test
    void coldStart_noHistory_returnsNineWithoutError() {
        // sin pila ni géneros escuchados; bloque B vacío, bloque C elige cualquier género
        when(songRepository.findAvailableSongIds()).thenReturn(ids(1, 30));
        when(songRepository.findDistinctAvailableGenres())
                .thenReturn(new ArrayList<>(List.of("Pop")));
        when(songRepository.findAvailableSongIdsByGenre("Pop")).thenReturn(ids(100, 5));

        List<Song> result = service.getDailyDiscoveries(1L);

        assertEquals(9, result.size());
        assertEquals(9, idsOf(result).stream().distinct().count());
    }
}
