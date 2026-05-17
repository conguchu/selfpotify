package anton.davila.selfpotify;

import static org.junit.jupiter.api.Assertions.*;

import anton.davila.selfpotify.config.AppProperties;
import anton.davila.selfpotify.music.service.external.LastFmService;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

/**
 * Test de integración real contra la API de Last.fm.
 * <p>
 * A diferencia de {@code LastFmServiceTest} (unitario, con {@code RestTemplate}
 * mockeado), este test levanta el contexto de Spring y hace llamadas HTTP
 * reales a Last.fm.
 * <p>
 * La API key se toma del fichero {@code .env} de la raíz del proyecto. El
 * proyecto ya no usa {@code spring-dotenv}, así que el {@code .env} no se carga
 * solo: {@link #lastfmApiKey(DynamicPropertyRegistry)} lo lee y registra
 * {@code app.lastfm.api-key}. Si no hay {@code .env} o no contiene
 * {@code LASTFM_API_KEY}, los tests se omiten ({@code assumeTrue}) en lugar de
 * fallar.
 * <p>
 * Nombre con sufijo {@code IT} y {@code @Tag("integration")} para mantenerlo
 * fuera del {@code mvn test} normal; ejecútalo explícitamente con
 * {@code ./mvnw test -Dtest=LastFmServiceIT}.
 */
@Tag("integration")
@SpringBootTest
public class LastFmServiceIT {

    @Autowired
    private LastFmService lastFmService;

    @Autowired
    private AppProperties appProperties;

    @DynamicPropertySource
    static void lastfmApiKey(DynamicPropertyRegistry registry) {
        registry.add("app.lastfm.api-key", () -> readEnvValue("LASTFM_API_KEY"));
    }

    /** Lee una clave del .env de la raíz del proyecto; cadena vacía si no existe. */
    private static String readEnvValue(String key) {
        Path env = Path.of(".env");
        if (!Files.isReadable(env)) {
            return "";
        }
        try {
            for (String line : Files.readAllLines(env)) {
                String trimmed = line.strip();
                if (trimmed.startsWith(key + "=")) {
                    return trimmed.substring(key.length() + 1).strip();
                }
            }
        } catch (IOException e) {
            return "";
        }
        return "";
    }

    @BeforeEach
    void requireApiKey() {
        String apiKey = appProperties.getLastfm().getApiKey();
        Assumptions.assumeTrue(apiKey != null && !apiKey.isBlank(),
                "LASTFM_API_KEY no configurada en .env; se omite el test de integración.");
    }

    @Test
    void fetchGenre_knownTrack_returnsNonBlankGenre() {
        Optional<String> genre = lastFmService.fetchGenre("Queen", "Bohemian Rhapsody");

        assertTrue(genre.isPresent(), "Last.fm debería devolver un género para un tema conocido");
        assertFalse(genre.get().isBlank());
    }

    @Test
    void fetchGenre_unknownTrack_fallsBackToArtistTags() {
        // El track no existe, así que el servicio debe caer a artist.getTopTags.
        Optional<String> genre = lastFmService.fetchGenre(
                "Queen", "tema-que-no-existe-xyz-" + System.nanoTime());

        assertTrue(genre.isPresent(), "Debería resolverse vía los tags del artista");
        assertFalse(genre.get().isBlank());
    }

    @Test
    void fetchGenre_nonexistentArtist_returnsEmpty() {
        Optional<String> genre = lastFmService.fetchGenre(
                "artista-inventado-" + System.nanoTime(), null);

        assertTrue(genre.isEmpty(), "Un artista inexistente no debería devolver género");
    }
}
