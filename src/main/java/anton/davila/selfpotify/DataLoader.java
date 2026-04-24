package anton.davila.selfpotify;

import anton.davila.selfpotify.music.entity.Album;
import anton.davila.selfpotify.music.entity.Artist;
import anton.davila.selfpotify.music.entity.Playlist;
import anton.davila.selfpotify.music.entity.Song;
import anton.davila.selfpotify.music.repository.AlbumRepository;
import anton.davila.selfpotify.music.repository.ArtistRepository;
import anton.davila.selfpotify.music.repository.PlaylistRepository;
import anton.davila.selfpotify.music.repository.SongRepository;
import anton.davila.selfpotify.user.entity.Admin;
import anton.davila.selfpotify.user.entity.User;
import anton.davila.selfpotify.user.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class DataLoader implements CommandLineRunner {

    @Autowired private UserRepository userRepository;
    @Autowired private ArtistRepository artistRepository;
    @Autowired private AlbumRepository albumRepository;
    @Autowired private SongRepository songRepository;
    @Autowired private PlaylistRepository playlistRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        // Solo ejecutar si la BD está vacía para evitar duplicados
        if (songRepository.count() > 0) return;

        // ── Usuarios ──────────────────────────────────────────────────────────
        User user    = saveUser("user",   "password", false);
        User admin   = saveUser("admin",  "admin123", true);
        User alice   = saveUser("alice",  "password", false);
        User bob     = saveUser("bob",    "password", false);
        User carlos  = saveUser("carlos", "password", false);
        User diana   = saveUser("diana",  "password", false);

        // ── Artistas ──────────────────────────────────────────────────────────
        Artist weeknd    = artist("The Weeknd",   18_000_000, "https://i.scdn.co/image/ab6761610000e5eb214f3cf1cbe7139c1e26ffbb");
        Artist daftPunk  = artist("Daft Punk",    10_500_000, "https://i.scdn.co/image/ab6761610000e5eb27c99c28a1e3c9ec407fa48f");
        Artist radiohead = artist("Radiohead",    9_200_000,  "https://i.scdn.co/image/ab6761610000e5eb7da39dea0a72f581535fb11f");
        Artist taylor    = artist("Taylor Swift", 23_000_000, "https://i.scdn.co/image/ab6761610000e5eb5a00969a4698c3132a15fbb0");
        Artist badBunny  = artist("Bad Bunny",    22_000_000, "https://i.scdn.co/image/ab6761610000e5ebe24b3e3b2879d5fec3a3bc60");

        // ── Álbumes ───────────────────────────────────────────────────────────
        Album afterHours = album("After Hours",           "https://i.scdn.co/image/ab67616d0000b2738863bc11d2aa12b54f5aeb36", List.of(weeknd));
        Album ram        = album("Random Access Memories", "https://i.scdn.co/image/ab67616d0000b2739b9b36b0e22870b9f541ad30", List.of(daftPunk));
        Album okComputer = album("OK Computer",           "https://i.scdn.co/image/ab67616d0000b27348c0ca55b55b92e71b2b97ae", List.of(radiohead));
        Album midnights  = album("Midnights",             "https://i.scdn.co/image/ab67616d0000b273bb54dde68cd23e2a268ae0f5", List.of(taylor));
        Album verano     = album("Un Verano Sin Ti",      "https://i.scdn.co/image/ab67616d0000b273b3fb9ed97e625f1e4a23e97f", List.of(badBunny));

        // ── Canciones ─────────────────────────────────────────────────────────
        Song blindingLights      = song("Blinding Lights",         200_000, "Synth-pop",  171, afterHours, List.of(weeknd));
        Song saveYourTears       = song("Save Your Tears",         215_000, "Pop",         119, afterHours, List.of(weeknd));
        Song afterHoursSong      = song("After Hours",             361_000, "R&B",          95, afterHours, List.of(weeknd));
        Song heartless           = song("Heartless",               187_000, "Hip-hop",     130, afterHours, List.of(weeknd));

        Song getLucky            = song("Get Lucky",               248_000, "Funk",        116, ram,        List.of(daftPunk));
        Song instantCrush        = song("Instant Crush",           337_000, "Electronic",   92, ram,        List.of(daftPunk));
        Song loseYourself        = song("Lose Yourself to Dance",  353_000, "Disco",       100, ram,        List.of(daftPunk));
        Song givenToFly          = song("Give Life Back to Music", 274_000, "Funk",        115, ram,        List.of(daftPunk));

        Song karmaPolice         = song("Karma Police",            263_000, "Alternative",  69, okComputer, List.of(radiohead));
        Song paranoidAndroid     = song("Paranoid Android",        387_000, "Art rock",     66, okComputer, List.of(radiohead));
        Song noSurprises         = song("No Surprises",            228_000, "Alternative",  75, okComputer, List.of(radiohead));
        Song existenceOfTime     = song("Exit Music (For a Film)", 254_000, "Art rock",     68, okComputer, List.of(radiohead));

        Song antiHero            = song("Anti-Hero",               200_000, "Pop",          97, midnights,  List.of(taylor));
        Song lavenderHaze        = song("Lavender Haze",           202_000, "Synth-pop",   132, midnights,  List.of(taylor));
        Song midnightRain        = song("Midnight Rain",           174_000, "Pop",          80, midnights,  List.of(taylor));
        Song bejeweled           = song("Bejeweled",               194_000, "Pop",         112, midnights,  List.of(taylor));

        Song titiMePregunto      = song("Tití Me Preguntó",        248_000, "Reggaeton",   102, verano,     List.of(badBunny));
        Song moscowMule          = song("Moscow Mule",             331_000, "Reggaeton",    86, verano,     List.of(badBunny));
        Song mePortoBonito       = song("Me Porto Bonito",         178_000, "Dembow",      106, verano,     List.of(badBunny));
        Song unforgettable       = song("Unforgettable",           218_000, "Trap",         90, verano,     List.of(badBunny));

        // ── Playlists ─────────────────────────────────────────────────────────

        // user (usuario base)
        playlist(user, "My Mix",         "Mezcla personal",        true,
                List.of(blindingLights, getLucky, antiHero));
        playlist(user, "Late Night",     "Para noches tranquilas", false,
                List.of(noSurprises, midnightRain, afterHoursSong));

        // alice
        playlist(alice, "Mis Favoritas", "Lo mejor de siempre",    true,
                List.of(blindingLights, getLucky, karmaPolice, antiHero));
        playlist(alice, "Para Dormir",   "Ambient y chill",        false,
                List.of(noSurprises, midnightRain, loseYourself));

        // bob
        playlist(bob, "Workout",         "Energía máxima",         true,
                List.of(blindingLights, titiMePregunto, mePortoBonito, getLucky));
        playlist(bob, "Noche en Madrid", "Sesión de noche",        false,
                List.of(moscowMule, instantCrush, heartless));

        // carlos
        playlist(carlos, "Clásicos 00s", "Lo que nunca pasa",     true,
                List.of(paranoidAndroid, karmaPolice, noSurprises, existenceOfTime));
        playlist(carlos, "Descubriendo", "Artistas nuevos",       false,
                List.of(lavenderHaze, saveYourTears, givenToFly));

        // diana
        playlist(diana, "Chill Vibes",   "Relajación total",      true,
                List.of(loseYourself, lavenderHaze, noSurprises, midnightRain));
        playlist(diana, "Veranito",      "Solo reggaeton",         true,
                List.of(titiMePregunto, moscowMule, mePortoBonito, unforgettable));
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private User saveUser(String username, String rawPassword, boolean isAdmin) {
        return userRepository.findByUsername(username).orElseGet(() -> {
            User u = isAdmin ? new Admin() : new User();
            u.setUsername(username);
            u.setPassword(passwordEncoder.encode(rawPassword));
            return userRepository.save(u);
        });
    }

    private Artist artist(String name, int listeners, String picturePath) {
        Artist a = new Artist();
        a.setName(name);
        a.setListeners(listeners);
        a.setPicture_path(picturePath);
        return artistRepository.save(a);
    }

    private Album album(String name, String pictureUrl, List<Artist> artists) {
        Album a = new Album();
        a.setName(name);
        a.setPicture_url(pictureUrl);
        a.setArtists(artists);
        return albumRepository.save(a);
    }

    private Song song(String title, int durationMs, String genre, int bpm,
                      Album album, List<Artist> artists) {
        Song s = new Song();
        s.setTitle(title);
        s.setDuration_ms(durationMs);
        s.setGenre(genre);
        s.setBpm(bpm);
        s.setAlbum(album);
        s.setArtists(artists);
        return songRepository.save(s);
    }

    private void playlist(User creator, String name, String description,
                          boolean isPublic, List<Song> songs) {
        Playlist p = new Playlist(songs);
        p.setName(name);
        p.setDescription(description);
        p.setPublic(isPublic);
        p.setCreator(creator);
        playlistRepository.save(p);
    }
}
