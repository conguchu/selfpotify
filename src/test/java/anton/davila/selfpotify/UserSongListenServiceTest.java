package anton.davila.selfpotify;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

import anton.davila.selfpotify.music.entity.Song;
import anton.davila.selfpotify.music.repository.SongRepository;
import anton.davila.selfpotify.user.entity.User;
import anton.davila.selfpotify.user.listen.entity.UserSongListen;
import anton.davila.selfpotify.user.listen.repository.UserSongListenRepository;
import anton.davila.selfpotify.user.listen.service.UserSongListenService;
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

@SpringBootTest
public class UserSongListenServiceTest {

    @Autowired
    private UserSongListenService service;

    @MockitoBean
    private UserSongListenRepository listenRepository;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private SongRepository songRepository;

    private User user;
    private Song song;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);
        user.setUsername("user");

        song = new Song();
        song.setId(10L);
        song.setTitle("Bohemian Rhapsody");

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(songRepository.findById(10L)).thenReturn(Optional.of(song));
    }

    @Test
    void recordListen_persistsEvent() {
        when(listenRepository.countByUser_Id(1L)).thenReturn(5L);

        service.recordListen(1L, 10L);

        verify(listenRepository, times(1)).save(any(UserSongListen.class));
        verify(listenRepository, never()).deleteAll(anyList());
    }

    @Test
    void recordListen_evictsOldestWhenOverLimit() {
        // 1002 escuchas tras guardar: deben descartarse las 2 más antiguas
        when(listenRepository.countByUser_Id(1L)).thenReturn(1002L);
        List<UserSongListen> oldest = new ArrayList<>(List.of(
                new UserSongListen(user, song), new UserSongListen(user, song)));
        when(listenRepository.findByUser_IdOrderByListenedAtAsc(eq(1L), any(Pageable.class)))
                .thenReturn(oldest);

        service.recordListen(1L, 10L);

        verify(listenRepository, times(1)).save(any(UserSongListen.class));
        verify(listenRepository, times(1)).deleteAll(oldest);
    }

    @Test
    void recordListen_throwsWhenUserNotFound() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> service.recordListen(99L, 10L));
        assertEquals("No se encontró el usuario con ID 99", ex.getMessage());
        verify(listenRepository, never()).save(any(UserSongListen.class));
    }

    @Test
    void recordListen_throwsWhenSongNotFound() {
        when(songRepository.findById(99L)).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> service.recordListen(1L, 99L));
        assertEquals("No se encontró la canción con ID 99", ex.getMessage());
        verify(listenRepository, never()).save(any(UserSongListen.class));
    }
}
