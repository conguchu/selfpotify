package anton.davila.selfpotify;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

import anton.davila.selfpotify.music.entity.Playlist;
import anton.davila.selfpotify.music.repository.PlaylistRepository;
import anton.davila.selfpotify.music.service.PlaylistService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@SpringBootTest
public class PlaylistServiceTest {

    @Autowired
    private PlaylistService playlistService;

    @MockitoBean
    private PlaylistRepository playlistRepository;

    private Playlist playlistOriginal;

    @BeforeEach
    void setUp() {
        playlistOriginal = new Playlist();
        playlistOriginal.setId(1L);
        playlistOriginal.setPublic(true);
    }

    @Test
    void testAdd() {
        when(playlistRepository.save(any(Playlist.class))).thenReturn(playlistOriginal);
        Playlist result = playlistService.add(new Playlist());
        assertNotNull(result);
        assertTrue(result.isPublic());
    }

    @Test
    void testGetAll() {
        when(playlistRepository.findAll()).thenReturn(Arrays.asList(playlistOriginal));
        List<Playlist> playlists = playlistService.getAll();
        assertEquals(1, playlists.size());
    }

    @Test
    void testUpdate_Success() {
        Playlist newData = new Playlist();
        newData.setPublic(false);
        when(playlistRepository.findById(1L)).thenReturn(Optional.of(playlistOriginal));
        
        Playlist updated = playlistService.update(1L, newData);
        assertFalse(updated.isPublic());
    }

    @Test
    void testDelete_Success() {
        when(playlistRepository.findById(1L)).thenReturn(Optional.of(playlistOriginal));
        Playlist deleted = playlistService.delete(1L);
        assertNotNull(deleted);
        verify(playlistRepository, times(1)).delete(playlistOriginal);
    }
}
