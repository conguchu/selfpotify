package anton.davila.selfpotify;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

import anton.davila.selfpotify.music.entity.Album;
import anton.davila.selfpotify.music.repository.AlbumRepository;
import anton.davila.selfpotify.music.service.AlbumService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@SpringBootTest
public class AlbumServiceTest {

    @Autowired
    private AlbumService albumService;

    @MockitoBean
    private AlbumRepository albumRepository;

    private Album albumOriginal;

    @BeforeEach
    void setUp() {
        albumOriginal = new Album();
        albumOriginal.setId(1L);
        albumOriginal.setName("A Night at the Opera");
    }

    @Test
    void testAdd() {
        when(albumRepository.save(any(Album.class))).thenReturn(albumOriginal);
        Album result = albumService.add(new Album());
        assertNotNull(result);
        assertEquals("A Night at the Opera", result.getName());
        verify(albumRepository, times(1)).save(any(Album.class));
    }

    @Test
    void testGetAll() {
        when(albumRepository.findAll()).thenReturn(Arrays.asList(albumOriginal));
        List<Album> albums = albumService.getAll();
        assertFalse(albums.isEmpty());
        assertEquals(1, albums.size());
    }

    @Test
    void testGetById() {
        when(albumRepository.findById(1L)).thenReturn(Optional.of(albumOriginal));
        Optional<Album> result = albumService.getById(1L);
        assertTrue(result.isPresent());
        assertEquals("A Night at the Opera", result.get().getName());
    }

    @Test
    void testUpdate_Success() {
        when(albumRepository.findById(1L)).thenReturn(Optional.of(albumOriginal));

        Album updated = albumService.updateMeta(1L, "Updated Album Name", "/assets/covers/x.jpg");
        assertEquals("Updated Album Name", updated.getName());
        assertEquals("/assets/covers/x.jpg", updated.getPicture_url());
    }

    @Test
    void testUpdate_NotFound() {
        when(albumRepository.findById(1L)).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> albumService.updateMeta(1L, "x", null));
    }

    @Test
    void testDelete_Success() {
        when(albumRepository.findById(1L)).thenReturn(Optional.of(albumOriginal));
        Album deleted = albumService.delete(1L);
        assertNotNull(deleted);
        verify(albumRepository, times(1)).delete(albumOriginal);
    }
}
