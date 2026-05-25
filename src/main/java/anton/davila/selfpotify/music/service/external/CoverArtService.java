package anton.davila.selfpotify.music.service.external;

import anton.davila.selfpotify.config.AppProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Resuelve carátulas de canción/álbum e imágenes de artista contra fuentes
 * KEYLESS (sin API key) y devuelve URLs externas directas (links en la nube).
 *
 * <p><b>Orden de prioridad ("lo más oficial primero").</b>
 * <ul>
 *   <li><b>Carátula de canción / álbum:</b>
 *     <ol>
 *       <li><b>MusicBrainz + Cover Art Archive</b> — la fuente más canónica de
 *           la carátula <em>oficial</em> de un lanzamiento (release MBID →
 *           portada en archive.org). MusicBrainz exige un User-Agent descriptivo
 *           y aplica rate-limit (~1 req/s).</li>
 *       <li><b>iTunes Search API</b> — CDN de Apple, carátula oficial de alta
 *           calidad y muy fiable.</li>
 *       <li><b>Deezer API</b> — CDN de Deezer como último recurso.</li>
 *     </ol>
 *   </li>
 *   <li><b>Imagen de artista:</b> <b>Deezer</b> (campo {@code picture_xl}, CDN de
 *       Deezer). iTunes no expone foto de artista y MusicBrainz no aloja
 *       fotografías, así que Deezer es la mejor opción keyless.</li>
 * </ul>
 *
 * <p>Cada llamada está aislada: si una fuente falla (red, JSON inesperado,
 * rate-limit) se captura y se pasa a la siguiente; si ninguna encuentra nada se
 * devuelve {@link Optional#empty()} y el llamante deja el campo a {@code null}.
 *
 * <p>Vive en {@code service.external} junto a {@link LastFmService} y, al igual
 * que él, crea su propio {@link RestTemplate} (sustituible en tests).
 */
@Slf4j
@Service
public class CoverArtService {

    private static final String ITUNES_SEARCH = "https://itunes.apple.com/search";
    private static final String DEEZER_SEARCH_TRACK = "https://api.deezer.com/search";
    private static final String DEEZER_SEARCH_ARTIST = "https://api.deezer.com/search/artist";
    private static final String MUSICBRAINZ_RELEASE = "https://musicbrainz.org/ws/2/release/";
    private static final String COVER_ART_ARCHIVE = "https://coverartarchive.org/release/";

    private final AppProperties appProperties;
    private RestTemplate restTemplate;

    public CoverArtService(AppProperties appProperties) {
        this.appProperties = appProperties;
        AppProperties.CoverArt cfg = appProperties.getCoverArt();
        // RestTemplate propio con timeouts (igual filosofía que LastFmService, que
        // crea el suyo). Sustituible por un mock en tests vía reflexión.
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(cfg.getConnectTimeoutMs());
        factory.setReadTimeout(cfg.getReadTimeoutMs());
        this.restTemplate = new RestTemplate(factory);
    }

    private boolean disabled() {
        if (!appProperties.getCoverArt().isEnabled()) {
            log.debug("CoverArtService deshabilitado (app.cover-art.enabled=false).");
            return true;
        }
        return false;
    }

    // =====================================================================
    // ----- API pública
    // =====================================================================

    /**
     * Carátula de una canción a partir de artista + título (y álbum si se conoce).
     * Recorre, en orden, Cover Art Archive (vía MusicBrainz), iTunes y Deezer.
     */
    public Optional<String> fetchTrackCover(String artist, String track, String album) {
        if (disabled() || isBlank(artist)) return Optional.empty();

        Optional<String> mb = fetchCoverArtArchive(artist, album, track);
        if (mb.isPresent()) return mb;

        Optional<String> itunes = fetchItunesCover(artist, album, track);
        if (itunes.isPresent()) return itunes;

        return fetchDeezerTrackCover(artist, track, album);
    }

    /**
     * Carátula de un álbum a partir de artista + nombre del álbum. Mismo orden
     * de prioridad que {@link #fetchTrackCover}, pero buscando por álbum.
     */
    public Optional<String> fetchAlbumCover(String artist, String album) {
        if (disabled() || isBlank(artist) || isBlank(album)) return Optional.empty();

        Optional<String> mb = fetchCoverArtArchive(artist, album, null);
        if (mb.isPresent()) return mb;

        Optional<String> itunes = fetchItunesCover(artist, album, null);
        if (itunes.isPresent()) return itunes;

        return fetchDeezerTrackCover(artist, album, album);
    }

    /** Imagen de un artista (Deezer {@code picture_xl}). */
    public Optional<String> fetchArtistImage(String artist) {
        if (disabled() || isBlank(artist)) return Optional.empty();
        return fetchDeezerArtistImage(artist);
    }

    // =====================================================================
    // ----- MusicBrainz + Cover Art Archive (carátula oficial del release)
    // =====================================================================

    @SuppressWarnings("unchecked")
    private Optional<String> fetchCoverArtArchive(String artist, String album, String track) {
        // 1) Encontrar un release MBID en MusicBrainz.
        String query = buildMusicBrainzQuery(artist, album, track);
        if (query == null) return Optional.empty();

        try {
            String url = UriComponentsBuilder.fromUriString(MUSICBRAINZ_RELEASE)
                    .queryParam("query", query)
                    .queryParam("fmt", "json")
                    .queryParam("limit", 5)
                    .encode()
                    .build()
                    .toUriString();

            Map<String, Object> body = getForMap(url, true);
            if (body == null) return Optional.empty();

            Object releasesObj = body.get("releases");
            if (!(releasesObj instanceof List<?> releases) || releases.isEmpty()) {
                return Optional.empty();
            }

            // 2) Para cada release candidato, pedir su portada al Cover Art Archive.
            for (Object r : releases) {
                if (!(r instanceof Map<?, ?> release)) continue;
                Object id = release.get("id");
                if (!(id instanceof String mbid) || mbid.isBlank()) continue;

                Optional<String> cover = fetchCoverArtArchiveFront(mbid);
                if (cover.isPresent()) return cover;
            }
            return Optional.empty();
        } catch (RestClientException | ClassCastException e) {
            log.warn("Fallo consultando MusicBrainz para '{} - {}': {}", artist, album, e.getMessage());
            return Optional.empty();
        }
    }

    @SuppressWarnings("unchecked")
    private Optional<String> fetchCoverArtArchiveFront(String mbid) {
        try {
            // El Cover Art Archive devuelve 404 si no hay carátula para ese release.
            Map<String, Object> body = getForMap(COVER_ART_ARCHIVE + mbid, false);
            if (body == null) return Optional.empty();

            Object imagesObj = body.get("images");
            if (!(imagesObj instanceof List<?> images)) return Optional.empty();

            for (Object img : images) {
                if (!(img instanceof Map<?, ?> image)) continue;
                Object front = image.get("front");
                if (Boolean.TRUE.equals(front)) {
                    Optional<String> url = preferredCaaUrl(image);
                    if (url.isPresent()) return url;
                }
            }
            // Si ninguna está marcada como "front", usa la primera disponible.
            if (!images.isEmpty() && images.get(0) instanceof Map<?, ?> first) {
                return preferredCaaUrl(first);
            }
            return Optional.empty();
        } catch (RestClientException | ClassCastException e) {
            log.debug("Cover Art Archive sin portada para release {}: {}", mbid, e.getMessage());
            return Optional.empty();
        }
    }

    /** Prefiere una miniatura razonable (500px) y cae a la imagen original. */
    @SuppressWarnings("unchecked")
    private Optional<String> preferredCaaUrl(Map<?, ?> image) {
        Object thumbs = image.get("thumbnails");
        if (thumbs instanceof Map<?, ?> thumbnails) {
            Object large = thumbnails.get("500");
            if (large instanceof String s && !s.isBlank()) return Optional.of(toHttps(s));
            Object small = thumbnails.get("large");
            if (small instanceof String s && !s.isBlank()) return Optional.of(toHttps(s));
        }
        Object full = image.get("image");
        if (full instanceof String s && !s.isBlank()) return Optional.of(toHttps(s));
        return Optional.empty();
    }

    private String buildMusicBrainzQuery(String artist, String album, String track) {
        // Lucene: artista siempre; álbum (release) si se conoce, si no el título del track.
        StringBuilder q = new StringBuilder();
        q.append("artist:\"").append(escapeLucene(artist)).append('"');
        if (!isBlank(album)) {
            q.append(" AND release:\"").append(escapeLucene(album)).append('"');
        } else if (!isBlank(track)) {
            q.append(" AND release:\"").append(escapeLucene(track)).append('"');
        } else {
            return null; // sólo artista no basta para identificar un release concreto
        }
        return q.toString();
    }

    // =====================================================================
    // ----- iTunes Search API
    // =====================================================================

    @SuppressWarnings("unchecked")
    private Optional<String> fetchItunesCover(String artist, String album, String track) {
        String term = artist + " " + (isBlank(album) ? track : album);
        if (isBlank(term)) return Optional.empty();
        try {
            String url = UriComponentsBuilder.fromUriString(ITUNES_SEARCH)
                    .queryParam("term", term)
                    .queryParam("entity", isBlank(album) ? "musicTrack" : "album")
                    .queryParam("limit", 1)
                    .encode()
                    .build()
                    .toUriString();

            Map<String, Object> body = getForMap(url, false);
            if (body == null) return Optional.empty();

            Object resultsObj = body.get("results");
            if (!(resultsObj instanceof List<?> results) || results.isEmpty()) {
                return Optional.empty();
            }
            if (!(results.get(0) instanceof Map<?, ?> first)) return Optional.empty();

            Object art = first.get("artworkUrl100");
            if (art instanceof String s && !s.isBlank()) {
                // iTunes devuelve 100x100; subimos la resolución sustituyendo el sufijo.
                return Optional.of(s.replace("100x100bb", "600x600bb"));
            }
            return Optional.empty();
        } catch (RestClientException | ClassCastException e) {
            log.warn("Fallo consultando iTunes para '{} - {}': {}", artist, term, e.getMessage());
            return Optional.empty();
        }
    }

    // =====================================================================
    // ----- Deezer API
    // =====================================================================

    @SuppressWarnings("unchecked")
    private Optional<String> fetchDeezerTrackCover(String artist, String track, String album) {
        try {
            String q = "artist:\"" + artist + "\""
                    + (isBlank(album) ? "" : " album:\"" + album + "\"")
                    + (isBlank(track) ? "" : " track:\"" + track + "\"");
            String url = UriComponentsBuilder.fromUriString(DEEZER_SEARCH_TRACK)
                    .queryParam("q", q)
                    .queryParam("limit", 1)
                    .encode()
                    .build()
                    .toUriString();

            Map<String, Object> body = getForMap(url, false);
            List<?> data = dataList(body);
            if (data == null || data.isEmpty() || !(data.get(0) instanceof Map<?, ?> first)) {
                return Optional.empty();
            }
            Object albumNode = first.get("album");
            if (albumNode instanceof Map<?, ?> albumMap) {
                Object cover = firstNonBlank(albumMap, "cover_xl", "cover_big", "cover_medium", "cover");
                if (cover != null) return Optional.of(cover.toString());
            }
            return Optional.empty();
        } catch (RestClientException | ClassCastException e) {
            log.warn("Fallo consultando Deezer (track) para '{} - {}': {}", artist, track, e.getMessage());
            return Optional.empty();
        }
    }

    @SuppressWarnings("unchecked")
    private Optional<String> fetchDeezerArtistImage(String artist) {
        try {
            String url = UriComponentsBuilder.fromUriString(DEEZER_SEARCH_ARTIST)
                    .queryParam("q", artist)
                    .queryParam("limit", 1)
                    .encode()
                    .build()
                    .toUriString();

            Map<String, Object> body = getForMap(url, false);
            List<?> data = dataList(body);
            if (data == null || data.isEmpty() || !(data.get(0) instanceof Map<?, ?> first)) {
                return Optional.empty();
            }
            Object pic = firstNonBlank(first, "picture_xl", "picture_big", "picture_medium", "picture");
            return pic == null ? Optional.empty() : Optional.of(pic.toString());
        } catch (RestClientException | ClassCastException e) {
            log.warn("Fallo consultando Deezer (artista) para '{}': {}", artist, e.getMessage());
            return Optional.empty();
        }
    }

    // =====================================================================
    // ----- Utilidades
    // =====================================================================

    /**
     * GET que devuelve el body como Map. Si {@code withUserAgent} es true (sólo
     * MusicBrainz lo exige) añade la cabecera User-Agent configurada.
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> getForMap(String url, boolean withUserAgent) {
        if (!withUserAgent) {
            return restTemplate.getForObject(url, Map.class);
        }
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.USER_AGENT, appProperties.getCoverArt().getUserAgent());
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        RequestEntity<Void> req = new RequestEntity<>(headers, HttpMethod.GET, URI.create(url));
        ResponseEntity<Map> resp = restTemplate.exchange(req, Map.class);
        return resp.getBody();
    }

    @SuppressWarnings("unchecked")
    private List<?> dataList(Map<String, Object> body) {
        if (body == null) return null;
        Object data = body.get("data");
        return data instanceof List<?> list ? list : null;
    }

    private Object firstNonBlank(Map<?, ?> map, String... keys) {
        for (String key : keys) {
            Object v = map.get(key);
            if (v instanceof String s && !s.isBlank()) return v;
        }
        return null;
    }

    private static String toHttps(String url) {
        return url != null && url.startsWith("http://") ? "https://" + url.substring(7) : url;
    }

    private static String escapeLucene(String value) {
        // Escapa los caracteres especiales de Lucene que usa el query de MusicBrainz.
        return value.replaceAll("([+\\-!(){}\\[\\]^\"~*?:\\\\/])", "\\\\$1");
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
