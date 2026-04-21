package anton.davila.selfpotify;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

import anton.davila.selfpotify.music.entity.Artist;
import anton.davila.selfpotify.music.repository.ArtistRepository;
import anton.davila.selfpotify.music.service.ArtistService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@SpringBootTest
public class ArtistServiceTest {

    @Autowired
    private ArtistService artistService;

    @MockitoBean
    private ArtistRepository artistRepository;

    private Artist artistOriginal;

    @BeforeEach
    void setUp() {
        artistOriginal = new Artist();
        artistOriginal.setId(1L);
        artistOriginal.setName("Queen");
    }

    @Test
    void testAdd() {
        when(artistRepository.save(any(Artist.class))).thenReturn(artistOriginal);
        Artist result = artistService.add(new Artist());
        assertNotNull(result);
        assertEquals("Queen", result.getName());
    }

    @Test
    void testGetAll() {
        when(artistRepository.findAll()).thenReturn(Arrays.asList(artistOriginal));
        List<Artist> artists = artistService.getAll();
        assertEquals(1, artists.size());
    }

    @Test
    void testGetById() {
        when(artistRepository.findById(1L)).thenReturn(Optional.of(artistOriginal));
        Optional<Artist> result = artistService.getById(1L);
        assertTrue(result.isPresent());
        assertEquals("Queen", result.get().getName());
    }

    @Test
    void testUpdate_Success() {
        Artist newData = new Artist();
        newData.setName("Freddie Mercury");
        when(artistRepository.findById(1L)).thenReturn(Optional.of(artistOriginal));
        
        Artist updated = artistService.update(1L, newData);
        assertEquals("Freddie Mercury", updated.getName());
    }

    @Test
    void testDelete_Success() {
        when(artistRepository.findById(1L)).thenReturn(Optional.of(artistOriginal));
        Artist deleted = artistService.delete(1L);
        assertNotNull(deleted);
        verify(artistRepository, times(1)).delete(artistOriginal);
    }
}
