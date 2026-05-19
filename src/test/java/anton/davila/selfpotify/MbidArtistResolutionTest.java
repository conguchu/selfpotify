package anton.davila.selfpotify;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import anton.davila.selfpotify.music.entity.Artist;
import anton.davila.selfpotify.music.entity.Song;
import anton.davila.selfpotify.music.repository.ArtistRepository;
import anton.davila.selfpotify.music.repository.SongRepository;
import anton.davila.selfpotify.music.service.SongService;
import anton.davila.selfpotify.music.service.external.LastFmService;
import anton.davila.selfpotify.music.service.external.LastFmService.ArtistIdentity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Tests dedicados a la deduplicación de artistas por MBID que hace
 * {@code SongService.resolveArtist} durante {@code loadFolder}.
 * <p>
 * El método {@code resolveArtist} es privado, así que se ejercita a través de
 * {@link SongService#loadFolder(String)}. Para que sea determinista:
 * <ul>
 *   <li>Se crean ficheros {@code .mp3} <em>vacíos</em> en un {@code @TempDir}
 *       con nombres reales de la biblioteca "Dembow mix". Sin tags ID3, el
 *       servicio cae a la convención "Artista - Título" del nombre de archivo.</li>
 *   <li>{@link LastFmService} está mockeado: simula la resolución de Last.fm
 *       sin red, devolviendo MBIDs estables.</li>
 *   <li>{@link ArtistRepository} está mockeado con un almacén en memoria que
 *       imita {@code save}, {@code findByMbid} y {@code findByNameIgnoreCase}.</li>
 * </ul>
 */
@SpringBootTest
public class MbidArtistResolutionTest {

    private static final String MBID_EL_ALFA = "mbid-el-alfa-0001";
    private static final String MBID_ALFA_IT = "mbid-alfa-italian-02";
    private static final String MBID_BAD_BUNNY = "mbid-bad-bunny-0003";

    @Autowired
    private SongService songService;

    @MockitoBean
    private SongRepository songRepository;

    @MockitoBean
    private ArtistRepository artistRepository;

    @MockitoBean
    private LastFmService lastFmService;

    /** Almacén en memoria que respalda al ArtistRepository mockeado. */
    private final List<Artist> store = new ArrayList<>();
    private final AtomicLong seq = new AtomicLong();

    @BeforeEach
    void setUp() {
        store.clear();
        seq.set(0);

        // songRepository.saveAll devuelve lo mismo que recibe (sin tocar BBDD).
        when(songRepository.saveAll(anyList()))
                .thenAnswer(inv -> inv.getArgument(0));

        // ArtistRepository en memoria.
        when(artistRepository.save(any(Artist.class))).thenAnswer(inv -> {
            Artist a = inv.getArgument(0);
            if (a.getId() == null) {
                a.setId(seq.incrementAndGet());
                store.add(a);
            }
            return a;
        });
        when(artistRepository.findByMbid(anyString())).thenAnswer(inv -> {
            String mbid = inv.getArgument(0);
            return store.stream().filter(a -> mbid.equals(a.getMbid())).findFirst();
        });
        when(artistRepository.findByNameIgnoreCase(anyString())).thenAnswer(inv -> {
            String name = inv.getArgument(0);
            return store.stream().filter(a -> name.equalsIgnoreCase(a.getName())).findFirst();
        });

        // Last.fm simulado: las variantes de "El Alfa" colapsan a una identidad;
        // "Alfa" (cantante italiana) es un artista distinto pese al nombre parecido.
        when(lastFmService.resolveArtist(anyString())).thenAnswer(inv -> {
            String name = ((String) inv.getArgument(0)).toLowerCase();
            if (name.contains("alfa") && (name.contains("jefe") || name.equals("el alfa"))) {
                return Optional.of(new ArtistIdentity("El Alfa", MBID_EL_ALFA));
            }
            if (name.equals("alfa")) {
                return Optional.of(new ArtistIdentity("Alfa", MBID_ALFA_IT));
            }
            if (name.contains("bad") && name.contains("bunny")) {
                return Optional.of(new ArtistIdentity("Bad Bunny", MBID_BAD_BUNNY));
            }
            return Optional.empty(); // artista desconocido para Last.fm
        });
    }

    /** Crea ficheros .mp3 vacíos con los nombres indicados dentro de dir. */
    private void createFiles(Path dir, String... fileNames) throws IOException {
        for (String fileName : fileNames) {
            Files.createFile(dir.resolve(fileName));
        }
    }

    /** Primer artista de la canción cuyo título coincide. */
    private Artist artistOf(List<Song> songs, String titleContains) {
        return songs.stream()
                .filter(s -> s.getTitle() != null && s.getTitle().contains(titleContains))
                .findFirst()
                .orElseThrow(() -> new AssertionError("No se encontró la canción: " + titleContains))
                .getArtists().get(0);
    }

    @Test
    void distinctFilenameVariants_ofSameArtist_collapseIntoOneEntityViaMbid(@TempDir Path dir)
            throws IOException {
        // Dos formas de escribir el MISMO artista en la biblioteca real.
        createFiles(dir,
                "El Alfa - ESTE.mp3",
                "✅EL ALFA EL JEFE - ✅EL ALFA - SINGAPUR (EL ANDROIDE).mp3");

        List<Song> songs = songService.loadFolder(dir.toString());

        assertEquals(2, songs.size());
        Artist a1 = artistOf(songs, "ESTE");
        Artist a2 = artistOf(songs, "SINGAPUR");

        assertSame(a1, a2, "Ambas variantes deben resolver a la MISMA entidad Artist");
        assertEquals("El Alfa", a1.getName(), "Debe usarse el nombre canónico de Last.fm");
        assertEquals(MBID_EL_ALFA, a1.getMbid());
        assertEquals(1, store.size(), "Sólo debe existir una fila de artista");
    }

    @Test
    void similarlyNamedButDifferentArtists_stayAsSeparateEntities(@TempDir Path dir)
            throws IOException {
        // "El Alfa" (dembow) y "Alfa" (pop italiano) comparten subcadena pero
        // tienen MBIDs distintos: no deben fusionarse.
        createFiles(dir,
                "El Alfa - Gogo Dance.mp3",
                "Alfa - A me mi piace.mp3");

        List<Song> songs = songService.loadFolder(dir.toString());

        Artist elAlfa = artistOf(songs, "Gogo Dance");
        Artist alfa = artistOf(songs, "A me mi piace");

        assertNotSame(elAlfa, alfa);
        assertEquals(MBID_EL_ALFA, elAlfa.getMbid());
        assertEquals(MBID_ALFA_IT, alfa.getMbid());
        assertEquals(2, store.size());
    }

    @Test
    void existingArtistWithoutMbid_getsMbidBackfilledOnScan(@TempDir Path dir)
            throws IOException {
        // Fila preexistente "El Alfa" SIN mbid (escaneada antes de tener Last.fm).
        Artist legacy = new Artist();
        legacy.setName("El Alfa");
        legacy.setId(seq.incrementAndGet());
        store.add(legacy);

        createFiles(dir, "El Alfa - ESTE.mp3");

        List<Song> songs = songService.loadFolder(dir.toString());

        Artist resolved = artistOf(songs, "ESTE");
        assertSame(legacy, resolved, "Debe reutilizar la fila existente, no crear otra");
        assertEquals(MBID_EL_ALFA, resolved.getMbid(), "Se le debe rellenar el MBID (backfill)");
        assertEquals(1, store.size());
        verify(artistRepository).save(legacy);
    }

    @Test
    void manyTracksOfSameArtist_resolveToOneEntityAndQueryLastFmOncePerName(@TempDir Path dir)
            throws IOException {
        createFiles(dir,
                "Bad-Bunny - Callaita.mp3",
                "Bad-Bunny - DtMF.mp3",
                "Bad-Bunny - Moscow Mule.mp3",
                "Bad-Bunny - Safaera.mp3");

        List<Song> songs = songService.loadFolder(dir.toString());

        assertEquals(4, songs.size());
        Artist first = songs.get(0).getArtists().get(0);
        for (Song s : songs) {
            assertSame(first, s.getArtists().get(0),
                    "Todas las canciones deben compartir la misma entidad Artist");
        }
        assertEquals("Bad Bunny", first.getName());
        assertEquals(1, store.size());
        // La caché por lote evita repetir la consulta: un único nombre limpio.
        verify(lastFmService, times(1)).resolveArtist(anyString());
    }

    @Test
    void unknownArtist_fallsBackToCleanedNameMatching(@TempDir Path dir) throws IOException {
        // Last.fm no reconoce a este artista: se deduplica por nombre limpio.
        createFiles(dir,
                "DJ Desconocido - Tema Uno.mp3",
                "DJ Desconocido - Tema Dos.mp3");

        List<Song> songs = songService.loadFolder(dir.toString());

        Artist a1 = artistOf(songs, "Tema Uno");
        Artist a2 = artistOf(songs, "Tema Dos");

        assertSame(a1, a2);
        assertEquals("DJ Desconocido", a1.getName());
        assertNull(a1.getMbid(), "Sin MBID cuando Last.fm no resuelve al artista");
        assertEquals(1, store.size());
    }

    @Test
    void emojiAndWhitespaceVariants_inTheSameBatch_doNotDuplicateRows(@TempDir Path dir)
            throws IOException {
        // Tres escrituras del mismo artista que sólo difieren en adornos.
        createFiles(dir,
                "El Alfa - ESTE.mp3",
                "✅EL ALFA EL JEFE - ✅EL ALFA - BESALO.mp3",
                "El Alfa - Gogo Dance.mp3");

        List<Song> songs = songService.loadFolder(dir.toString());

        assertEquals(3, songs.size());
        Artist ref = songs.get(0).getArtists().get(0);
        for (Song s : songs) {
            assertSame(ref, s.getArtists().get(0));
        }
        assertEquals(1, store.size(), "Las 3 variantes deben colapsar en una sola fila");
    }
}
