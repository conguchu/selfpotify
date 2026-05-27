package anton.davila.selfpotify.controllers;

import anton.davila.selfpotify.controllers.dto.ArtistDTO;
import anton.davila.selfpotify.controllers.dto.SongDTO;
import anton.davila.selfpotify.music.entity.Artist;
import anton.davila.selfpotify.music.entity.Song;
import anton.davila.selfpotify.music.service.SongService;
import anton.davila.selfpotify.user.entity.User;
import anton.davila.selfpotify.user.feed.entity.UserFeed;
import anton.davila.selfpotify.user.feed.service.DailyDiscoveryService;
import anton.davila.selfpotify.user.feed.service.UserFeedService;
import anton.davila.selfpotify.user.repository.UserRepository;
import anton.davila.selfpotify.user.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/feed")
@PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
public class FeedController {

    @Autowired
    private UserFeedService userFeedService;

    @Autowired
    private DailyDiscoveryService dailyDiscoveryService;

    @Autowired
    private SongService songService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserService userService;

    /**
     * Home de la plataforma. Cada vez que el usuario accede, su feed se
     * regenera con el feed por defecto (los 10 artistas más escuchados).
     */
    @GetMapping
    public List<ArtistDTO> getHomeFeed() {
        User currentUser = getCurrentUser();
        UserFeed feed = userFeedService.regenerateFeedForUser(currentUser.getId());
        return feed.getRecommendedArtists().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Devuelve los 10 géneros escuchados más recientemente por el usuario
     * autenticado (índice 0 = más reciente). Alimenta la sección de géneros
     * del home, igual que {@link #getHomeFeed()} alimenta la de artistas.
     */
    @GetMapping("/genres")
    public List<String> getRecentGenres() {
        User currentUser = getCurrentUser();
        return userService.getLast10GenresListened(currentUser.getId());
    }

    /**
     * Descubrimientos diarios del usuario autenticado: 9 canciones compuestas
     * por 3 aleatorias, 3 no escuchadas de su último género escuchado y 3 de un
     * género que no escucha (con los fallbacks descritos en
     * {@link DailyDiscoveryService}). La lista es estable durante el día y se
     * devuelve mezclada.
     */
    @GetMapping("/daily-discoveries")
    public List<SongDTO> getDailyDiscoveries() {
        User currentUser = getCurrentUser();
        List<Song> songs = dailyDiscoveryService.getDailyDiscoveries(currentUser.getId());
        // Una única consulta agrupada para la popularidad (evita el N+1).
        Map<Long, Long> listenCounts = songService.getListenCountsBySong();
        return songs.stream()
                .map(song -> SongDTO.fromEntity(song, listenCounts.getOrDefault(song.getId(), 0L)))
                .collect(Collectors.toList());
    }

    private User getCurrentUser() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        String username;
        if (principal instanceof UserDetails) {
            username = ((UserDetails) principal).getUsername();
        } else {
            username = principal.toString();
        }
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Authenticated user not found"));
    }

    private ArtistDTO convertToDTO(Artist artist) {
        ArtistDTO dto = new ArtistDTO();
        dto.setId(artist.getId());
        dto.setName(artist.getName());
        dto.setPhotoUrl(artist.getPicture_path());
        if (artist.getAlbums() != null) {
            dto.setAlbumIds(artist.getAlbums().stream().map(a -> a.getId()).collect(Collectors.toList()));
        }
        if (artist.getSongs() != null) {
            dto.setSongIds(artist.getSongs().stream().map(s -> s.getId()).collect(Collectors.toList()));
        }
        return dto;
    }
}
