package anton.davila.selfpotify;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

import anton.davila.selfpotify.music.entity.Song;
import anton.davila.selfpotify.music.repository.SongRepository;
import anton.davila.selfpotify.music.service.SongService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@SpringBootTest
public class SongServiceTest {

    @Autowired
    private SongService songService;

    @MockitoBean
    private SongRepository songRepository;

    private Song songOriginal;

    @BeforeEach
    void setUp() {
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
        verify(songRepository, times(1)).findAll();
    }

    @Test
    void testGetById_Found() {
        when(songRepository.findById(1L)).thenReturn(Optional.of(songOriginal));

        Optional<Song> result = songService.getById(1L);

        assertTrue(result.isPresent());
        assertEquals(1L, result.get().getId());
        verify(songRepository, times(1)).findById(1L);
    }

    @Test
    void testUpdate_Success() {
        Song newData = new Song();
        newData.setTitle("Updated Title");
        newData.setGenre("Pop");

        when(songRepository.findById(1L)).thenReturn(Optional.of(songOriginal));

        Song updatedSong = songService.update(1L, newData);

        assertEquals("Updated Title", updatedSong.getTitle());
        assertEquals("Pop", updatedSong.getGenre());
        verify(songRepository, times(1)).findById(1L);
    }

    @Test
    void testUpdate_NotFound() {
        // Simulamos que NO encuentra la canción
        when(songRepository.findById(1L)).thenReturn(Optional.empty());

        // Verificamos que se lanza la excepción y guardamos la referencia
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            songService.update(1L, new Song());
        });

        // Verificamos que el mensaje de la excepción es exactamente el que esperamos
        assertEquals("No se ha encontrado la cancion con ID 1", exception.getMessage());
        verify(songRepository, times(1)).findById(1L);

        // NOTA: Aquí es donde verás el ERROR en consola. Es el comportamiento correcto.
    }

    @Test
    void testDelete_Success() {
        when(songRepository.findById(1L)).thenReturn(Optional.of(songOriginal));

        Song deleted = songService.delete(1L);

        assertNotNull(deleted);
        assertEquals(1L, deleted.getId());
        verify(songRepository, times(1)).delete(songOriginal);
    }

    @Test
    void testDelete_NotFound() {
        // Simulamos que NO encuentra la canción
        when(songRepository.findById(1L)).thenReturn(Optional.empty());

        // Verificamos que se lanza la excepción
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            songService.delete(1L);
        });

        // Verificamos el mensaje
        assertEquals("No se ha encontrado la cancion con ID 1", exception.getMessage());
        verify(songRepository, never()).delete(any(Song.class)); // Verificamos que NUNCA se llama al delete
    }


    @Test
    void testLoadFolderWithRealFiles() {
        // 1. CONFIGURACIÓN DE RUTA: Escribe aquí la ruta real de tu sistema
        // IMPORTANTE: Recuerda usar barras dobles (\\) en Windows o barra simple (/)
        String miRutaReal = "/Volumes/CONGUCHU/agosto 25";

        // Simulamos el repositorio para que devuelva lo mismo que se intenta guardar
        when(songRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

        // 2. EJECUCIÓN
        List<Song> result = songService.loadFolder(miRutaReal);

        // 3. VERIFICACIÓN
        assertNotNull(result, "La lista resultante no debería ser nula");
        // No comprobamos un tamaño exacto porque dependerá de cuántos archivos tengas en la carpeta real

        // 4. LISTADO BONITO EN CONSOLA
        System.out.println("\n=========================================================================================");
        System.out.println("💿 RESULTADO DEL ESCANEO: Se encontraron " + result.size() + " canciones");
        System.out.println("=========================================================================================");

        for (int i = 0; i < result.size(); i++) {
            Song s = result.get(i);

            // Manejo de nulos por si alguna canción no tiene ese metadato
            String titulo = (s.getTitle() != null) ? s.getTitle() : "Desconocido";
            String genero = (s.getGenre() != null && !s.getGenre().isBlank()) ? s.getGenre() : "N/A";

            // Formateo en columnas
            System.out.printf("%03d. 🎵 %-35s | ⏱️ %-8s ms | 🎸 %-15s | 🥁 %-4s BPM%n",
                    (i + 1),
                    titulo,
                    s.getDuration_ms(),
                    genero,
                    s.getBpm());

            System.out.printf("     📁 %s%n", s.getSongPath());
            System.out.println("-----------------------------------------------------------------------------------------");
        }
        System.out.println("\n");

        // Verificamos que se llamó al guardado en base de datos al menos una vez con la lista entera
        verify(songRepository, times(1)).saveAll(anyList());
    }

}