package anton.davila.selfpotify;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import anton.davila.selfpotify.config.ConfigService;
import anton.davila.selfpotify.music.service.external.EmbeddedCoverExtractor;
import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.tag.Tag;
import org.jaudiotagger.tag.images.Artwork;
import org.jaudiotagger.tag.images.StandardArtwork;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Base64;
import java.util.Optional;

/**
 * Extracción de carátula embebida sin red: parte de un MP3 silencioso muy
 * pequeño ({@code src/test/resources/audio/silent.mp3}) y le incrusta una imagen
 * con jaudiotagger antes de ejecutar el extractor. El {@code assetsDir} se
 * redirige a un {@link TempDir} mockeando {@link ConfigService}.
 */
public class EmbeddedCoverExtractorTest {

    // PNG 1x1 (transparente) en base64.
    private static final byte[] PNG_1x1 = Base64.getDecoder().decode(
            "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNk+M8AAAMBAQDJ/pLvAAAAAElFTkSuQmCC");

    private static final Path FIXTURE = Paths.get("src/test/resources/audio/silent.mp3");

    @TempDir
    Path assetsDir;

    private ConfigService configService;
    private EmbeddedCoverExtractor extractor;

    @BeforeEach
    void setUp() {
        configService = mock(ConfigService.class);
        when(configService.assetsDir()).thenReturn(assetsDir);
        extractor = new EmbeddedCoverExtractor(configService);
    }

    /** Copia el fixture a un temporal y, si {@code artwork} no es null, lo incrusta. */
    private File mp3With(byte[] artwork, String mime) throws Exception {
        Path tmp = Files.createTempFile("cover-test", ".mp3");
        Files.copy(FIXTURE, tmp, StandardCopyOption.REPLACE_EXISTING);
        if (artwork != null) {
            AudioFile af = AudioFileIO.read(tmp.toFile());
            Tag tag = af.getTagOrCreateAndSetDefault();
            Artwork art = new StandardArtwork();
            art.setBinaryData(artwork);
            art.setMimeType(mime);
            tag.setField(art);
            af.commit();
        }
        tmp.toFile().deleteOnExit();
        return tmp.toFile();
    }

    @Test
    void extractToAsset_withEmbeddedArtwork_writesAssetAndReturnsPath() throws Exception {
        File mp3 = mp3With(PNG_1x1, "image/png");

        Optional<String> result = extractor.extractToAsset(mp3);

        assertTrue(result.isPresent(), "Debería extraer la carátula embebida");
        String path = result.get();
        assertTrue(path.startsWith("/assets/covers/"), "La ruta debe servirse desde /assets/covers/");
        assertTrue(path.endsWith(".png"), "La extensión debe derivarse del MIME (png)");

        // El fichero realmente existe en el assetsDir simulado, con el contenido exacto.
        String fileName = path.substring("/assets/covers/".length());
        Path written = assetsDir.resolve("covers").resolve(fileName);
        assertTrue(Files.exists(written), "El binario debe quedar escrito en disco");
        assertArrayEquals(PNG_1x1, Files.readAllBytes(written));
    }

    @Test
    void extractToAsset_isContentAddressedAndIdempotent() throws Exception {
        File a = mp3With(PNG_1x1, "image/png");
        File b = mp3With(PNG_1x1, "image/png");

        String first = extractor.extractToAsset(a).orElseThrow();
        String second = extractor.extractToAsset(b).orElseThrow();

        // Misma imagen -> mismo nombre (hash del contenido) -> deduplicado.
        assertEquals(first, second);
        try (var stream = Files.list(assetsDir.resolve("covers"))) {
            assertEquals(1, stream.count(), "Carátulas idénticas no deben duplicar ficheros");
        }
    }

    @Test
    void extractToAsset_noArtwork_returnsEmpty() throws Exception {
        File mp3 = mp3With(null, null);
        assertTrue(extractor.extractToAsset(mp3).isEmpty());
    }

    @Test
    void extractToAsset_nonexistentFile_returnsEmpty() {
        assertTrue(extractor.extractToAsset(new File("/no/existe/none.mp3")).isEmpty());
    }

    @Test
    void extractToAsset_nullFile_returnsEmpty() {
        assertTrue(extractor.extractToAsset(null).isEmpty());
    }
}
