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

    /** Identidad canónica de un artista resuelta contra Last.fm. */
    public record ArtistIdentity(String name, String mbid) {}

    /**
     * Resuelve el nombre canónico y el MBID (MusicBrainz ID) de un artista
     * usando {@code artist.getInfo} con autocorrección de Last.fm.
     * <p>
     * Sirve para unificar variantes de escritura del mismo artista durante el
     * escaneo: mayúsculas, acentos, espacios y erratas conocidas se normalizan,
     * y el MBID devuelto es un identificador estable con el que emparejar.
     *
     * @param name nombre del artista tal como aparece en los metadatos (ya limpiado)
     * @return identidad canónica si Last.fm reconoce al artista; vacío en caso contrario
     */
    @SuppressWarnings("unchecked")
    public Optional<ArtistIdentity> resolveArtist(String name) {
        String apiKey = appProperties.getLastfm().getApiKey();
        if (apiKey == null || apiKey.isBlank()) {
            log.debug("Last.fm API key no configurada; no se resuelve la identidad del artista.");
            return Optional.empty();
        }
        if (name == null || name.isBlank()) {
            return Optional.empty();
        }
        try {
            String url = UriComponentsBuilder.fromUriString(appProperties.getLastfm().getBaseUrl())
                    .queryParam("method", "artist.getInfo")
                    .queryParam("api_key", apiKey)
                    .queryParam("artist", name)
                    .queryParam("autocorrect", 1)
                    .queryParam("format", "json")
                    .build()
                    .toUriString();

            Map<String, Object> body = restTemplate.getForObject(url, Map.class);
            if (body == null || !(body.get("artist") instanceof Map)) {
                log.debug("Last.fm no reconoció al artista '{}'.", name);
                return Optional.empty();
            }
            Map<String, Object> artistNode = (Map<String, Object>) body.get("artist");
            if (!(artistNode.get("name") instanceof String canonical) || canonical.isBlank()) {
                return Optional.empty();
            }
            String mbid = (artistNode.get("mbid") instanceof String m && !m.isBlank()) ? m : null;
            log.debug("Last.fm resolvió '{}' -> '{}' (mbid={}).", name, canonical, mbid);
            return Optional.of(new ArtistIdentity(canonical.trim(), mbid));
        } catch (RestClientException | ClassCastException e) {
            log.warn("Fallo resolviendo el artista '{}' en Last.fm: {}", name, e.getMessage());
            return Optional.empty();
        }
    }

    private Optional<String> firstTagName(List<Map<String, Object>> tags) {
        if (tags == null || tags.isEmpty()) return Optional.empty();
        for (Map<String, Object> tag : tags) {
            Object name = tag.get("name");
            if (name instanceof String s && !s.isBlank()) {
                return Optional.of(toTitleCase(s));
            }
        }
        return Optional.empty();
    }

    /**
     * Capitaliza la primera letra de cada palabra del género
     * (p. ej. "latin pop" -> "Latin Pop").
     */
    private String toTitleCase(String value) {
        StringBuilder result = new StringBuilder(value.length());
        boolean startOfWord = true;
        for (char c : value.toCharArray()) {
            if (Character.isWhitespace(c)) {
                startOfWord = true;
                result.append(c);
            } else if (startOfWord) {
                result.append(Character.toTitleCase(c));
                startOfWord = false;
            } else {
                result.append(Character.toLowerCase(c));
            }
        }
        return result.toString();
    }
}
