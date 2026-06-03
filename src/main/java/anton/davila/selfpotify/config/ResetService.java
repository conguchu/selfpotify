package anton.davila.selfpotify.config;

import anton.davila.selfpotify.music.repository.AlbumRepository;
import anton.davila.selfpotify.music.repository.ArtistRepository;
import anton.davila.selfpotify.music.repository.PlaylistCollaboratorRepository;
import anton.davila.selfpotify.music.repository.PlaylistRepository;
import anton.davila.selfpotify.music.repository.PlaylistShareTokenRepository;
import anton.davila.selfpotify.music.repository.SongRepository;
import anton.davila.selfpotify.user.follow.repository.UserFollowRepository;
import anton.davila.selfpotify.user.listen.repository.UserSongListenRepository;
import anton.davila.selfpotify.user.profile.repository.ProfileRepository;
import anton.davila.selfpotify.user.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Slf4j
@Service
public class ResetService {

    @Autowired
    private ConfigService configService;

    @Autowired
    private PlaylistRepository playlistRepository;

    @Autowired
    private PlaylistCollaboratorRepository playlistCollaboratorRepository;

    @Autowired
    private PlaylistShareTokenRepository playlistShareTokenRepository;

    @Autowired
    private SongRepository songRepository;

    @Autowired
    private AlbumRepository albumRepository;

    @Autowired
    private ArtistRepository artistRepository;

    @Autowired
    private ProfileRepository profileRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserSongListenRepository userSongListenRepository;

    @Autowired
    private UserFollowRepository userFollowRepository;

    @Autowired
    private AdminBootstrapRunner adminBootstrapRunner;

    @Autowired
    private LibraryBootstrap libraryBootstrap;

    /**
     * Vacía toda la base de datos, resetea la configuración y deja el servidor
     * en el mismo estado en que arrancaría tras un primer despliegue: ejecuta
     * los mismos bootstraps que corren en el arranque para reseedear el admin
     * desde el .env (si {@code ADMIN_USERNAME}/{@code ADMIN_PASSWORD} están
     * definidos) y reañadir la librería musical del .env a las rutas de
     * escaneo (si está configurada y accesible).
     */
    @Transactional
    public void resetAll() throws IOException {
        log.warn("RESET: borrando toda la base de datos y la configuración");

        userSongListenRepository.deleteAll();
        userFollowRepository.deleteAll();
        // Las playlists están referenciadas por colaboradores y magic links
        // (FK sin cascade), así que hay que vaciar esas tablas cruzadas antes
        // de borrar las playlists para no chocar con la restricción.
        playlistShareTokenRepository.deleteAll();
        playlistCollaboratorRepository.deleteAll();
        playlistRepository.deleteAll();
        songRepository.deleteAll();
        albumRepository.deleteAll();
        artistRepository.deleteAll();
        profileRepository.deleteAll();
        userRepository.deleteAll();

        configService.resetToDefaults();

        // Reproducir los bootstraps de arranque para dejar el servidor en el
        // mismo estado que un primer despliegue: admin del .env y ruta de
        // librería del .env reañadida a scan.paths.
        adminBootstrapRunner.run(null);
        libraryBootstrap.run(null);

        log.info("RESET completado: bootstraps de admin y librería re-ejecutados, config en blanco");
    }
}
