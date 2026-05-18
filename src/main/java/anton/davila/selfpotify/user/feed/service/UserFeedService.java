package anton.davila.selfpotify.user.feed.service;

import anton.davila.selfpotify.music.entity.Artist;
import anton.davila.selfpotify.music.repository.ArtistRepository;
import anton.davila.selfpotify.user.entity.User;
import anton.davila.selfpotify.user.feed.entity.UserFeed;
import anton.davila.selfpotify.user.feed.repository.UserFeedRepository;
import anton.davila.selfpotify.user.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
public class UserFeedService {

    @Autowired
    private UserFeedRepository userFeedRepository;

    @Autowired
    private ArtistRepository artistRepository;

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
     * Construye un feed por defecto: los 10 artistas más escuchados.
     * De momento todos los usuarios reciben las mismas recomendaciones.
     */
    public UserFeed buildDefaultFeed() {
        List<Artist> topArtists = artistRepository.findTop10ByOrderByListenersDesc();
        log.info("Construyendo feed por defecto con {} artistas más escuchados", topArtists.size());
        UserFeed feed = new UserFeed();
        feed.setRecommendedArtists(new ArrayList<>(topArtists));
        return feed;
    }

    /**
     * Regenera el feed del usuario indicado con el feed por defecto
     * (los 10 artistas más escuchados). Se invoca cada vez que el usuario
     * accede al home de la plataforma.
     *
     * @param userId identificador del usuario
     * @return el feed ya regenerado
     */
    @Transactional
    public UserFeed regenerateFeedForUser(long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("No se encontró el usuario con ID " + userId));
        log.info("Regenerando feed del usuario con ID: {}", userId);
        UserFeed defaultFeed = buildDefaultFeed();
        UserFeed feed = user.getUserFeed();
        if (feed == null) {
            feed = userFeedRepository.save(defaultFeed);
            user.setUserFeed(feed);
        } else {
            feed.copy(defaultFeed);
        }
        return feed;
    }

    /**
     * Asigna a todos los usuarios que aún no tienen feed un feed por defecto
     * con los 10 artistas más escuchados.
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
            UserFeed feed = userFeedRepository.save(buildDefaultFeed());
            user.setUserFeed(feed);
            userRepository.save(user);
            assigned++;
        }
        log.info("Feed por defecto asignado a {} usuario(s)", assigned);
        return assigned;
    }
}
