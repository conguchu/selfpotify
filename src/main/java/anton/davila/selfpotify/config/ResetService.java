package anton.davila.selfpotify.config;

import anton.davila.selfpotify.music.repository.AlbumRepository;
import anton.davila.selfpotify.music.repository.ArtistRepository;
import anton.davila.selfpotify.music.repository.PlaylistRepository;
import anton.davila.selfpotify.music.repository.SongRepository;
import anton.davila.selfpotify.user.entity.Admin;
import anton.davila.selfpotify.user.entity.User;
import anton.davila.selfpotify.user.listen.repository.UserSongListenRepository;
import anton.davila.selfpotify.user.profile.repository.ProfileRepository;
import anton.davila.selfpotify.user.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
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
    private PasswordEncoder passwordEncoder;

    /** Vacía toda la base de datos, recrea los usuarios por defecto y resetea la configuración. */
    @Transactional
    public void resetAll() throws IOException {
        log.warn("RESET: borrando toda la base de datos y la configuración");

        userSongListenRepository.deleteAll();
        playlistRepository.deleteAll();
        songRepository.deleteAll();
        albumRepository.deleteAll();
        artistRepository.deleteAll();
        profileRepository.deleteAll();
        userRepository.deleteAll();

        seedDefaultUsers();
        configService.resetToDefaults();

        log.info("RESET completado: usuarios por defecto y config en blanco");
    }

    private void seedDefaultUsers() {
        User user = new User();
        user.setUsername("user");
        user.setPassword(passwordEncoder.encode("password"));
        userRepository.save(user);

        Admin admin = new Admin();
        admin.setUsername("admin");
        admin.setPassword(passwordEncoder.encode("admin"));
        userRepository.save(admin);
    }
}
