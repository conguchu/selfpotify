package anton.davila.selfpotify.user.service;

import anton.davila.selfpotify.user.entity.Admin;
import anton.davila.selfpotify.user.entity.User;
import anton.davila.selfpotify.user.repository.AdminRepository;
import anton.davila.selfpotify.user.feed.entity.UserFeed;
import anton.davila.selfpotify.user.repository.UserRepository;
import anton.davila.selfpotify.music.repository.SongRepository;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
public class UserService {

    /** Géneros aleatorios del catálogo que se añaden siempre a la lista del home. */
    private static final int RANDOM_GENRES = 3;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AdminRepository adminRepository;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private SongRepository songRepository;

    public User add(User u) {
        log.info("Añadiendo nuevo usuario: {}", u.getUsername());
        return userRepository.save(u);
    }

    public List<User> getAll() {
        log.info("Recuperando todos los usuarios");
        return userRepository.findAll();
    }

    public Optional<User> getById(long id) {
        log.info("Buscando usuario por ID: {}", id);
        return userRepository.findById(id);
    }

    public Optional<User> getByUsername(String username) {
        log.info("Buscando usuario por nombre: {}", username);
        return userRepository.findByUsername(username);
    }

    /**
     * Obtiene el feed del usuario indicado por su ID.
     *
     * @param id identificador del usuario
     * @return el feed asociado al usuario
     */
    public UserFeed getUserFeedById(long id) {
        log.info("Buscando feed del usuario con ID: {}", id);
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("No se encontró el usuario con ID " + id));
        return user.getUserFeed();
    }

    /**
     * Apila el género de una canción recién escuchada en el feed del usuario.
     * Se ejecuta dentro de una transacción para que el dirty checking de
     * Hibernate persista la pila {@code last20GenresListened}. Los géneros
     * nulos o en blanco se ignoran.
     *
     * @param userId identificador del usuario que escucha
     * @param genre  género de la canción reproducida
     */
    @Transactional
    public void registerGenreListen(long userId, String genre) {
        if (genre == null || genre.isBlank()) {
            return;
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("No se encontró el usuario con ID " + userId));
        user.getUserFeed().pushGenero(genre);
    }

    /**
     * Obtiene los 10 géneros escuchados más recientemente por el usuario.
     * La pila de géneros del feed mantiene el índice 0 como el más reciente,
     * por lo que basta con tomar la cabecera de la lista.
     *
     * <p>Además de los géneros recientes, se añaden siempre hasta
     * {@link #RANDOM_GENRES} géneros aleatorios del catálogo (no repetidos) como
     * descubrimiento, igual que el feed de artistas reserva huecos aleatorios.
     *
     * @param id identificador del usuario
     * @return los géneros recientes (máx. 10) seguidos de hasta
     *         {@link #RANDOM_GENRES} géneros aleatorios adicionales
     */
    @Transactional
    public List<String> getLast10GenresListened(long id) {
        log.info("Recuperando los 10 últimos géneros escuchados por el usuario con ID: {}", id);
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("No se encontró el usuario con ID " + id));
        List<String> genres = user.getUserFeed().getLast20GenresListened();
        List<String> result = new ArrayList<>(genres.subList(0, Math.min(10, genres.size())));

        // Siempre se añaden algunos géneros aleatorios del catálogo, excluyendo
        // los ya presentes, como descubrimiento.
        List<String> pool = new ArrayList<>(songRepository.findDistinctGenres());
        pool.removeAll(result);
        Collections.shuffle(pool);
        result.addAll(pool.subList(0, Math.min(RANDOM_GENRES, pool.size())));
        return result;
    }

    @Transactional
    public User update(long id, User userData) {
        log.info("Actualizando usuario con ID: {}", id);
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("No se encontró el usuario con ID " + id));
        user.copy(userData);
        return user;
    }

    @Transactional
    public User changeRole(long id, boolean admin) {
        log.info("Cambiando rol del usuario con ID {} a {}", id, admin ? "ADMIN" : "USER");
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("No se encontró el usuario con ID " + id));

        boolean isAdmin = user instanceof Admin;
        if (isAdmin == admin) {
            return user;
        }

        if (isAdmin && adminRepository.count() <= 1) {
            throw new IllegalStateException("No se puede degradar al último administrador");
        }

        userRepository.updateUserType(id, admin ? "ADMIN" : "USER");
        entityManager.flush();
        entityManager.clear();

        return userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("No se encontró el usuario con ID " + id));
    }

    public User delete(long id) {
        log.warn("Eliminando usuario con ID: {}", id);
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("No se encontró el usuario con ID " + id));
        userRepository.delete(user);
        return user;
    }
}
