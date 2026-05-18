package anton.davila.selfpotify.controllers;

import anton.davila.selfpotify.controllers.dto.ArtistDTO;
import anton.davila.selfpotify.music.entity.Artist;
import anton.davila.selfpotify.user.entity.User;
import anton.davila.selfpotify.user.feed.entity.UserFeed;
import anton.davila.selfpotify.user.feed.service.UserFeedService;
import anton.davila.selfpotify.user.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/feed")
@PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
public class FeedController {

    @Autowired
    private UserFeedService userFeedService;

    @Autowired
    private UserRepository userRepository;

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
