package anton.davila.selfpotify;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import anton.davila.selfpotify.config.AppProperties;
import anton.davila.selfpotify.music.service.external.LastFmService;
import anton.davila.selfpotify.music.service.external.LastFmService.ArtistIdentity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.Optional;

/**
 * Tests unitarios dedicados a {@link LastFmService#resolveArtist(String)} — la
 * pieza que obtiene el nombre canónico y el MBID de un artista.
 * <p>
 * El {@code RestTemplate} interno se sustituye por un mock, de modo que estos
 * tests verifican el <em>parsing</em> de la respuesta JSON sin tocar la red.
 * Los nombres de artista usados (El Alfa, Bad Bunny, Wisin &amp; Yandel) salen
 * de la biblioteca real "Dembow mix".
 */
public class LastFmResolveArtistTest {

    private RestTemplate restTemplate;
    private AppProperties appProperties;
    private LastFmService lastFmService;

    @BeforeEach
    void setUp() {
        appProperties = new AppProperties();
        appProperties.getLastfm().setApiKey("test-key");
        appProperties.getLastfm().setBaseUrl("https://ws.audioscrobbler.com/2.0/");

        lastFmService = new LastFmService(appProperties);
        restTemplate = mock(RestTemplate.class);
        ReflectionTestUtils.setField(lastFmService, "restTemplate", restTemplate);
    }

    /** Respuesta JSON de artist.getInfo con nombre y (opcionalmente) mbid. */
    private Map<String, Object> artistInfo(String name, String mbid) {
        return mbid == null
                ? Map.of("artist", Map.of("name", name))
                : Map.of("artist", Map.of("name", name, "mbid", mbid));
    }

    @Test
    void resolveArtist_apiKeyMissing_returnsEmptyWithoutHttpCall() {
        appProperties.getLastfm().setApiKey("");
        assertTrue(lastFmService.resolveArtist("El Alfa").isEmpty());
        verifyNoInteractions(restTemplate);
    }

    @Test
    void resolveArtist_apiKeyNull_returnsEmptyWithoutHttpCall() {
        appProperties.getLastfm().setApiKey(null);
        assertTrue(lastFmService.resolveArtist("El Alfa").isEmpty());
        verifyNoInteractions(restTemplate);
    }

    @Test
    void resolveArtist_blankName_returnsEmptyWithoutHttpCall() {
        assertTrue(lastFmService.resolveArtist("   ").isEmpty());
        assertTrue(lastFmService.resolveArtist(null).isEmpty());
        verifyNoInteractions(restTemplate);
    }

    @Test
    void resolveArtist_validResponse_returnsCanonicalNameAndMbid() {
        String mbid = "f1f0e0a0-0000-4000-8000-000000000001";
        when(restTemplate.getForObject(contains("artist.getInfo"), eq(Map.class)))
                .thenReturn(artistInfo("Bad Bunny", mbid));

        Optional<ArtistIdentity> identity = lastFmService.resolveArtist("bad-bunny");

        assertTrue(identity.isPresent());
        assertEquals("Bad Bunny", identity.get().name());
        assertEquals(mbid, identity.get().mbid());
    }

    @Test
    void resolveArtist_callsArtistGetInfoWithAutocorrect() {
        when(restTemplate.getForObject(anyString(), eq(Map.class)))
                .thenReturn(artistInfo("El Alfa", "mbid-x"));

        lastFmService.resolveArtist("EL ALFA EL JEFE");

        verify(restTemplate).getForObject(
                argThat((String url) -> url.contains("method=artist.getInfo")
                        && url.contains("autocorrect=1")),
                eq(Map.class));
    }

    @Test
    void resolveArtist_mbidMissing_returnsIdentityWithNullMbid() {
        when(restTemplate.getForObject(contains("artist.getInfo"), eq(Map.class)))
                .thenReturn(artistInfo("Chaki Bley", null));

        Optional<ArtistIdentity> identity = lastFmService.resolveArtist("Chaki Bley");

        assertTrue(identity.isPresent());
        assertEquals("Chaki Bley", identity.get().name());
        assertNull(identity.get().mbid(), "mbid ausente debe quedar a null");
    }

    @Test
    void resolveArtist_mbidBlank_returnsIdentityWithNullMbid() {
        when(restTemplate.getForObject(contains("artist.getInfo"), eq(Map.class)))
                .thenReturn(artistInfo("Chaki Bley", "   "));

        Optional<ArtistIdentity> identity = lastFmService.resolveArtist("Chaki Bley");

        assertTrue(identity.isPresent());
        assertNull(identity.get().mbid(), "mbid en blanco debe normalizarse a null");
    }

    @Test
    void resolveArtist_canonicalNameIsTrimmed() {
        when(restTemplate.getForObject(contains("artist.getInfo"), eq(Map.class)))
                .thenReturn(artistInfo("  El Alfa  ", "mbid-y"));

        Optional<ArtistIdentity> identity = lastFmService.resolveArtist("el alfa");

        assertTrue(identity.isPresent());
        assertEquals("El Alfa", identity.get().name());
    }

    @Test
    void resolveArtist_nullBody_returnsEmpty() {
        when(restTemplate.getForObject(anyString(), eq(Map.class))).thenReturn(null);
        assertTrue(lastFmService.resolveArtist("Wisin & Yandel").isEmpty());
    }

    @Test
    void resolveArtist_errorResponseWithoutArtistNode_returnsEmpty() {
        // Last.fm responde {"error":6,"message":"..."} para artistas inexistentes.
        when(restTemplate.getForObject(anyString(), eq(Map.class)))
                .thenReturn(Map.of("error", 6, "message", "The artist you supplied could not be found"));

        assertTrue(lastFmService.resolveArtist("artista-inventado-xyz").isEmpty());
    }

    @Test
    void resolveArtist_artistNodeNotAMap_returnsEmpty() {
        when(restTemplate.getForObject(anyString(), eq(Map.class)))
                .thenReturn(Map.of("artist", "no-soy-un-mapa"));

        assertTrue(lastFmService.resolveArtist("El Alfa").isEmpty());
    }

    @Test
    void resolveArtist_artistNodeWithoutName_returnsEmpty() {
        when(restTemplate.getForObject(anyString(), eq(Map.class)))
                .thenReturn(Map.of("artist", Map.of("mbid", "mbid-sin-nombre")));

        assertTrue(lastFmService.resolveArtist("El Alfa").isEmpty());
    }

    @Test
    void resolveArtist_blankCanonicalName_returnsEmpty() {
        when(restTemplate.getForObject(anyString(), eq(Map.class)))
                .thenReturn(Map.of("artist", Map.of("name", "   ", "mbid", "mbid-z")));

        assertTrue(lastFmService.resolveArtist("El Alfa").isEmpty());
    }

    @Test
    void resolveArtist_restClientException_returnsEmpty() {
        when(restTemplate.getForObject(anyString(), eq(Map.class)))
                .thenThrow(new RestClientException("network down"));

        assertTrue(lastFmService.resolveArtist("El Alfa").isEmpty());
    }
}
