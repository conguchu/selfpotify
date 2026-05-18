package anton.davila.selfpotify;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import anton.davila.selfpotify.music.entity.Artist;
import anton.davila.selfpotify.music.entity.Song;
import anton.davila.selfpotify.music.repository.SongRepository;
import anton.davila.selfpotify.music.service.external.GenreApiService;
import anton.davila.selfpotify.music.service.external.LastFmService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

@ExtendWith(MockitoExtension.class)
public class GenreApiServiceTest {

    @Mock
    private SongRepository songRepository;

    @Mock
    private LastFmService lastFmService;

    @InjectMocks
    private GenreApiService genreApiService;

    private Song song;

    @BeforeEach
    void setUp() {
        Artist artist = new Artist();
        artist.setId(1L);
        artist.setName("Queen");

        song = new Song();
        song.setId(10L);
        song.setTitle("Bohemian Rhapsody");
        song.setArtists(List.of(artist));
    }

    @Test
    void applyGenreIfMissing_nullSong_returnsNull() {
        assertNull(genreApiService.applyGenreIfMissing(null));
        verifyNoInteractions(lastFmService, songRepository);
    }

    @Test
    void applyGenreIfMissing_alreadyHasGenre_doesNothing() {
        song.setGenre("rock");
        Song result = genreApiService.applyGenreIfMissing(song);
        assertSame(song, result);
        verifyNoInteractions(lastFmService, songRepository);
    }

    @Test
    void applyGenreIfMissing_blankGenre_treatedAsMissing() {
        song.setGenre("   ");
        when(lastFmService.fetchGenre("Queen", "Bohemian Rhapsody"))
                .thenReturn(Optional.of("rock"));
        when(songRepository.save(song)).thenReturn(song);

        Song result = genreApiService.applyGenreIfMissing(song);

        assertEquals("rock", result.getGenre());
        verify(songRepository).save(song);
    }

    @Test
    void applyGenreIfMissing_noArtists_returnsSongUnchanged() {
        song.setArtists(Collections.emptyList());
        Song result = genreApiService.applyGenreIfMissing(song);
        assertSame(song, result);
        assertNull(result.getGenre());
        verifyNoInteractions(lastFmService, songRepository);
    }

    @Test
    void applyGenreIfMissing_nullArtistList_returnsSongUnchanged() {
        song.setArtists(null);
        Song result = genreApiService.applyGenreIfMissing(song);
        assertSame(song, result);
        verifyNoInteractions(lastFmService, songRepository);
    }

    @Test
    void applyGenreIfMissing_firstArtistHasBlankName_returnsSongUnchanged() {
        Artist blank = new Artist();
        blank.setName("  ");
        song.setArtists(List.of(blank));
        Song result = genreApiService.applyGenreIfMissing(song);
        assertSame(song, result);
        verifyNoInteractions(lastFmService, songRepository);
    }

    @Test
    void applyGenreIfMissing_lastFmReturnsEmpty_doesNotPersist() {
        when(lastFmService.fetchGenre("Queen", "Bohemian Rhapsody"))
                .thenReturn(Optional.empty());

        Song result = genreApiService.applyGenreIfMissing(song);

        assertSame(song, result);
        assertNull(result.getGenre());
        verify(songRepository, never()).save(any());
    }

    @Test
    void applyGenreIfMissing_lastFmReturnsGenre_persistsAndReturnsSaved() {
        Song saved = new Song();
        saved.setId(10L);
        saved.setTitle("Bohemian Rhapsody");
        saved.setGenre("rock");

        when(lastFmService.fetchGenre("Queen", "Bohemian Rhapsody"))
                .thenReturn(Optional.of("rock"));
        when(songRepository.save(song)).thenReturn(saved);

        Song result = genreApiService.applyGenreIfMissing(song);

        assertSame(saved, result);
        assertEquals("rock", song.getGenre());
        verify(songRepository).save(song);
    }
}
