package anton.davila.selfpotify;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import anton.davila.selfpotify.music.entity.Album;
import anton.davila.selfpotify.music.entity.Artist;
import anton.davila.selfpotify.music.entity.Song;
import anton.davila.selfpotify.music.repository.AlbumRepository;
import anton.davila.selfpotify.music.repository.ArtistRepository;
import anton.davila.selfpotify.music.repository.SongRepository;
import anton.davila.selfpotify.music.service.external.CoverApiService;
import anton.davila.selfpotify.music.service.external.CoverArtService;
import anton.davila.selfpotify.music.service.external.EmbeddedCoverExtractor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.File;
import java.util.List;
import java.util.Optional;

/**
 * Servicio idempotente de carátulas: mismo espíritu que {@code GenreApiServiceTest}.
 * Todas las fuentes (embebida + online) están mockeadas.
 */
@ExtendWith(MockitoExtension.class)
public class CoverApiServiceTest {

    @Mock private SongRepository songRepository;
    @Mock private AlbumRepository albumRepository;
    @Mock private ArtistRepository artistRepository;
    @Mock private CoverArtService coverArtService;
    @Mock private EmbeddedCoverExtractor embeddedCoverExtractor;

    @InjectMocks private CoverApiService coverApiService;

    private Song song;
    private Artist artist;

    @BeforeEach
    void setUp() {
        artist = new Artist();
        artist.setId(1L);
        artist.setName("Queen");

        song = new Song();
        song.setId(10L);
        song.setTitle("Bohemian Rhapsody");
        song.setSongPath("/music/queen/bohemian.mp3");
        song.setArtists(List.of(artist));
    }

    @Test
    void applyCoverIfMissing_nullSong_returnsNull() {
        assertNull(coverApiService.applyCoverIfMissing(null));
        verifyNoInteractions(coverArtService, embeddedCoverExtractor, songRepository);
    }

    @Test
    void applyCoverIfMissing_embeddedArtwork_wins_andSkipsOnlineForSong() {
        when(embeddedCoverExtractor.extractToAsset(any(File.class)))
                .thenReturn(Optional.of("/assets/covers/abc.png"));

        Song result = coverApiService.applyCoverIfMissing(song);

        assertEquals("/assets/covers/abc.png", result.getPicture_url());
        verify(songRepository).save(song);
        // Con carátula embebida NO se consulta internet para la canción.
        verify(coverArtService, never()).fetchTrackCover(any(), any(), any());
    }

    @Test
    void applyCoverIfMissing_noEmbedded_fallsBackToOnlineTrackCover() {
        when(embeddedCoverExtractor.extractToAsset(any(File.class))).thenReturn(Optional.empty());
        when(coverArtService.fetchTrackCover("Queen", "Bohemian Rhapsody", null))
                .thenReturn(Optional.of("https://cdn/cover.jpg"));

        Song result = coverApiService.applyCoverIfMissing(song);

        assertEquals("https://cdn/cover.jpg", result.getPicture_url());
        verify(songRepository).save(song);
    }

    @Test
    void applyCoverIfMissing_alreadyHasCover_doesNotTouchSong() {
        song.setPicture_url("https://cdn/existing.jpg");

        coverApiService.applyCoverIfMissing(song);

        // No hay álbum ni se pide nada para la canción ya cubierta.
        verify(songRepository, never()).save(any());
        verify(coverArtService, never()).fetchTrackCover(any(), any(), any());
        // Sin embebida necesaria (la canción ya tiene y no hay álbum que cubrir).
        verify(embeddedCoverExtractor, never()).extractToAsset(any());
    }

    @Test
    void applyCoverIfMissing_nothingFound_leavesNull() {
        when(embeddedCoverExtractor.extractToAsset(any(File.class))).thenReturn(Optional.empty());
        when(coverArtService.fetchTrackCover("Queen", "Bohemian Rhapsody", null))
                .thenReturn(Optional.empty());

        Song result = coverApiService.applyCoverIfMissing(song);

        assertNull(result.getPicture_url());
        verify(songRepository, never()).save(any());
    }

    @Test
    void applyCoverIfMissing_albumReusesEmbeddedCover() {
        Album album = new Album();
        album.setId(5L);
        album.setName("A Night at the Opera");
        song.setAlbum(album);

        when(embeddedCoverExtractor.extractToAsset(any(File.class)))
                .thenReturn(Optional.of("/assets/covers/abc.png"));

        coverApiService.applyCoverIfMissing(song);

        assertEquals("/assets/covers/abc.png", song.getPicture_url());
        assertEquals("/assets/covers/abc.png", album.getPicture_url());
        verify(albumRepository).save(album);
        // El álbum reusó la embebida; no se consulta online para el álbum.
        verify(coverArtService, never()).fetchAlbumCover(any(), any());
    }

    @Test
    void applyCoverIfMissing_albumWithoutEmbedded_goesOnline() {
        Album album = new Album();
        album.setId(5L);
        album.setName("A Night at the Opera");
        song.setAlbum(album);
        song.setPicture_url("https://cdn/song.jpg"); // canción ya cubierta

        when(embeddedCoverExtractor.extractToAsset(any(File.class))).thenReturn(Optional.empty());
        when(coverArtService.fetchAlbumCover("Queen", "A Night at the Opera"))
                .thenReturn(Optional.of("https://cdn/album.jpg"));

        coverApiService.applyCoverIfMissing(song);

        assertEquals("https://cdn/album.jpg", album.getPicture_url());
        verify(albumRepository).save(album);
    }

    @Test
    void applyCoverIfMissing_albumAlreadyHasCover_skipped() {
        Album album = new Album();
        album.setId(5L);
        album.setName("A Night at the Opera");
        album.setPicture_url("https://cdn/album-existing.jpg");
        song.setAlbum(album);
        song.setPicture_url("https://cdn/song.jpg");

        coverApiService.applyCoverIfMissing(song);

        verify(albumRepository, never()).save(any());
        verify(coverArtService, never()).fetchAlbumCover(any(), any());
    }

    @Test
    void applyCoverIfMissing_artistImageFilledWhenMissing() {
        song.setPicture_url("https://cdn/song.jpg"); // sólo probamos el artista
        when(coverArtService.fetchArtistImage("Queen"))
                .thenReturn(Optional.of("https://cdn/artist.jpg"));

        coverApiService.applyCoverIfMissing(song);

        assertEquals("https://cdn/artist.jpg", artist.getPicture_path());
        verify(artistRepository).save(artist);
    }

    @Test
    void applyCoverIfMissing_artistAlreadyHasImage_skipped() {
        song.setPicture_url("https://cdn/song.jpg");
        artist.setPicture_path("https://cdn/artist-existing.jpg");

        coverApiService.applyCoverIfMissing(song);

        verify(artistRepository, never()).save(any());
        verify(coverArtService, never()).fetchArtistImage(any());
    }
}
