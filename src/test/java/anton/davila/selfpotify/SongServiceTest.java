package anton.davila.selfpotify;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

import anton.davila.selfpotify.entity.music.Song;
import anton.davila.selfpotify.repository.SongRepository;
import anton.davila.selfpotify.service.SongService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@SpringBootTest
public class SongServiceTest {

    @Autowired
    private SongService songService; // El servicio que estamos probando

    @MockitoBean
    private SongRepository songRepository; // Simulamos el repositorio

    private Song songOriginal;

    @BeforeEach
    void setUp() {
        // Inicializamos un objeto de prueba antes de cada test
        songOriginal = new Song();
        songOriginal.setId(1L);
        songOriginal.setTitle("Bohemian Rhapsody");
        songOriginal.setGenre("Rock");
    }

    @Test
    void testAdd() {
        when(songRepository.save(any(Song.class))).thenReturn(songOriginal);

        Song result = songService.add(new Song());

        assertNotNull(result);
        assertEquals("Bohemian Rhapsody", result.getTitle());
        verify(songRepository, times(1)).save(any(Song.class));
    }

    @Test
    void testGetAll() {
        when(songRepository.findAll()).thenReturn(Arrays.asList(songOriginal));

        List<Song> songs = songService.getAll();

        assertFalse(songs.isEmpty());
        assertEquals(1, songs.size());
    }

    @Test
    void testGetById_Found() {
        when(songRepository.findById(1L)).thenReturn(Optional.of(songOriginal));

        Optional<Song> result = songService.getById(1L);

        assertTrue(result.isPresent());
        assertEquals(1L, result.get().getId());
    }

    @Test
    void testUpdate_Success() {
        // Datos nuevos para actualizar
        Song newData = new Song();
        newData.setTitle("Updated Title");
        newData.setGenre("Pop");

        when(songRepository.findById(1L)).thenReturn(Optional.of(songOriginal));

        // Ejecutamos el update
        Song updatedSong = songService.update(1L, newData);

        // Verificamos que el objeto original cambió sus valores
        assertEquals("Updated Title", updatedSong.getTitle());
        assertEquals("Pop", updatedSong.getGenre());
        // Como usas @Transactional y Dirty Checking, no hace falta verificar songRepository.save()
    }

    @Test
    void testUpdate_NotFound() {
        when(songRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> {
            songService.update(1L, new Song());
        });
    }

    @Test
    void testDelete_Success() {
        when(songRepository.findById(1L)).thenReturn(Optional.of(songOriginal));

        Song deleted = songService.delete(1L);

        assertNotNull(deleted);
        verify(songRepository, times(1)).delete(songOriginal);
    }

    @Test
    void testDelete_NotFound() {
        when(songRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> {
            songService.delete(1L);
        });
    }
}