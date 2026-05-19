package anton.davila.selfpotify.user.listen.service;

import anton.davila.selfpotify.music.entity.Song;
import anton.davila.selfpotify.music.repository.SongRepository;
import anton.davila.selfpotify.user.entity.User;
import anton.davila.selfpotify.user.listen.entity.UserSongListen;
import anton.davila.selfpotify.user.listen.repository.UserSongListenRepository;
import anton.davila.selfpotify.user.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
public class UserSongListenService {

    /**
     * Máximo de escuchas almacenadas por usuario. Al superarlo se descartan las
     * más antiguas (FIFO). Es un límite de diseño fijo —no configuración por
     * instalación—, igual que {@code MAX_GENEROS} en {@code UserFeed}.
     */
    private static final int MAX_ESCUCHAS = 1000;

    @Autowired
    private UserSongListenRepository repository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SongRepository songRepository;

    /**
     * Registra una escucha del usuario sobre una canción. Si el usuario supera
     * {@link #MAX_ESCUCHAS} registros, borra los más antiguos hasta volver al
     * límite, de forma que la tabla no crece sin control.
     *
     * @param userId id del usuario que escucha
     * @param songId id de la canción escuchada
     */
    @Transactional
    public void recordListen(long userId, long songId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("No se encontró el usuario con ID " + userId));
        Song song = songRepository.findById(songId)
                .orElseThrow(() -> new RuntimeException("No se encontró la canción con ID " + songId));

        repository.save(new UserSongListen(user, song));

        long total = repository.countByUser_Id(userId);
        if (total > MAX_ESCUCHAS) {
            int sobran = (int) (total - MAX_ESCUCHAS);
            List<UserSongListen> aDescartar =
                    repository.findByUser_IdOrderByListenedAtAsc(userId, PageRequest.of(0, sobran));
            repository.deleteAll(aDescartar);
            log.info("Usuario {}: descartadas {} escuchas antiguas (límite {})",
                    userId, sobran, MAX_ESCUCHAS);
        }
    }
}
