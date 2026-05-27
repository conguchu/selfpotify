package anton.davila.selfpotify.music.service.external;

import anton.davila.selfpotify.music.entity.Album;
import anton.davila.selfpotify.music.entity.Artist;
import anton.davila.selfpotify.music.entity.Song;
import anton.davila.selfpotify.music.repository.AlbumRepository;
import anton.davila.selfpotify.music.repository.ArtistRepository;
import anton.davila.selfpotify.music.repository.SongRepository;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.List;
import java.util.Optional;

/**
 * Servicio idempotente de carátulas/fotos apoyado en fuentes externas, gemelo de
 * {@code GenreApiService}: rellena la imagen de canción, álbum y artista <b>sólo
 * si falta</b>, tanto en el escaneo inicial como en el re-escaneo.
 *
 * <p>Vive en un bean separado de {@code SongService} a propósito (igual que
 * {@code GenreApiService}): así {@link #applyCoverIfMissing(Song)} pasa por el
 * proxy de Spring y {@link Transactional} surte efecto. Una auto-invocación
 * {@code this.applyCoverIfMissing(...)} se saltaría el proxy.
 *
 * <p><b>Prioridad de la imagen de la canción</b> (decidida con el usuario):
 * <ol>
 *   <li>Si el archivo de audio trae carátula <b>embebida</b>, se usa y <b>no</b>
 *       se consulta internet para esa canción (ver {@link EmbeddedCoverExtractor}).</li>
 *   <li>Si no hay embebida, se busca en fuentes <b>keyless</b> la más oficial
 *       (ver {@link CoverArtService}) y se guarda la URL externa.</li>
 *   <li>Si no se encuentra nada, el campo queda {@code null} y el frontend pinta
 *       su icono; no se generan placeholders.</li>
 * </ol>
 *
 * <p>La carátula del <b>álbum</b> reutiliza la embebida de la canción si existe
 * (es la misma portada oficial del lanzamiento); si no, va a las fuentes online.
 * La imagen del <b>artista</b> es online-only (Deezer): los ficheros de audio no
 * suelen llevar foto del artista.
 */
@Slf4j
@Service
public class CoverApiService {

    private final SongRepository songRepository;
    private final AlbumRepository albumRepository;
    private final ArtistRepository artistRepository;
    private final CoverArtService coverArtService;
    private final EmbeddedCoverExtractor embeddedCoverExtractor;

    public CoverApiService(SongRepository songRepository,
                           AlbumRepository albumRepository,
                           ArtistRepository artistRepository,
                           CoverArtService coverArtService,
                           EmbeddedCoverExtractor embeddedCoverExtractor) {
        this.songRepository = songRepository;
        this.albumRepository = albumRepository;
        this.artistRepository = artistRepository;
        this.coverArtService = coverArtService;
        this.embeddedCoverExtractor = embeddedCoverExtractor;
    }

    /**
     * Rellena, si faltan, la carátula de la canción (embebida → online), la del
     * álbum y la foto del artista. No hace nada en los campos ya poblados.
     *
     * @param song canción gestionada o con id
     * @return la canción (actualizada si se le aplicó carátula)
     */
    @Transactional
    public Song applyCoverIfMissing(Song song) {
        if (song == null) {
            log.warn("applyCoverIfMissing: canción nula, se ignora.");
            return null;
        }

        String artistName = primaryArtistName(song);
        String albumName = song.getAlbum() != null ? song.getAlbum().getName() : null;

        // 1) Carátula embebida: gana cuando existe. Sirve también para el álbum.
        String embedded = null;
        if (isBlank(song.getPicture_url()) || albumNeedsCover(song)) {
            embedded = extractEmbedded(song).orElse(null);
        }

        // ----- Canción -----
        if (isBlank(song.getPicture_url())) {
            String cover = embedded;
            if (cover == null) {
                cover = coverArtService.fetchTrackCover(artistName, song.getTitle(), albumName).orElse(null);
            }
            if (cover != null) {
                song.setPicture_url(cover);
                songRepository.save(song);
                log.info("Carátula asignada a la canción '{}' (id={}).", song.getTitle(), song.getId());
            } else {
                log.debug("Sin carátula para la canción '{}' (id={}); se deja null.",
                        song.getTitle(), song.getId());
            }
        }

        // ----- Álbum -----
        applyAlbumCoverIfMissing(song.getAlbum(), artistName, embedded);

        // ----- Artista(s) -----
        applyArtistImageIfMissing(song.getArtists());

        return song;
    }

    /** Rellena la carátula del álbum (embebida de la pista si la hay, si no online). */
    private void applyAlbumCoverIfMissing(Album album, String artistName, String embedded) {
        if (album == null || !isBlank(album.getPicture_url())) {
            return;
        }
        String cover = embedded;
        if (cover == null) {
            cover = coverArtService.fetchAlbumCover(artistName, album.getName()).orElse(null);
        }
        if (cover != null) {
            album.setPicture_url(cover);
            albumRepository.save(album);
            log.info("Carátula asignada al álbum '{}' (id={}).", album.getName(), album.getId());
        }
    }

    /** Rellena la foto de cada artista sin imagen (Deezer). */
    private void applyArtistImageIfMissing(List<Artist> artists) {
        if (artists == null) return;
        for (Artist artist : artists) {
            if (artist == null || !isBlank(artist.getPicture_path())) {
                continue;
            }
            if (isBlank(artist.getName())) continue;
            // Algunos nombres agrupan a varios artistas ("Mora / Jaycob"): se
            // busca la foto solo del primero, que no existe como entidad propia
            // bajo ese nombre combinado en fuentes como Deezer.
            String query = primaryArtistName(artist.getName());
            Optional<String> image = coverArtService.fetchArtistImage(query);

            if (image.isPresent()) {
                artist.setPicture_path(image.get());
                artistRepository.save(artist);
                log.info("Foto asignada al artista '{}' (id={}).", artist.getName(), artist.getId());
                log.info("Foto asignada al artista '{}' (id={}, búsqueda='{}').",
                        artist.getName(), artist.getId(), query);
            }
        }
    }

    /**
     * Nombre del artista principal cuando varios vienen agrupados en una sola
     * cadena separados por '/'. Devuelve la primera parte sin espacios sobrantes;
     * si no hay separador, el nombre tal cual.
     */
    private String primaryArtistName(String name) {
        int sep = name.indexOf('/');
        return (sep >= 0 ? name.substring(0, sep) : name).trim();
    }

    private boolean albumNeedsCover(Song song) {
        return song.getAlbum() != null && isBlank(song.getAlbum().getPicture_url());
    }

    private Optional<String> extractEmbedded(Song song) {
        if (song.getSongPath() == null || song.getSongPath().isBlank()) {
            return Optional.empty();
        }
        return embeddedCoverExtractor.extractToAsset(new File(song.getSongPath()));
    }

    private String primaryArtistName(Song song) {
        List<Artist> artists = song.getArtists();
        if (artists == null || artists.isEmpty()) return null;
        Artist first = artists.get(0);
        if (first == null || isBlank(first.getName())) return null;
        return first.getName();
    }

    private boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
