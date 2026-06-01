package anton.davila.selfpotify.music.service;

import anton.davila.selfpotify.music.entity.Artist;
import anton.davila.selfpotify.music.repository.ArtistRepository;
import anton.davila.selfpotify.music.service.external.LastFmService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;

/**
 * Resuelve la entidad {@link Artist} de un nombre crudo, emparejando por la
 * identidad canónica que devuelve Last.fm (nombre + MBID) en lugar de fiarse del
 * string del tag ID3, que es inconsistente entre archivos del mismo artista
 * (emojis, espacios, mayúsculas, alias). Así el mismo artista real no acaba en
 * varias filas distintas.
 *
 * <p>Lógica compartida por las dos vías de incorporación de música para que una
 * canción <b>subida desde el panel</b> resuelva su artista exactamente igual que
 * una <b>escaneada del disco</b>:
 * <ul>
 *   <li>{@code SongService} (escaneo periódico / re-escaneo de carpeta), con una
 *       caché por lote para no repetir llamadas HTTP dentro del mismo escaneo.</li>
 *   <li>{@code SongUploadService} (commit de la subida drag &amp; drop).</li>
 * </ul>
 */
@Slf4j
@Service
public class ArtistResolver {

    private final ArtistRepository artistRepository;
    private final LastFmService lastFmService;

    public ArtistResolver(ArtistRepository artistRepository, LastFmService lastFmService) {
        this.artistRepository = artistRepository;
        this.lastFmService = lastFmService;
    }

    /**
     * Resuelve (o crea) el {@link Artist} de un nombre crudo:
     * <ol>
     *   <li>Se limpia el nombre de adornos ({@link #cleanName}).</li>
     *   <li>Se consulta Last.fm para obtener el nombre canónico y el MBID.</li>
     *   <li>Se empareja primero por MBID y, en su defecto, por nombre. Si una fila
     *       existente no tenía MBID, se le rellena (backfill).</li>
     * </ol>
     * Si Last.fm no está configurado o no reconoce al artista, se cae al
     * emparejamiento por nombre limpio (que ya unifica espacios y mayúsculas).
     *
     * @param rawName nombre tal como viene del tag/usuario
     * @param cache   caché opcional por lote (clave = nombre en minúsculas); puede ser {@code null}
     * @return el artista resuelto, o {@code null} si el nombre queda vacío
     */
    public Artist resolve(String rawName, Map<String, Artist> cache) {
        String cleaned = cleanName(rawName);
        if (cleaned.isBlank()) {
            cleaned = rawName == null ? "" : rawName.trim();
        }
        if (cleaned.isBlank()) {
            return null;
        }
        String cacheKey = cleaned.toLowerCase();
        if (cache != null) {
            Artist cached = cache.get(cacheKey);
            if (cached != null) {
                return cached;
            }
        }

        Optional<LastFmService.ArtistIdentity> identity = lastFmService.resolveArtist(cleaned);
        Artist artist;

        if (identity.isPresent() && identity.get().mbid() != null) {
            String mbid = identity.get().mbid();
            String canonicalName = identity.get().name();
            artist = artistRepository.findByMbid(mbid)
                    .orElseGet(() -> artistRepository.findByNameIgnoreCase(canonicalName)
                            .map(existing -> backfillMbid(existing, mbid))
                            .orElseGet(() -> createArtist(canonicalName, mbid)));
        } else {
            String name = identity.map(LastFmService.ArtistIdentity::name).orElse(cleaned);
            artist = artistRepository.findByNameIgnoreCase(name)
                    .orElseGet(() -> createArtist(name, null));
        }

        if (cache != null) {
            cache.put(cacheKey, artist);
            // También por nombre canónico: otras variantes del mismo lote que
            // resuelvan a este nombre reusan la entrada sin re-consultar Last.fm.
            cache.putIfAbsent(artist.getName().toLowerCase(), artist);
        }
        return artist;
    }

    /**
     * Nombre canónico del artista según Last.fm <b>sin tocar la base de datos</b>.
     * Sirve para previsualizar el nombre ya corregido en el borrador de subida
     * (staging) antes de que el admin confirme y se cree/empareje la entidad.
     * Si Last.fm no responde, devuelve el nombre simplemente limpiado.
     */
    public String canonicalName(String rawName) {
        String cleaned = cleanName(rawName);
        if (cleaned.isBlank()) {
            return rawName == null ? null : rawName.trim();
        }
        return lastFmService.resolveArtist(cleaned)
                .map(LastFmService.ArtistIdentity::name)
                .orElse(cleaned);
    }

    /** Rellena el MBID de un artista existente que aún no lo tenía. */
    private Artist backfillMbid(Artist artist, String mbid) {
        if (artist.getMbid() == null || artist.getMbid().isBlank()) {
            artist.setMbid(mbid);
            log.info("Asignado MBID '{}' al artista existente '{}' (id={}).",
                    mbid, artist.getName(), artist.getId());
            return artistRepository.save(artist);
        }
        return artist;
    }

    private Artist createArtist(String name, String mbid) {
        Artist artist = new Artist();
        artist.setName(name);
        artist.setMbid(mbid);
        log.info("Creando nuevo artista '{}' (mbid={}).", name, mbid);
        return artistRepository.save(artist);
    }

    /**
     * Limpia el nombre del artista de adornos que impiden emparejar variantes:
     * emojis, símbolos y espacios redundantes. Conserva letras, dígitos, espacios
     * y la puntuación habitual en nombres ('&', '-', '.', '\'', '()', '/').
     */
    public String cleanName(String raw) {
        if (raw == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder(raw.length());
        for (int i = 0; i < raw.length(); i++) {
            char c = raw.charAt(i);
            if (Character.isLetterOrDigit(c) || Character.isWhitespace(c) || "&-.'()/".indexOf(c) >= 0) {
                sb.append(c);
            }
        }
        return sb.toString().replaceAll("\\s+", " ").trim();
    }
}
