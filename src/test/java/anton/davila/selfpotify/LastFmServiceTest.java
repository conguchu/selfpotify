package anton.davila.selfpotify;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import anton.davila.selfpotify.config.AppProperties;
import anton.davila.selfpotify.music.service.external.LastFmService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public class LastFmServiceTest {

    private RestTemplate restTemplate;
    private AppProperties appProperties;
    private LastFmService lastFmService;

    @BeforeEach
    void setUp() {
        appProperties = new AppProperties();
        appProperties.getLastfm().setApiKey("test-key");
        appProperties.getLastfm().setBaseUrl("https://ws.audioscrobbler.com/2.0/");

        lastFmService = new LastFmService(appProperties);

        // El servicio crea su propio RestTemplate en el constructor; lo sustituimos
        // por un mock para no hacer llamadas HTTP reales.
        restTemplate = mock(RestTemplate.class);
        ReflectionTestUtils.setField(lastFmService, "restTemplate", restTemplate);
    }

    /** Construye la respuesta JSON de track.getInfo con los tags indicados. */
    private Map<String, Object> trackResponse(String... tagNames) {
        return Map.of("track", Map.of("toptags", Map.of("tag", tags(tagNames))));
    }

    /** Construye la respuesta JSON de artist.getTopTags con los tags indicados. */
    private Map<String, Object> artistResponse(String... tagNames) {
        return Map.of("toptags", Map.of("tag", tags(tagNames)));
    }

    private List<Map<String, Object>> tags(String... tagNames) {
        return java.util.Arrays.stream(tagNames)
                .map(n -> Map.<String, Object>of("name", n))
                .toList();
    }

    @Test
    void fetchGenre_apiKeyMissing_returnsEmpty() {
        appProperties.getLastfm().setApiKey("");
        assertTrue(lastFmService.fetchGenre("Queen", "Bohemian Rhapsody").isEmpty());
        verifyNoInteractions(restTemplate);
    }

    @Test
    void fetchGenre_apiKeyNull_returnsEmpty() {
        appProperties.getLastfm().setApiKey(null);
        assertTrue(lastFmService.fetchGenre("Queen", "Bohemian Rhapsody").isEmpty());
        verifyNoInteractions(restTemplate);
    }

    @Test
    void fetchGenre_blankArtist_returnsEmpty() {
        assertTrue(lastFmService.fetchGenre("  ", "Bohemian Rhapsody").isEmpty());
        verifyNoInteractions(restTemplate);
    }

    @Test
    void fetchGenre_nullArtist_returnsEmpty() {
        assertTrue(lastFmService.fetchGenre(null, "Bohemian Rhapsody").isEmpty());
        verifyNoInteractions(restTemplate);
    }

    @Test
    void fetchGenre_trackTagFound_returnsFirstTag() {
        when(restTemplate.getForObject(contains("track.getInfo"), eq(Map.class)))
                .thenReturn(trackResponse("rock", "classic rock"));

        Optional<String> genre = lastFmService.fetchGenre("Queen", "Bohemian Rhapsody");

        assertEquals(Optional.of("rock"), genre);
        verify(restTemplate, never()).getForObject(contains("artist.getTopTags"), any());
    }

    @Test
    void fetchGenre_noTrack_fallsBackToArtistTags() {
        Optional<String> genre = runWithNoTrackThenArtist(artistResponse("pop"));
        assertEquals(Optional.of("pop"), genre);
    }

    @Test
    void fetchGenre_blankTrack_goesStraightToArtistTags() {
        when(restTemplate.getForObject(contains("artist.getTopTags"), eq(Map.class)))
                .thenReturn(artistResponse("jazz"));

        Optional<String> genre = lastFmService.fetchGenre("Queen", "  ");

        assertEquals(Optional.of("jazz"), genre);
        verify(restTemplate, never()).getForObject(contains("track.getInfo"), any());
    }

    @Test
    void fetchGenre_nullTrack_goesStraightToArtistTags() {
        when(restTemplate.getForObject(contains("artist.getTopTags"), eq(Map.class)))
                .thenReturn(artistResponse("metal"));

        Optional<String> genre = lastFmService.fetchGenre("Queen", null);

        assertEquals(Optional.of("metal"), genre);
        verify(restTemplate, never()).getForObject(contains("track.getInfo"), any());
    }

    @Test
    void fetchGenre_trackNodeMissing_fallsBackToArtist() {
        when(restTemplate.getForObject(contains("track.getInfo"), eq(Map.class)))
                .thenReturn(Map.of("error", 6));
        when(restTemplate.getForObject(contains("artist.getTopTags"), eq(Map.class)))
                .thenReturn(artistResponse("indie"));

        assertEquals(Optional.of("indie"),
                lastFmService.fetchGenre("Queen", "Bohemian Rhapsody"));
    }

    @Test
    void fetchGenre_trackToptagsMissing_fallsBackToArtist() {
        when(restTemplate.getForObject(contains("track.getInfo"), eq(Map.class)))
                .thenReturn(Map.of("track", Map.of("name", "x")));
        when(restTemplate.getForObject(contains("artist.getTopTags"), eq(Map.class)))
                .thenReturn(artistResponse("folk"));

        assertEquals(Optional.of("folk"),
                lastFmService.fetchGenre("Queen", "Bohemian Rhapsody"));
    }

    @Test
    void fetchGenre_emptyTrackTagList_fallsBackToArtist() {
        when(restTemplate.getForObject(contains("track.getInfo"), eq(Map.class)))
                .thenReturn(trackResponse());
        when(restTemplate.getForObject(contains("artist.getTopTags"), eq(Map.class)))
                .thenReturn(artistResponse("blues"));

        assertEquals(Optional.of("blues"),
                lastFmService.fetchGenre("Queen", "Bohemian Rhapsody"));
    }

    @Test
    void fetchGenre_blankTagNamesSkipped_returnsFirstNonBlank() {
        when(restTemplate.getForObject(contains("track.getInfo"), eq(Map.class)))
                .thenReturn(trackResponse("   ", "punk"));

        assertEquals(Optional.of("punk"),
                lastFmService.fetchGenre("Queen", "Bohemian Rhapsody"));
    }

    @Test
    void fetchGenre_nullBody_returnsEmpty() {
        when(restTemplate.getForObject(anyString(), eq(Map.class))).thenReturn(null);
        assertTrue(lastFmService.fetchGenre("Queen", "Bohemian Rhapsody").isEmpty());
    }

    @Test
    void fetchGenre_restClientException_returnsEmpty() {
        when(restTemplate.getForObject(anyString(), eq(Map.class)))
                .thenThrow(new RestClientException("network down"));
        assertTrue(lastFmService.fetchGenre("Queen", "Bohemian Rhapsody").isEmpty());
    }

    @Test
    void fetchGenre_malformedJsonCausesClassCast_returnsEmpty() {
        // "track" debería ser un Map; un String provoca ClassCastException,
        // que el servicio captura y trata como ausencia de género.
        when(restTemplate.getForObject(contains("track.getInfo"), eq(Map.class)))
                .thenReturn(Map.of("track", "no-soy-un-mapa"));
        when(restTemplate.getForObject(contains("artist.getTopTags"), eq(Map.class)))
                .thenReturn(null);

        assertTrue(lastFmService.fetchGenre("Queen", "Bohemian Rhapsody").isEmpty());
    }

    @Test
    void fetchGenre_noTagsAnywhere_returnsEmpty() {
        when(restTemplate.getForObject(contains("track.getInfo"), eq(Map.class)))
                .thenReturn(trackResponse());
        when(restTemplate.getForObject(contains("artist.getTopTags"), eq(Map.class)))
                .thenReturn(artistResponse());

        assertTrue(lastFmService.fetchGenre("Queen", "Bohemian Rhapsody").isEmpty());
    }

    /** Helper: simula track sin tags y devuelve la respuesta de artista indicada. */
    private Optional<String> runWithNoTrackThenArtist(Map<String, Object> artistBody) {
        when(restTemplate.getForObject(contains("track.getInfo"), eq(Map.class)))
                .thenReturn(trackResponse());
        when(restTemplate.getForObject(contains("artist.getTopTags"), eq(Map.class)))
                .thenReturn(artistBody);
        return lastFmService.fetchGenre("Queen", "Bohemian Rhapsody");
    }
}
