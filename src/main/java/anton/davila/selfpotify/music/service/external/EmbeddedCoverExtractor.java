package anton.davila.selfpotify.music.service.external;

import anton.davila.selfpotify.config.ConfigService;
import lombok.extern.slf4j.Slf4j;
import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.tag.Tag;
import org.jaudiotagger.tag.images.Artwork;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Optional;

/**
 * Extrae la carátula EMBEBIDA de un archivo de audio (ID3 APIC / FLAC picture,
 * vía jaudiotagger) y la materializa como un asset estático servible.
 *
 * <p><b>Cómo se sirve la carátula embebida (decisión de diseño).</b> Los campos
 * de imagen son {@code String} con URL y el frontend renderiza URLs, así que se
 * eligió la opción <b>(a)</b>: volcar los bytes a
 * {@code <assetsDir>/covers/<sha256>.<ext>} y guardar en el campo la ruta
 * relativa {@code /assets/covers/<sha256>.<ext>}, que {@code WebMvcConfig} ya
 * sirve públicamente. Se descartó la opción (b) (un endpoint
 * {@code GET /api/songs/{id}/cover} que streamee los bytes) porque reutiliza el
 * mecanismo de assets ya existente (el del logo), no añade superficie de API
 * autenticada para algo público, y deja la imagen cacheable por el navegador
 * como un fichero normal.
 *
 * <p>El nombre es <b>content-addressed</b> (hash SHA-256 del binario): carátulas
 * idénticas (típico en todas las pistas de un mismo álbum) se deduplican en un
 * único fichero, y la extracción es idempotente (si el fichero ya existe no se
 * reescribe). El reset del servidor borra la carpeta {@code assets/} entera.
 *
 * <p><b>Tensión documentada:</b> la carátula embebida es local (no un "link en
 * la nube" como las fuentes externas), pero el usuario decidió que la embebida
 * gana cuando existe; servirla como asset propio es la forma de exponerla con la
 * misma forma (URL) que las externas.
 */
@Slf4j
@Service
public class EmbeddedCoverExtractor {

    private static final String COVERS_SUBDIR = "covers";

    private final ConfigService configService;

    public EmbeddedCoverExtractor(ConfigService configService) {
        this.configService = configService;
    }

    /**
     * Si el archivo trae carátula embebida, la vuelca a
     * {@code assets/covers/<hash>.<ext>} y devuelve su ruta {@code /assets/...}.
     * Devuelve vacío si no hay carátula embebida o si falla la lectura/escritura.
     */
    public Optional<String> extractToAsset(File audioFile) {
        if (audioFile == null || !audioFile.isFile()) {
            return Optional.empty();
        }
        try {
            AudioFile read = AudioFileIO.read(audioFile);
            Tag tag = read.getTag();
            if (tag == null) {
                return Optional.empty();
            }
            Artwork artwork = tag.getFirstArtwork();
            if (artwork == null) {
                return Optional.empty();
            }
            byte[] data = artwork.getBinaryData();
            if (data == null || data.length == 0) {
                return Optional.empty();
            }
            String ext = extensionFor(artwork.getMimeType());
            return Optional.of(writeCover(data, ext));
        } catch (Exception e) {
            // Un archivo corrupto o sin artwork no debe romper el escaneo del lote.
            log.debug("No se pudo extraer carátula embebida de {}: {}", audioFile.getName(), e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Variante por ruta: extrae la carátula embebida del audio en {@code songPath}
     * y la vuelca a {@code assets/covers}. Devuelve la URL {@code /assets/covers/...}
     * o {@code null} si no hay carátula. Conveniencia para la subida desde el panel.
     */
    public String extractAndStore(String songPath) {
        if (songPath == null || songPath.isBlank()) return null;
        return extractToAsset(new File(songPath)).orElse(null);
    }

    /**
     * Guarda una imagen ARBITRARIA (p.ej. una carátula subida desde el panel) en el
     * MISMO almacén que las embebidas: {@code assets/covers/<sha256>.<ext>}. El
     * nombrado por hash hace la operación idempotente. Devuelve {@code /assets/covers/...}.
     */
    public String storeImageBytes(byte[] data, String mime) throws IOException {
        if (data == null || data.length == 0) {
            throw new IOException("Imagen vacía");
        }
        return writeCover(data, extensionFor(mime));
    }

    /** Escribe los bytes en {@code covers/<sha256>.<ext>} (idempotente). */
    private String writeCover(byte[] data, String ext) throws IOException {
        Path coversDir = configService.assetsDir().resolve(COVERS_SUBDIR);
        Files.createDirectories(coversDir);

        String hash = sha256Hex(data);
        String fileName = hash + "." + ext;
        Path target = coversDir.resolve(fileName);

        if (!Files.exists(target)) {
            Path tmp = coversDir.resolve(fileName + ".tmp");
            Files.write(tmp, data);
            try {
                Files.move(tmp, target, java.nio.file.StandardCopyOption.ATOMIC_MOVE);
            } catch (IOException atomicFail) {
                Files.move(tmp, target, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            }
            log.debug("Carátula embebida guardada en {}", target);
        }
        return "/assets/" + COVERS_SUBDIR + "/" + fileName;
    }

    private String extensionFor(String mimeType) {
        if (mimeType == null) return "jpg";
        String mt = mimeType.toLowerCase();
        if (mt.contains("png")) return "png";
        if (mt.contains("webp")) return "webp";
        if (mt.contains("gif")) return "gif";
        // image/jpeg, image/jpg y cualquier otro se tratan como jpg.
        return "jpg";
    }

    private String sha256Hex(byte[] data) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(md.digest(data));
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 siempre está disponible en la JVM; este camino es inalcanzable.
            throw new IllegalStateException("SHA-256 no disponible", e);
        }
    }
}
