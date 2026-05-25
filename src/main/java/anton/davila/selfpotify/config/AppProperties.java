package anton.davila.selfpotify.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.unit.DataSize;

@Getter
@Setter
@ConfigurationProperties(prefix = "app")
public class AppProperties {

    private Admin admin = new Admin();
    private Jwt jwt = new Jwt();
    private Web web = new Web();
    private Lastfm lastfm = new Lastfm();
    private Library library = new Library();
    private Logo logo = new Logo();

    @Getter
    @Setter
    public static class Admin {
        private String username = "";
        private String password = "";
    }

    @Getter
    @Setter
    public static class Jwt {
        private String secret = "";
        private long expirationMs = 86_400_000L;
    }

    @Getter
    @Setter
    public static class Web {
        private String origin = "http://localhost:3000";
    }

    @Getter
    @Setter
    public static class Lastfm {
        private String apiKey = "";
        private String baseUrl = "https://ws.audioscrobbler.com/2.0/";
    }

    @Getter
    @Setter
    public static class Logo {
        /** Tamaño máximo del logo subido (LOGO_MAX_FILE_SIZE). Debe coincidir con
         *  spring.servlet.multipart.max-file-size. */
        private DataSize maxFileSize = DataSize.ofMegabytes(2);
    }

    @Getter
    @Setter
    public static class Library {
        /** Ruta de la librería musical en el host (MUSIC_LIBRARY_PATH). Se usa fuera de Docker. */
        private String path = "";
        /** Punto de montaje de la librería dentro del contenedor (fijado por docker compose). */
        private String dockerPath = "/music";
        /** Marca explícita de ejecución en Docker (la fija el Dockerfile). */
        private boolean docker = false;
    }
}
