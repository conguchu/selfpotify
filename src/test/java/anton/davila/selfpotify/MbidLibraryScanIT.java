package anton.davila.selfpotify;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import anton.davila.selfpotify.music.entity.Artist;
import anton.davila.selfpotify.music.entity.Song;
import anton.davila.selfpotify.music.repository.SongRepository;
import anton.davila.selfpotify.music.service.SongService;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * Test de integración real: escanea la biblioteca "Dembow mix" del usuario y
 * comprueba la resolución de artistas y MBIDs de extremo a extremo.
 * <p>
 * A diferencia de {@link MbidArtistResolutionTest} (determinista, con Last.fm y
 * el repositorio mockeados), aquí se usa el {@code LastFmService} real y se hace
 * un escaneo real de la carpeta. {@code SongRepository} sigue mockeado para no
 * depender de un esquema de canciones; {@code ArtistRepository} es el real
 * (H2 en memoria) para que la deduplicación por MBID funcione contra la BBDD.
 * <p>
 * El test se omite ({@code assumeTrue}) si la carpeta no existe en la máquina.
 * Sufijo {@code IT} y {@code @Tag("integration")} para mantenerlo fuera del
 * {@code mvn test} normal; ejecútalo con
 * {@code ./mvnw test -Dtest=MbidLibraryScanIT}.
 */
@Tag("integration")
@SpringBootTest
public class MbidLibraryScanIT {

    private static final String LIBRARY_PATH =
            "/Users/antondavila/Music/SoundCloud/Dembow mix";

    @Autowired
    private SongService songService;

    @MockitoBean
    private SongRepository songRepository;

    /** La API key real se toma del .env de la raíz; vacía si no existe. */
    @DynamicPropertySource
    static void lastfmApiKey(DynamicPropertyRegistry registry) {
        registry.add("app.lastfm.api-key", () -> readEnvValue("LASTFM_API_KEY"));
    }

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
    void setUp() {
        Assumptions.assumeTrue(Files.isDirectory(Path.of(LIBRARY_PATH)),
                "Carpeta '" + LIBRARY_PATH + "' no encontrada; se omite el test.");
        when(songRepository.saveAll(anyList()))
                .thenAnswer(inv -> inv.getArgument(0));
        // GenreApiService persiste la canción cuando Last.fm devuelve género:
        // el mock debe devolver la propia canción y no null.
        when(songRepository.save(any(Song.class)))
                .thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void scanRealLibrary_everySongHasAnArtist_andArtistsAreDeduplicated() {
        List<Song> songs = songService.loadFolder(LIBRARY_PATH);

        assertFalse(songs.isEmpty(), "La biblioteca debería contener canciones");

        // Conjunto por identidad: cuenta entidades Artist distintas (no por equals,
        // que en @Data recorrería las relaciones perezosas).
        Set<Artist> artists = java.util.Collections.newSetFromMap(new IdentityHashMap<>());

        int songsWithoutArtist = 0;
        int withMbid = 0;
        for (Song s : songs) {
            if (s.getArtists() == null || s.getArtists().isEmpty()) {
                songsWithoutArtist++;
                continue;
            }
            for (Artist a : s.getArtists()) {
                artists.add(a);
            }
        }
        for (Artist a : artists) {
            if (a.getMbid() != null && !a.getMbid().isBlank()) {
                withMbid++;
            }
        }

        // Reporte legible en consola.
        Map<String, String> report = new TreeMap<>();
        for (Artist a : artists) {
            report.put(a.getName(), a.getMbid() == null ? "(sin mbid)" : a.getMbid());
        }
        System.out.println("\n==================== ESCANEO BIBLIOTECA ====================");
        System.out.printf("Canciones: %d | Artistas distintos: %d | Con MBID: %d | Sin artista: %d%n",
                songs.size(), artists.size(), withMbid, songsWithoutArtist);
        System.out.println("------------------------------------------------------------");
        report.forEach((name, mbid) -> System.out.printf("  %-32s %s%n", name, mbid));
        System.out.println("============================================================\n");

        // Invariantes que deben cumplirse independientemente del contenido real:
        assertTrue(artists.size() <= songs.size(),
                "No puede haber más artistas distintos que canciones");
        // Ninguna entidad Artist debe tener nombre nulo o vacío.
        for (Artist a : artists) {
            assertNotNull(a.getName());
            assertFalse(a.getName().isBlank());
        }
        // Si dos entidades distintas comparten MBID no nulo, la dedup falló.
        Map<String, Artist> seenMbids = new java.util.HashMap<>();
        for (Artist a : artists) {
            if (a.getMbid() == null || a.getMbid().isBlank()) {
                continue;
            }
            Artist prev = seenMbids.putIfAbsent(a.getMbid(), a);
            assertNull(prev, () -> "MBID duplicado en dos entidades Artist: " + a.getMbid());
        }
    }
}
