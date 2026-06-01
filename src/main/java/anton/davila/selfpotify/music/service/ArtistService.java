package anton.davila.selfpotify.music.service;

import anton.davila.selfpotify.music.entity.Album;
import anton.davila.selfpotify.music.entity.Artist;
import anton.davila.selfpotify.music.entity.Song;
import anton.davila.selfpotify.music.repository.ArtistRepository;
import anton.davila.selfpotify.user.feed.entity.UserFeed;
import anton.davila.selfpotify.user.feed.repository.UserFeedRepository;
import anton.davila.selfpotify.user.listen.repository.UserSongListenRepository;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
public class ArtistService {

    @Autowired
    private ArtistRepository artistRepository;

    @Autowired
    private UserSongListenRepository userSongListenRepository;

    @Autowired
    private UserFeedRepository userFeedRepository;

    @Autowired
    private ArtistResolver artistResolver;

    public Artist add(Artist a) {
        log.info("Añadiendo nuevo artista: {}", a.getName());
        return artistRepository.save(a);
    }

    public List<Artist> getAll() {
        log.info("Recuperando todos los artistas");
        return artistRepository.findAll();
    }

    public Optional<Artist> getById(long id) {
        log.info("Buscando artista por ID: {}", id);
        return artistRepository.findById(id);
    }

    @Transactional
    public Artist update(long id, Artist artistData) {
        log.info("Actualizando artista con ID: {}", id);
        Artist artist = artistRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("No se encontró el artista con ID " + id));
        artist.copy(artistData);
        return artist;
    }

    @Transactional
    public Artist delete(long id) {
        log.warn("Eliminando artista con ID: {}", id);
        Artist artist = artistRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("No se encontró el artista con ID " + id));
        // Soltar las FKs (song_artist, album_artist, feed) antes de borrar la fila.
        // Las canciones/álbumes no se borran: solo dejan de atribuirse a este artista.
        reassign(artist, List.of());
        detachFromFeeds(artist);
        artistRepository.delete(artist);
        return artist;
    }

    /**
     * Top 10 canciones del artista ordenadas por escuchas globales (desc),
     * derivadas de la tabla de eventos {@code user_song_listen}.
     *
     * @param id id del artista
     * @return como máximo 10 canciones del artista, de más a menos escuchadas
     */
    public List<Song> getTop10SongsById(Long id) {
        log.info("Recuperando top 10 canciones del artista con ID: {}", id);
        artistRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("No se encontró el artista con ID " + id));
        return userSongListenRepository.findSongsByArtistOrderByGlobalListensDesc(id, PageRequest.of(0, 10));
    }

    // =====================================================================
    // ----- Separar y juntar artistas (limpieza de duplicados/etiquetas)
    // =====================================================================

    /**
     * Separa un artista mal etiquetado (p. ej. "Ill Pekeño / Ergo Pro") en los
     * artistas reales cuyos nombres indica el administrador. Cada nombre se
     * resuelve contra Last.fm con el mismo {@link ArtistResolver} que usa el
     * escaneo (nombre canónico + MBID, reusando un artista existente si ya lo
     * había). Todas las canciones y álbumes del artista original se atribuyen a
     * <b>todos</b> los resultantes y, después, el original se elimina.
     *
     * @param sourceId id del artista a separar
     * @param names    nombres de los artistas reales (mínimo dos)
     * @return los artistas resultantes (resueltos o reutilizados)
     */
    @Transactional
    public List<Artist> split(long sourceId, List<String> names) {
        Artist source = artistRepository.findById(sourceId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "No se encontró el artista con ID " + sourceId));

        List<String> cleanedNames = names == null ? List.of() : names.stream()
                .filter(n -> n != null && !n.isBlank())
                .map(String::trim)
                .toList();
        if (cleanedNames.size() < 2) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Se necesitan al menos dos nombres para separar el artista");
        }

        // Resolver cada nombre. Se descarta el que coincida con el propio original
        // (no tendría sentido "separar en sí mismo") y se deduplica por id.
        Map<String, Artist> resolveCache = new HashMap<>();
        LinkedHashMap<Long, Artist> targets = new LinkedHashMap<>();
        for (String name : cleanedNames) {
            Artist resolved = artistResolver.resolve(name, resolveCache);
            if (resolved != null && resolved.getId() != null
                    && !resolved.getId().equals(source.getId())) {
                targets.putIfAbsent(resolved.getId(), resolved);
            }
        }
        if (targets.size() < 2) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "No se pudieron resolver al menos dos artistas distintos del original");
        }

        List<Artist> targetList = new ArrayList<>(targets.values());
        reassign(source, targetList);
        detachFromFeeds(source);
        artistRepository.delete(source);
        log.info("Artista '{}' (id={}) separado en {} artistas: {}",
                source.getName(), sourceId, targetList.size(),
                targetList.stream().map(Artist::getName).toList());
        return targetList;
    }

    /**
     * Une varios artistas duplicados (p. ej. "El alfa" y "El Alfa") en uno solo.
     * El superviviente conserva su id y su MBID; absorbe las canciones y álbumes
     * del resto, que se eliminan. Opcionalmente se le renombra.
     *
     * @param ids        ids de los artistas a unir (incluido el superviviente)
     * @param survivorId id del artista que sobrevive; debe estar en {@code ids}
     * @param newName    nombre final del superviviente (opcional)
     * @return el artista superviviente
     */
    @Transactional
    public Artist merge(List<Long> ids, Long survivorId, String newName) {
        List<Long> distinctIds = ids == null ? List.of()
                : ids.stream().filter(id -> id != null).distinct().toList();
        if (distinctIds.size() < 2) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Se necesitan al menos dos artistas para juntarlos");
        }
        if (survivorId == null || !distinctIds.contains(survivorId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "El superviviente debe estar entre los artistas seleccionados");
        }

        Artist survivor = artistRepository.findById(survivorId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "No se encontró el artista superviviente con ID " + survivorId));

        for (Long id : distinctIds) {
            if (id.equals(survivorId)) {
                continue;
            }
            Artist other = artistRepository.findById(id)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                            "No se encontró el artista con ID " + id));
            reassign(other, List.of(survivor));
            detachFromFeeds(other);
            artistRepository.delete(other);
        }

        if (newName != null && !newName.isBlank()) {
            survivor.setName(newName.trim());
        }
        log.info("Unidos {} artistas en '{}' (id={})", distinctIds.size(), survivor.getName(), survivorId);
        return survivor;
    }

    // =====================================================================
    // ----- Helpers
    // =====================================================================

    /**
     * Quita {@code from} de todas sus canciones y álbumes y, en su lugar, añade
     * (sin duplicar) cada artista de {@code targets}. Con {@code targets} vacío,
     * simplemente desliga al artista para poder borrarlo sin romper las FKs.
     */
    private void reassign(Artist from, List<Artist> targets) {
        if (from.getSongs() != null) {
            for (Song song : new ArrayList<>(from.getSongs())) {
                rewire(song.getArtists(), from, targets, song::setArtists);
            }
        }
        if (from.getAlbums() != null) {
            for (Album album : new ArrayList<>(from.getAlbums())) {
                rewire(album.getArtists(), from, targets, album::setArtists);
            }
        }
    }

    /**
     * Sobre la lista de artistas (de una canción o un álbum), elimina {@code from}
     * y añade los {@code targets} que aún no estén presentes (comparando por id).
     */
    private void rewire(List<Artist> current, Artist from, List<Artist> targets,
                        java.util.function.Consumer<List<Artist>> setter) {
        List<Artist> artists = current == null ? new ArrayList<>() : new ArrayList<>(current);
        artists.removeIf(a -> a.getId() != null && a.getId().equals(from.getId()));
        for (Artist target : targets) {
            boolean present = artists.stream()
                    .anyMatch(a -> a.getId() != null && a.getId().equals(target.getId()));
            if (!present) {
                artists.add(target);
            }
        }
        setter.accept(artists);
    }

    /** Quita al artista de los feeds que lo recomiendan (la FK de la tabla cruzada). */
    private void detachFromFeeds(Artist artist) {
        for (UserFeed feed : userFeedRepository.findAllByRecommendedArtistId(artist.getId())) {
            feed.getRecommendedArtists()
                    .removeIf(a -> a.getId() != null && a.getId().equals(artist.getId()));
        }
    }
}
