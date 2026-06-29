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
    private Upload upload = new Upload();
    private CoverArt coverArt = new CoverArt();

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
        /** Timeout de conexión en ms para las llamadas a Last.fm (LASTFM_CONNECT_TIMEOUT_MS). */
        private int connectTimeoutMs = 4000;
        /** Timeout de lectura en ms para las llamadas a Last.fm (LASTFM_READ_TIMEOUT_MS). */
        private int readTimeoutMs = 6000;
    }

    @Getter
    @Setter
    public static class Logo {
        /** Tamaño máximo del logo subido (LOGO_MAX_FILE_SIZE). Validado a nivel de
         *  aplicación; el límite global de multipart es el de {@link Upload}. */
        private DataSize maxFileSize = DataSize.ofMegabytes(2);
    }

    @Getter
    @Setter
    public static class Upload {
        /** Tamaño máximo por archivo de audio subido vía drag&drop
         *  (SONG_UPLOAD_MAX_FILE_SIZE). Debe ser <= el límite global de multipart. */
        private DataSize maxFileSize = DataSize.ofMegabytes(50);
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

    /**
     * Carátulas/fotos automáticas durante el escaneo. Todas las fuentes funcionan
     * sin API key; sólo MusicBrainz exige un User-Agent descriptivo (su política
     * de uso lo requiere). Por defecto todo está activado y funciona sin tocar nada.
     */
    @Getter
    @Setter
    public static class CoverArt {
        /** Interruptor global de la resolución de carátulas online (COVER_ART_ENABLED). */
        private boolean enabled = true;
        /**
         * User-Agent enviado a MusicBrainz (COVER_ART_USER_AGENT). Su política exige
         * identificar la app y un contacto: "App/versión ( contacto )".
         */
        private String userAgent = "selfpotify/1.0 ( https://github.com/selfpotify )";
        /** Timeout de conexión en milisegundos para las llamadas a fuentes externas. */
        private int connectTimeoutMs = 4000;
        /** Timeout de lectura en milisegundos para las llamadas a fuentes externas. */
        private int readTimeoutMs = 6000;
    }
}
