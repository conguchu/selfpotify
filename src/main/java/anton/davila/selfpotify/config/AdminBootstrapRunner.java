package anton.davila.selfpotify.config;

import anton.davila.selfpotify.user.entity.Admin;
import anton.davila.selfpotify.user.repository.AdminRepository;
import anton.davila.selfpotify.user.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class AdminBootstrapRunner implements ApplicationRunner {

    private final AppProperties appProperties;
    private final AdminRepository adminRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AdminBootstrapRunner(AppProperties appProperties,
                                AdminRepository adminRepository,
                                UserRepository userRepository,
                                PasswordEncoder passwordEncoder) {
        this.appProperties = appProperties;
        this.adminRepository = adminRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (adminRepository.count() > 0) {
            log.debug("Admin ya existe en BBDD; bootstrap omitido.");
            return;
        }

        String username = appProperties.getAdmin().getUsername();
        String password = appProperties.getAdmin().getPassword();

        if (username == null || username.isBlank() || password == null || password.isBlank()) {
            log.warn("ADMIN_USERNAME o ADMIN_PASSWORD vacíos en .env; bootstrap del admin omitido. " +
                    "No habrá ningún admin hasta que los definas y reinicies.");
            return;
        }

        if (userRepository.findByUsername(username).isPresent()) {
            log.warn("Ya existe un usuario no-admin con username '{}'; bootstrap del admin omitido.", username);
            return;
        }

        Admin admin = new Admin();
        admin.setUsername(username);
        admin.setPassword(passwordEncoder.encode(password));
        adminRepository.save(admin);
        log.info("Admin inicial '{}' creado desde .env.", username);
    }
}
