package anton.davila.selfpotify.config;

import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

/**
 * Resuelve la ruta efectiva de la librería musical configurada en el `.env`,
 * eligiendo entre el punto de montaje del contenedor ({@code /music}) cuando se
 * ejecuta en Docker, o la ruta del host ({@code MUSIC_LIBRARY_PATH}) en local.
 */
@Component
public class MusicLibraryResolver {

    private final AppProperties props;

    public MusicLibraryResolver(AppProperties props) {
        this.props = props;
    }

    /** True si el backend corre dentro de un contenedor Docker. */
    public boolean runningInDocker() {
        if (props.getLibrary().isDocker()) {
            return true;
        }
        // Fallback robusto: Docker crea /.dockerenv dentro del contenedor.
        return Files.exists(Paths.get("/.dockerenv"));
    }

    /**
     * Ruta absoluta normalizada de la librería musical, o vacío si no está
     * configurada (p.ej. MUSIC_LIBRARY_PATH en blanco fuera de Docker).
     */
    public Optional<String> resolvePath() {
        String raw = runningInDocker()
                ? props.getLibrary().getDockerPath()
                : props.getLibrary().getPath();
        if (raw == null || raw.isBlank()) {
            return Optional.empty();
        }
        Path p = Paths.get(raw).toAbsolutePath().normalize();
        return Optional.of(p.toString());
    }
}
