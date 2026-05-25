package anton.davila.selfpotify.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Al arrancar, y solo mientras el setup inicial no esté completado, añade
 * automáticamente la librería musical configurada en el `.env` a las rutas de
 * escaneo. Idempotente: no duplica si ya está registrada. Tras completar el
 * wizard ({@code setupComplete=true}) deja de tocar las rutas para respetar las
 * decisiones del administrador.
 */
@Slf4j
@Component
@Order(100)
public class LibraryBootstrap implements ApplicationRunner {

    private final ConfigService configService;
    private final MusicLibraryResolver resolver;

    public LibraryBootstrap(ConfigService configService, MusicLibraryResolver resolver) {
        this.configService = configService;
        this.resolver = resolver;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (configService.getConfig().getFeatures().isSetupComplete()) {
            return;
        }
        resolver.resolvePath().ifPresent(raw -> {
            Path folder = Paths.get(raw);
            if (!Files.isDirectory(folder) || !Files.isReadable(folder)) {
                log.warn("Librería musical configurada no accesible, no se auto-añade: {}", folder);
                return;
            }
            boolean added = configService.addScanPath(folder.toString());
            if (added) {
                log.info("Librería musical auto-añadida a las rutas de escaneo: {} (docker={})",
                        folder, resolver.runningInDocker());
            }
        });
    }
}
