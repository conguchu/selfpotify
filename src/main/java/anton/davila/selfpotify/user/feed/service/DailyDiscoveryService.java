package anton.davila.selfpotify.user.feed.service;

import anton.davila.selfpotify.music.entity.Song;
import anton.davila.selfpotify.music.repository.SongRepository;
import anton.davila.selfpotify.user.entity.User;
import anton.davila.selfpotify.user.feed.entity.UserFeed;
import anton.davila.selfpotify.user.listen.repository.UserSongListenRepository;
import anton.davila.selfpotify.user.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/**
 * Calcula los <strong>descubrimientos diarios</strong> del usuario: una lista
 * de {@value #DISCOVERY_SIZE} canciones compuesta por tres bloques de
 * {@value #PER_BUCKET}:
 * <ol>
 *   <li><strong>Aleatorias</strong> del catálogo.</li>
 *   <li><strong>No escuchadas</strong> del último género que el usuario ha
 *       escuchado (cabeza de su pila de géneros), cayendo al siguiente género
 *       de la pila si no hay suficientes.</li>
 *   <li>De un <strong>género que el usuario no escucha</strong> (presente en el
 *       catálogo pero ausente de su historial), cayendo al género más antiguo
 *       de la pila si no existe candidato.</li>
 * </ol>
 *
 * <p><strong>Estable por día:</strong> toda la aleatoriedad (muestreo de cada
 * bloque, elección del género desconocido y barajado final) usa un único
 * {@link Random} sembrado con {@code userId + fecha}, y las consultas devuelven
 * IDs ordenados por id. Así, dos llamadas del mismo usuario el mismo día
 * devuelven exactamente la misma lista, que cambia a medianoche (zona horaria
 * del servidor). No se persiste nada: el resultado se recalcula en cada
 * petición de forma determinista.
 *
 * <p>El resultado se devuelve <strong>mezclado</strong> (los tres bloques no se
 * distinguen en el orden final).
 */
@Slf4j
@Service
public class DailyDiscoveryService {

    /** Número total de canciones que devuelven los descubrimientos diarios. */
    private static final int DISCOVERY_SIZE = 9;
    /** Canciones por cada uno de los tres bloques. */
    private static final int PER_BUCKET = 3;
    /** Tope para leer los géneros que el usuario ha escuchado (acotado por el FIFO de escuchas). */
    private static final int LISTENED_GENRES_LIMIT = 1000;

    @Autowired
    private SongRepository songRepository;

    @Autowired
    private UserSongListenRepository userSongListenRepository;

    @Autowired
    private UserRepository userRepository;

    /**
     * Calcula los descubrimientos diarios del usuario indicado.
     *
     * @param userId identificador del usuario
     * @return hasta {@value #DISCOVERY_SIZE} canciones distintas, mezcladas;
     *         menos sólo si el catálogo no da para más
     */
    @Transactional
    public List<Song> getDailyDiscoveries(long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("No se encontró el usuario con ID " + userId));
        log.info("Calculando descubrimientos diarios del usuario con ID: {}", userId);

        // Semilla estable por día: misma lista durante toda la jornada.
        Random rnd = new Random(userId * 31L + LocalDate.now().toEpochDay());

        UserFeed feed = user.getUserFeed();
        List<String> genreStack = (feed != null && feed.getLast20GenresListened() != null)
                ? feed.getLast20GenresListened()
                : List.of();
        Set<String> listenedGenres = new HashSet<>(userSongListenRepository
                .findTopGenresByUserListens(userId, PageRequest.of(0, LISTENED_GENRES_LIMIT)));

        // Conserva el orden de inserción de los bloques y deduplica entre ellos.
        Set<Long> chosen = new LinkedHashSet<>();

        // --- Bloque 1: aleatorias por completo ---
        chosen.addAll(pickRandom(songRepository.findAvailableSongIds(), chosen, PER_BUCKET, rnd));

        // --- Bloque 2: no escuchadas del último género (fallback al siguiente) ---
        chosen.addAll(pickUnheardFromGenreStack(userId, genreStack, chosen, rnd));

        // --- Bloque 3: de un género que el usuario no escucha (fallback al más antiguo de la pila) ---
        chosen.addAll(pickFromUnlistenedGenre(genreStack, listenedGenres, chosen, rnd));

        // --- Relleno de cortesía: si el catálogo es pequeño y no se llegó a 9 ---
        if (chosen.size() < DISCOVERY_SIZE) {
            chosen.addAll(pickRandom(songRepository.findAvailableSongIds(), chosen,
                    DISCOVERY_SIZE - chosen.size(), rnd));
        }

        List<Long> finalIds = new ArrayList<>(chosen);
        Collections.shuffle(finalIds, rnd);
        log.debug("Descubrimientos diarios del usuario {}: {} canciones", userId, finalIds.size());
        return orderByIds(finalIds);
    }

    /**
     * Recorre la pila de géneros desde la cabeza (más reciente) acumulando
     * canciones no escuchadas hasta reunir {@value #PER_BUCKET}. Implementa el
     * fallback "a la siguiente" del enunciado.
     */
    private List<Long> pickUnheardFromGenreStack(long userId, List<String> genreStack,
                                                 Set<Long> exclude, Random rnd) {
        List<Long> picked = new ArrayList<>();
        Set<Long> taken = new HashSet<>(exclude);
        for (String genre : genreStack) {
            if (picked.size() >= PER_BUCKET) {
                break;
            }
            List<Long> candidates = pickRandom(
                    songRepository.findUnheardSongIdsByGenre(userId, genre),
                    taken, PER_BUCKET - picked.size(), rnd);
            picked.addAll(candidates);
            taken.addAll(candidates);
        }
        return picked;
    }

    /**
     * Elige un género del catálogo que el usuario no escucha (al azar entre los
     * candidatos) y toma de él {@value #PER_BUCKET} canciones. Si no hay ningún
     * género desconocido con canciones nuevas, cae al género más antiguo de la
     * pila del usuario.
     */
    private List<Long> pickFromUnlistenedGenre(List<String> genreStack, Set<String> listenedGenres,
                                               Set<Long> exclude, Random rnd) {
        List<String> candidates = new ArrayList<>();
        for (String genre : songRepository.findDistinctAvailableGenres()) {
            if (!listenedGenres.contains(genre)) {
                candidates.add(genre);
            }
        }
        Collections.shuffle(candidates, rnd);
        for (String genre : candidates) {
            List<Long> picked = pickRandom(
                    songRepository.findAvailableSongIdsByGenre(genre), exclude, PER_BUCKET, rnd);
            if (!picked.isEmpty()) {
                return picked;
            }
        }

        // Fallback: el género más antiguo de la pila (último elemento).
        if (!genreStack.isEmpty()) {
            String oldest = genreStack.get(genreStack.size() - 1);
            return pickRandom(songRepository.findAvailableSongIdsByGenre(oldest),
                    exclude, PER_BUCKET, rnd);
        }
        return List.of();
    }

    /**
     * Baraja los candidatos con el {@link Random} sembrado y devuelve hasta
     * {@code limit} ids que no estén ya en {@code exclude}.
     */
    private List<Long> pickRandom(List<Long> candidates, Set<Long> exclude, int limit, Random rnd) {
        if (limit <= 0 || candidates.isEmpty()) {
            return List.of();
        }
        List<Long> pool = new ArrayList<>(candidates);
        Collections.shuffle(pool, rnd);
        List<Long> picked = new ArrayList<>(limit);
        for (Long id : pool) {
            if (picked.size() >= limit) {
                break;
            }
            if (!exclude.contains(id)) {
                picked.add(id);
            }
        }
        return picked;
    }

    /**
     * Carga las entidades de los ids dados conservando el orden de la lista
     * (findAllById no garantiza orden).
     */
    private List<Song> orderByIds(List<Long> ids) {
        Map<Long, Song> byId = new LinkedHashMap<>();
        for (Song s : songRepository.findAllById(ids)) {
            byId.put(s.getId(), s);
        }
        List<Song> ordered = new ArrayList<>(ids.size());
        for (Long id : ids) {
            Song s = byId.get(id);
            if (s != null) {
                ordered.add(s);
            }
        }
        return ordered;
    }
}
