package anton.davila.selfpotify.user.feed.service;

import anton.davila.selfpotify.music.entity.Artist;
import anton.davila.selfpotify.music.repository.ArtistRepository;
import anton.davila.selfpotify.user.entity.User;
import anton.davila.selfpotify.user.feed.entity.UserFeed;
import anton.davila.selfpotify.user.feed.repository.UserFeedRepository;
import anton.davila.selfpotify.user.listen.repository.UserSongListenRepository;
import anton.davila.selfpotify.user.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
public class UserFeedService {

    /** Número de artistas recomendados que devuelve el feed del home. */
    private static final int FEED_SIZE = 10;

    /**
     * Cuántos de los {@link #FEED_SIZE} artistas del feed se reservan siempre
     * para descubrimientos aleatorios del catálogo, de modo que el usuario no
     * vea siempre los mismos artistas afines a su historial.
     */
    private static final int RANDOM_ARTISTS = 3;

    @Autowired
    private UserFeedRepository userFeedRepository;

    @Autowired
    private UserSongListenRepository userSongListenRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ArtistRepository artistRepository;

    public UserFeed add(UserFeed f) {
        log.info("Añadiendo nuevo feed de usuario");
        return userFeedRepository.save(f);
    }

    public List<UserFeed> getAll() {
        log.info("Recuperando todos los feeds de usuario");
        return userFeedRepository.findAll();
    }

    public Optional<UserFeed> getById(long id) {
        log.info("Buscando feed de usuario por ID: {}", id);
        return userFeedRepository.findById(id);
    }

    @Transactional
    public UserFeed update(long id, UserFeed feedData) {
        log.info("Actualizando feed de usuario con ID: {}", id);
        UserFeed feed = userFeedRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("No se encontró el feed con ID " + id));
        feed.copy(feedData);
        return feed;
    }

    public UserFeed delete(long id) {
        log.warn("Eliminando feed de usuario con ID: {}", id);
        UserFeed feed = userFeedRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("No se encontró el feed con ID " + id));
        userFeedRepository.delete(feed);
        return feed;
    }

    /**
     * Calcula los artistas recomendados para un usuario concreto.
     * <p>
     * Reglas, en orden:
     * <ol>
     *   <li><strong>Servidor sin escuchas:</strong> si <em>ninguna</em> canción
     *       tiene escuchas registradas, no hay popularidad de la que tirar y se
     *       recomiendan <em>todos</em> los artistas a todos los usuarios.</li>
     *   <li><strong>Usuario sin escuchas:</strong> igualmente, si <em>este</em>
     *       usuario no tiene escuchas propias no hay historial con el que
     *       personalizar, así que recibe también todos los artistas.</li>
     *   <li><strong>Usuario con historial:</strong> se recomiendan de primeros
     *       los artistas con más escuchas globales dentro de los géneros que el
     *       usuario ha estado escuchando últimamente (la pila reciente
     *       {@code last20GenresListened}, cabeza = más reciente). Si quedan
     *       huecos hasta {@link #FEED_SIZE}, se amplía con más artistas de esos
     *       mismos géneros según el catálogo (aunque aún no tengan escuchas) y,
     *       por último, con la popularidad global para no devolver un feed
     *       corto.</li>
     * </ol>
     *
     * @param userId identificador del usuario
     * @return artistas recomendados, sin repetidos (todos los artistas si no hay
     *         escuchas; en otro caso máx. {@link #FEED_SIZE})
     */
    @Transactional
    public List<Artist> recommendArtistsForUser(long userId) {
        // 1) Sin ninguna escucha en todo el servidor: todos los artistas a todos.
        if (userSongListenRepository.count() == 0) {
            log.info("Servidor sin escuchas: se recomiendan todos los artistas (usuario {})", userId);
            return artistRepository.findAll();
        }

        // 2) El usuario no tiene escuchas propias: igualmente, todos los artistas.
        if (userSongListenRepository.countByUser_Id(userId) == 0) {
            log.info("Usuario {} sin escuchas: se recomiendan todos los artistas", userId);
            return artistRepository.findAll();
        }

        PageRequest topN = PageRequest.of(0, FEED_SIZE);
        // LinkedHashSet: preserva el orden de inserción (relevancia) y deduplica.
        LinkedHashSet<Artist> recommended = new LinkedHashSet<>();
        // Se reservan RANDOM_ARTISTS huecos para descubrimientos aleatorios, así
        // que la parte personalizada se llena hasta este tope.
        int personalizedTarget = FEED_SIZE - RANDOM_ARTISTS;

        // 3a) De primeros, los artistas más escuchados (globalmente) de los
        //     géneros que el usuario ha estado escuchando últimamente.
        for (String genre : recentGenresOf(userId)) {
            if (recommended.size() >= personalizedTarget) {
                break;
            }
            for (Artist a : userSongListenRepository.findArtistsByGenreOrderByGlobalListensDesc(genre, topN)) {
                if (recommended.size() >= personalizedTarget) {
                    break;
                }
                recommended.add(a);
            }
        }

        // 3b) Si quedan huecos, se amplía con MÁS artistas de esos mismos géneros
        //     según el catálogo (aunque aún no tengan escuchas), para no reducir
        //     el feed al único artista que el usuario ya ha escuchado.
        for (String genre : recentGenresOf(userId)) {
            if (recommended.size() >= personalizedTarget) {
                break;
            }
            for (Artist a : artistRepository.findArtistsByGenre(genre, topN)) {
                if (recommended.size() >= personalizedTarget) {
                    break;
                }
                recommended.add(a);
            }
        }

        // 3c) Si todavía quedan huecos en la parte personalizada, se completan
        //     con la popularidad global.
        if (recommended.size() < personalizedTarget) {
            for (Artist global : userSongListenRepository.findArtistsByGlobalListensDesc(topN)) {
                if (recommended.size() >= personalizedTarget) {
                    break;
                }
                recommended.add(global);
            }
        }

        // 3d) Siempre RANDOM_ARTISTS artistas aleatorios del catálogo (no
        //     repetidos), como descubrimiento.
        recommended.addAll(pickRandomArtists(RANDOM_ARTISTS, recommended));

        // 3e) Por si el catálogo es pequeño y no se han alcanzado FEED_SIZE,
        //     se rellena con la popularidad global hasta donde se pueda.
        if (recommended.size() < FEED_SIZE) {
            for (Artist global : userSongListenRepository.findArtistsByGlobalListensDesc(topN)) {
                if (recommended.size() >= FEED_SIZE) {
                    break;
                }
                recommended.add(global);
            }
        }

        log.info("Usuario {}: feed con {} artistas (géneros recientes + afines del catálogo + {} aleatorios)",
                userId, recommended.size(), RANDOM_ARTISTS);
        return new ArrayList<>(recommended);
    }

    /**
     * Elige hasta {@code count} artistas aleatorios del catálogo, excluyendo los
     * ya presentes en {@code exclude}. Puede devolver menos si el catálogo no da
     * para más.
     */
    private List<Artist> pickRandomArtists(int count, Collection<Artist> exclude) {
        List<Artist> pool = new ArrayList<>(artistRepository.findAll());
        pool.removeAll(exclude);
        Collections.shuffle(pool);
        return pool.subList(0, Math.min(count, pool.size()));
    }

    /**
     * Géneros que el usuario ha estado escuchando últimamente (pila reciente del
     * feed, cabeza = más reciente). Lista vacía si aún no tiene feed.
     */
    private List<String> recentGenresOf(long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("No se encontró el usuario con ID " + userId));
        UserFeed feed = user.getUserFeed();
        if (feed == null) {
            return List.of();
        }
        return new ArrayList<>(feed.getLast20GenresListened());
    }

    /**
     * Regenera el feed del usuario indicado con recomendaciones personalizadas
     * según su historial. Se invoca cada vez que el usuario accede al home.
     * La pila de géneros escuchados ({@code last20GenresListened}) es historial
     * y no se vacía al regenerar.
     *
     * @param userId identificador del usuario
     * @return el feed ya regenerado
     */
    @Transactional
    public UserFeed regenerateFeedForUser(long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("No se encontró el usuario con ID " + userId));
        log.info("Regenerando feed del usuario con ID: {}", userId);

        List<Artist> recommended = recommendArtistsForUser(userId);

        UserFeed feed = user.getUserFeed();
        if (feed == null) {
            feed = new UserFeed();
            feed.setRecommendedArtists(new ArrayList<>(recommended));
            feed = userFeedRepository.save(feed);
            user.setUserFeed(feed);
        } else {
            // sólo se refrescan los artistas recomendados; la pila de géneros
            // escuchados (last20GenresListened) es historial del usuario y no
            // debe vaciarse al regenerar el home
            feed.setRecommendedArtists(new ArrayList<>(recommended));
        }
        return feed;
    }

    /**
     * Asigna a todos los usuarios que aún no tienen feed un feed con
     * recomendaciones personalizadas según su historial (popularidad global
     * para los que no tengan escuchas).
     *
     * @return número de usuarios a los que se les asignó un feed nuevo
     */
    @Transactional
    public int recommendDefaultFeedToAllUsers() {
        List<User> users = userRepository.findAll();
        int assigned = 0;
        for (User user : users) {
            if (user.getUserFeed() != null) {
                continue;
            }
            UserFeed feed = new UserFeed();
            feed.setRecommendedArtists(new ArrayList<>(recommendArtistsForUser(user.getId())));
            feed = userFeedRepository.save(feed);
            user.setUserFeed(feed);
            userRepository.save(user);
            assigned++;
        }
        log.info("Feed asignado a {} usuario(s) sin feed previo", assigned);
        return assigned;
    }
}
