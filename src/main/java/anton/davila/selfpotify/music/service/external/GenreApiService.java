package anton.davila.selfpotify.music.service.external;

import anton.davila.selfpotify.music.entity.Artist;
import anton.davila.selfpotify.music.entity.Song;
import anton.davila.selfpotify.music.repository.SongRepository;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * Servicio de clasificación de género apoyado en una API externa (Last.fm).
 * <p>
 * Vive en un bean separado de {@code SongService} a propósito: así las llamadas
 * a {@link #applyGenreIfMissing(Song)} pasan por el proxy de Spring y la
 * anotación {@link Transactional} surte efecto. Si este método estuviera en
 * {@code SongService} y se invocara con {@code this.applyGenreIfMissing(...)},
 * la auto-invocación se saltaría el proxy y no habría transacción real.
 */
@Slf4j
@Service
public class GenreApiService {

    private final SongRepository songRepository;
    private final LastFmService lastFmService;

    public GenreApiService(SongRepository songRepository, LastFmService lastFmService) {
        this.songRepository = songRepository;
        this.lastFmService = lastFmService;
    }

    /**
     * Si la canción no tiene género asignado, lo consulta en Last.fm
     * a partir de su título y primer artista, y lo persiste.
     * Si ya tiene género, no hace nada.
     *
     * @param song canción a clasificar (debe estar gestionada o tener id)
     * @return la canción (actualizada si se le ha aplicado género)
     */
    @Transactional
    public Song applyGenreIfMissing(Song song) {
        if (song == null) {
            log.warn("applyGenreIfMissing: canción nula, se ignora.");
            return null;
        }
        if (song.getGenre() != null && !song.getGenre().isBlank()) {
            log.debug("La canción '{}' ya tiene género '{}', se omite.", song.getTitle(), song.getGenre());
            return song;
        }

        String artistName = primaryArtistName(song);
        if (artistName == null) {
            log.warn("No se puede clasificar la canción '{}' (id={}): no tiene artista asociado.",
                    song.getTitle(), song.getId());
            return song;
        }

        Optional<String> genre = lastFmService.fetchGenre(artistName, song.getTitle());
        if (genre.isEmpty()) {
            log.info("Last.fm no devolvió género para '{} - {}'.", artistName, song.getTitle());
            return song;
        }

        song.setGenre(genre.get());
        Song saved = songRepository.save(song);
        log.info("Género '{}' asignado a la canción '{}' (id={}).",
                saved.getGenre(), saved.getTitle(), saved.getId());
        return saved;
    }

    private String primaryArtistName(Song song) {
        List<Artist> artists = song.getArtists();
        if (artists == null || artists.isEmpty()) return null;
        Artist first = artists.get(0);
        if (first == null || first.getName() == null || first.getName().isBlank()) return null;
        return first.getName();
    }
}
