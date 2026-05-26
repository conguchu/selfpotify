package anton.davila.selfpotify.user.feed.service;

import anton.davila.selfpotify.music.entity.Artist;
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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
public class UserFeedService {

    /** Número de artistas recomendados que devuelve el feed del home. */
    private static final int FEED_SIZE = 10;

    @Autowired
    private UserFeedRepository userFeedRepository;

    @Autowired
    private UserSongListenRepository userSongListenRepository;

    @Autowired
    private UserRepository userRepository;

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
     * El feed es <strong>personalizado</strong> a partir del propio historial
     * del usuario (sus filas en {@code user_song_listen}):
     * <ol>
     *   <li>Se toman primero los artistas que <em>ese</em> usuario más ha
     *       escuchado ({@code findTopArtistsByUserListens}), de más a menos.</li>
     *   <li>Si no llega a {@link #FEED_SIZE}, se completa con la popularidad
     *       global ({@code findArtistsByGlobalListensDesc}) descartando los que
     *       ya estaban, para aportar descubrimiento sin huecos.</li>
     * </ol>
     * <strong>Cold-start:</strong> un usuario sin escuchas no tiene historial,
     * así que la primera lista queda vacía y recibe directamente la popularidad
     * global (también derivada de los eventos, ya que el contador numérico
     * {@code listeners} desapareció).
     *
     * @param userId identificador del usuario
     * @return lista (máx. {@link #FEED_SIZE}) de artistas recomendados, sin repetidos
     */
    public List<Artist> recommendArtistsForUser(long userId) {
        PageRequest topN = PageRequest.of(0, FEED_SIZE);

        // LinkedHashSet: preserva el orden de inserción (relevancia) y deduplica.
        LinkedHashSet<Artist> recommended = new LinkedHashSet<>(
                userSongListenRepository.findTopArtistsByUserListens(userId, topN));

        if (recommended.size() < FEED_SIZE) {
            if (recommended.isEmpty()) {
                log.info("Usuario {} sin historial: feed por popularidad global (cold-start)", userId);
            } else {
                log.info("Usuario {}: completando el feed personalizado con popularidad global", userId);
            }
            for (Artist global : userSongListenRepository.findArtistsByGlobalListensDesc(topN)) {
                if (recommended.size() >= FEED_SIZE) {
                    break;
                }
                recommended.add(global);
            }
        } else {
            log.info("Usuario {}: feed personalizado con sus {} artistas más escuchados",
                    userId, recommended.size());
        }

        return new ArrayList<>(recommended);
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
