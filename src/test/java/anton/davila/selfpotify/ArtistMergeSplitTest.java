package anton.davila.selfpotify;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import anton.davila.selfpotify.music.entity.Artist;
import anton.davila.selfpotify.music.entity.Song;
import anton.davila.selfpotify.music.repository.ArtistRepository;
import anton.davila.selfpotify.music.service.ArtistResolver;
import anton.davila.selfpotify.music.service.ArtistService;
import anton.davila.selfpotify.music.service.external.CoverApiService;
import anton.davila.selfpotify.user.feed.repository.UserFeedRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Verifica la reasignación de canciones al juntar (superviviente) y separar
 * (resolución por Last.fm) artistas, sin tocar la BBDD real: los repositorios y
 * el resolver van mockeados; la lógica opera sobre entidades en memoria.
 */
@SpringBootTest
public class ArtistMergeSplitTest {

    @Autowired
    private ArtistService artistService;

    @MockitoBean
    private ArtistRepository artistRepository;

    @MockitoBean
    private ArtistResolver artistResolver;

    @MockitoBean
    private UserFeedRepository userFeedRepository;

    // Evita que el relleno de foto al separar haga llamadas de red (Deezer) en test.
    @MockitoBean
    private CoverApiService coverApiService;

    private Artist artist(long id, String name) {
        Artist a = new Artist();
        a.setId(id);
        a.setName(name);
        return a;
    }

    @Test
    void merge_movesSongsToSurvivorAndDeletesOthers() {
        Artist survivor = artist(1L, "El Alfa");
        Artist other = artist(2L, "El alfa");

        Song song = new Song();
        song.setId(10L);
        song.setArtists(new ArrayList<>(List.of(other)));
        other.setSongs(new ArrayList<>(List.of(song)));

        when(artistRepository.findById(1L)).thenReturn(Optional.of(survivor));
        when(artistRepository.findById(2L)).thenReturn(Optional.of(other));
        when(userFeedRepository.findAllByRecommendedArtistId(any())).thenReturn(List.of());

        Artist result = artistService.merge(List.of(1L, 2L), 1L, "El Alfa El Jefe");

        assertEquals(1L, result.getId());
        assertEquals("El Alfa El Jefe", result.getName());
        assertTrue(song.getArtists().stream().anyMatch(a -> a.getId().equals(1L)),
                "la canción debe quedar atribuida al superviviente");
        assertFalse(song.getArtists().stream().anyMatch(a -> a.getId().equals(2L)),
                "la canción ya no debe estar atribuida al absorbido");
        verify(artistRepository, times(1)).delete(other);
        verify(artistRepository, never()).delete(survivor);
    }

    @Test
    void merge_requiresSurvivorAmongIds() {
        assertThrows(ResponseStatusException.class,
                () -> artistService.merge(List.of(1L, 2L), 99L, null));
    }

    @Test
    void split_attributesSongsToAllResolvedArtistsAndDeletesOriginal() {
        Artist source = artist(1L, "Ill Pekeño / Ergo Pro");
        Artist illPekeno = artist(2L, "Ill Pekeño");
        Artist ergoPro = artist(3L, "Ergo Pro");

        Song song = new Song();
        song.setId(20L);
        song.setArtists(new ArrayList<>(List.of(source)));
        source.setSongs(new ArrayList<>(List.of(song)));

        when(artistRepository.findById(1L)).thenReturn(Optional.of(source));
        when(userFeedRepository.findAllByRecommendedArtistId(any())).thenReturn(List.of());
        when(artistResolver.resolve(eq("Ill Pekeño"), any())).thenReturn(illPekeno);
        when(artistResolver.resolve(eq("Ergo Pro"), any())).thenReturn(ergoPro);

        List<Artist> result = artistService.split(1L, List.of("Ill Pekeño", "Ergo Pro"));

        assertEquals(2, result.size());
        assertTrue(song.getArtists().stream().anyMatch(a -> a.getId().equals(2L)));
        assertTrue(song.getArtists().stream().anyMatch(a -> a.getId().equals(3L)));
        assertFalse(song.getArtists().stream().anyMatch(a -> a.getId().equals(1L)));
        verify(artistRepository, times(1)).delete(source);
    }

    @Test
    void split_requiresAtLeastTwoNames() {
        Artist source = artist(1L, "Whatever");
        when(artistRepository.findById(1L)).thenReturn(Optional.of(source));
        assertThrows(ResponseStatusException.class,
                () -> artistService.split(1L, List.of("Solo Uno")));
    }
}
