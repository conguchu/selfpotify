package anton.davila.selfpotify;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import anton.davila.selfpotify.config.AppProperties;
import anton.davila.selfpotify.music.service.external.CoverArtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Resolución de carátulas con HTTP MOCKEADO (sin red). Cubre el orden de
 * prioridad de fuentes, los fallbacks y el toggle de desactivación.
 */
public class CoverArtServiceTest {

    private RestTemplate restTemplate;
    private AppProperties appProperties;
    private CoverArtService service;

    @BeforeEach
    void setUp() {
        appProperties = new AppProperties();
        appProperties.getCoverArt().setEnabled(true);

        service = new CoverArtService(appProperties);
        // El servicio crea su propio RestTemplate; lo sustituimos por un mock.
        restTemplate = mock(RestTemplate.class);
        ReflectionTestUtils.setField(service, "restTemplate", restTemplate);
    }

    // ---------- helpers de respuesta ----------

    /** Respuesta de MusicBrainz con un único release del MBID dado (vía exchange con User-Agent). */
    private void mockMusicBrainz(String mbid) {
        Map<String, Object> body = mbid == null
                ? Map.of("releases", List.of())
                : Map.of("releases", List.of(Map.of("id", mbid)));
        when(restTemplate.exchange(any(RequestEntity.class), eq(Map.class)))
                .thenReturn(ResponseEntity.ok(body));
    }

    private void mockCoverArtArchive(String mbid, String frontUrl) {
        Map<String, Object> body = Map.of("images", List.of(Map.of(
                "front", true,
                "image", frontUrl,
                "thumbnails", Map.of("500", frontUrl))));
        when(restTemplate.getForObject(contains("coverartarchive.org/release/" + mbid), eq(Map.class)))
                .thenReturn(body);
    }

    private void mockItunes(String artworkUrl100) {
        Map<String, Object> body = Map.of("results", List.of(Map.of("artworkUrl100", artworkUrl100)));
        when(restTemplate.getForObject(contains("itunes.apple.com/search"), eq(Map.class)))
                .thenReturn(body);
    }

    private void mockDeezerTrack(String coverXl) {
        Map<String, Object> body = Map.of("data", List.of(Map.of("album", Map.of("cover_xl", coverXl))));
        when(restTemplate.getForObject(contains("api.deezer.com/search?"), eq(Map.class)))
                .thenReturn(body);
    }

    // ---------- pruebas ----------

    @Test
    void fetchTrackCover_disabled_returnsEmptyAndNoHttp() {
        appProperties.getCoverArt().setEnabled(false);
        assertTrue(service.fetchTrackCover("Queen", "Bohemian Rhapsody", "A Night at the Opera").isEmpty());
        verifyNoInteractions(restTemplate);
    }

    @Test
    void fetchTrackCover_blankArtist_returnsEmpty() {
        assertTrue(service.fetchTrackCover("  ", "track", "album").isEmpty());
        verifyNoInteractions(restTemplate);
    }

    @Test
    void fetchTrackCover_prefersCoverArtArchive_overItunesAndDeezer() {
        mockMusicBrainz("mbid-123");
        mockCoverArtArchive("mbid-123", "https://coverartarchive.org/release/mbid-123/front-500.jpg");
        // iTunes/Deezer también responderían, pero no deben consultarse.
        mockItunes("https://is.apple.com/100x100bb.jpg");

        Optional<String> cover = service.fetchTrackCover("Queen", "Bohemian Rhapsody", "A Night at the Opera");

        assertEquals(Optional.of("https://coverartarchive.org/release/mbid-123/front-500.jpg"), cover);
        verify(restTemplate, never()).getForObject(contains("itunes.apple.com"), any());
        verify(restTemplate, never()).getForObject(contains("api.deezer.com"), any());
    }

    @Test
    void fetchTrackCover_noMusicBrainzRelease_fallsBackToItunesUpscaled() {
        mockMusicBrainz(null); // sin releases
        mockItunes("https://is.apple.com/img/100x100bb.jpg");

        Optional<String> cover = service.fetchTrackCover("Queen", "Bohemian Rhapsody", "A Night at the Opera");

        // iTunes 100x100 -> 600x600.
        assertEquals(Optional.of("https://is.apple.com/img/600x600bb.jpg"), cover);
        verify(restTemplate, never()).getForObject(contains("api.deezer.com"), any());
    }

    @Test
    void fetchTrackCover_noMbNoItunes_fallsBackToDeezer() {
        mockMusicBrainz(null);
        when(restTemplate.getForObject(contains("itunes.apple.com/search"), eq(Map.class)))
                .thenReturn(Map.of("results", List.of()));
        mockDeezerTrack("https://e-cdns.deezer.com/cover_xl.jpg");

        Optional<String> cover = service.fetchTrackCover("Queen", "Bohemian Rhapsody", "A Night at the Opera");

        assertEquals(Optional.of("https://e-cdns.deezer.com/cover_xl.jpg"), cover);
    }

    @Test
    void fetchTrackCover_allSourcesEmpty_returnsEmpty() {
        mockMusicBrainz(null);
        when(restTemplate.getForObject(contains("itunes.apple.com/search"), eq(Map.class)))
                .thenReturn(Map.of("results", List.of()));
        when(restTemplate.getForObject(contains("api.deezer.com/search?"), eq(Map.class)))
                .thenReturn(Map.of("data", List.of()));

        assertTrue(service.fetchTrackCover("Nadie", "Nada", "Ningún álbum").isEmpty());
    }

    @Test
    void fetchTrackCover_musicBrainzThrows_fallsBackGracefully() {
        when(restTemplate.exchange(any(RequestEntity.class), eq(Map.class)))
                .thenThrow(new RestClientException("503 rate limited"));
        mockItunes("https://is.apple.com/100x100bb.jpg");

        Optional<String> cover = service.fetchTrackCover("Queen", "Bohemian Rhapsody", "A Night at the Opera");

        assertEquals(Optional.of("https://is.apple.com/600x600bb.jpg"), cover);
    }

    @Test
    void fetchAlbumCover_requiresAlbumName() {
        assertTrue(service.fetchAlbumCover("Queen", "  ").isEmpty());
        verifyNoInteractions(restTemplate);
    }

    @Test
    void fetchAlbumCover_usesCoverArtArchiveFirst() {
        mockMusicBrainz("mbid-album");
        mockCoverArtArchive("mbid-album", "https://coverartarchive.org/release/mbid-album/front-500.jpg");

        assertEquals(Optional.of("https://coverartarchive.org/release/mbid-album/front-500.jpg"),
                service.fetchAlbumCover("Queen", "A Night at the Opera"));
    }

    @Test
    void fetchArtistImage_usesDeezerPictureXl() {
        Map<String, Object> body = Map.of("data", List.of(Map.of(
                "picture_xl", "https://e-cdns.deezer.com/artist/xl.jpg")));
        when(restTemplate.getForObject(contains("api.deezer.com/search/artist"), eq(Map.class)))
                .thenReturn(body);

        assertEquals(Optional.of("https://e-cdns.deezer.com/artist/xl.jpg"),
                service.fetchArtistImage("Queen"));
    }

    @Test
    void fetchArtistImage_disabled_returnsEmpty() {
        appProperties.getCoverArt().setEnabled(false);
        assertTrue(service.fetchArtistImage("Queen").isEmpty());
        verifyNoInteractions(restTemplate);
    }

    @Test
    void fetchArtistImage_emptyDeezerData_returnsEmpty() {
        when(restTemplate.getForObject(contains("api.deezer.com/search/artist"), eq(Map.class)))
                .thenReturn(Map.of("data", List.of()));
        assertTrue(service.fetchArtistImage("Desconocido").isEmpty());
    }

    @Test
    void fetchArtistImage_deezerThrows_returnsEmpty() {
        when(restTemplate.getForObject(contains("api.deezer.com/search/artist"), eq(Map.class)))
                .thenThrow(new RestClientException("network down"));
        assertTrue(service.fetchArtistImage("Queen").isEmpty());
    }
}
