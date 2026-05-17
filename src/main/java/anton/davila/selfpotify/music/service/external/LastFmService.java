package anton.davila.selfpotify.music.service.external;

import anton.davila.selfpotify.config.AppProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Cliente sencillo de la API de Last.fm para resolver el género
 * de una canción a partir de su título y artista.
 */
@Slf4j
@Service
public class LastFmService {

    private final RestTemplate restTemplate;
    private final AppProperties appProperties;

    public LastFmService(AppProperties appProperties) {
        this.appProperties = appProperties;
        this.restTemplate = new RestTemplate();
    }

    /**
     * Devuelve el primer tag (género) asociado a la canción en Last.fm.
     * Intenta primero track.getInfo y, si no hay tags útiles, cae a artist.getTopTags.
     */
    public Optional<String> fetchGenre(String artist, String track) {
        String apiKey = appProperties.getLastfm().getApiKey();
        if (apiKey == null || apiKey.isBlank()) {
            log.warn("Last.fm API key no configurada (app.lastfm.api-key). Se omite la búsqueda de género.");
            return Optional.empty();
        }
        if (artist == null || artist.isBlank()) {
            log.debug("No se puede consultar Last.fm sin artista.");
            return Optional.empty();
        }

        Optional<String> trackGenre = fetchTrackGenre(apiKey, artist, track);
        if (trackGenre.isPresent()) {
            return trackGenre;
        }
        return fetchArtistGenre(apiKey, artist);
    }

    @SuppressWarnings("unchecked")
    private Optional<String> fetchTrackGenre(String apiKey, String artist, String track) {
        if (track == null || track.isBlank()) {
            return Optional.empty();
        }
        try {
            String url = UriComponentsBuilder.fromUriString(appProperties.getLastfm().getBaseUrl())
                    .queryParam("method", "track.getInfo")
                    .queryParam("api_key", apiKey)
                    .queryParam("artist", artist)
                    .queryParam("track", track)
                    .queryParam("autocorrect", 1)
                    .queryParam("format", "json")
                    .build()
                    .toUriString();

            Map<String, Object> body = restTemplate.getForObject(url, Map.class);
            if (body == null) return Optional.empty();

            Map<String, Object> trackNode = (Map<String, Object>) body.get("track");
            if (trackNode == null) return Optional.empty();

            Map<String, Object> topTags = (Map<String, Object>) trackNode.get("toptags");
            if (topTags == null) return Optional.empty();

            return firstTagName((List<Map<String, Object>>) topTags.get("tag"));
        } catch (RestClientException | ClassCastException e) {
            log.warn("Fallo consultando track.getInfo en Last.fm para '{} - {}': {}", artist, track, e.getMessage());
            return Optional.empty();
        }
    }

    @SuppressWarnings("unchecked")
    private Optional<String> fetchArtistGenre(String apiKey, String artist) {
        try {
            String url = UriComponentsBuilder.fromUriString(appProperties.getLastfm().getBaseUrl())
                    .queryParam("method", "artist.getTopTags")
                    .queryParam("api_key", apiKey)
                    .queryParam("artist", artist)
                    .queryParam("autocorrect", 1)
                    .queryParam("format", "json")
                    .build()
                    .toUriString();

            Map<String, Object> body = restTemplate.getForObject(url, Map.class);
            if (body == null) return Optional.empty();

            Map<String, Object> topTags = (Map<String, Object>) body.get("toptags");
            if (topTags == null) return Optional.empty();

            return firstTagName((List<Map<String, Object>>) topTags.get("tag"));
        } catch (RestClientException | ClassCastException e) {
            log.warn("Fallo consultando artist.getTopTags en Last.fm para '{}': {}", artist, e.getMessage());
            return Optional.empty();
        }
    }

    private Optional<String> firstTagName(List<Map<String, Object>> tags) {
        if (tags == null || tags.isEmpty()) return Optional.empty();
        for (Map<String, Object> tag : tags) {
            Object name = tag.get("name");
            if (name instanceof String s && !s.isBlank()) {
                return Optional.of(s);
            }
        }
        return Optional.empty();
    }
}
