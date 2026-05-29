package anton.davila.selfpotify.music.service;

import anton.davila.selfpotify.controllers.dto.AlbumDTO;
import anton.davila.selfpotify.controllers.dto.ArtistDTO;
import anton.davila.selfpotify.controllers.dto.GenreResultDTO;
import anton.davila.selfpotify.controllers.dto.PlaylistDTO;
import anton.davila.selfpotify.controllers.dto.SearchResponseDTO;
import anton.davila.selfpotify.controllers.dto.SearchResponseDTO.CategoryPage;
import anton.davila.selfpotify.controllers.dto.SongDTO;
import anton.davila.selfpotify.controllers.dto.UserSummaryDTO;
import anton.davila.selfpotify.music.entity.Album;
import anton.davila.selfpotify.music.entity.Artist;
import anton.davila.selfpotify.music.entity.Playlist;
import anton.davila.selfpotify.music.entity.Song;
import anton.davila.selfpotify.music.repository.AlbumRepository;
import anton.davila.selfpotify.music.repository.ArtistRepository;
import anton.davila.selfpotify.music.repository.PlaylistRepository;
import anton.davila.selfpotify.music.repository.SongRepository;
import anton.davila.selfpotify.user.entity.User;
import anton.davila.selfpotify.user.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Búsqueda transversal del catálogo y los usuarios.
 *
 * <p><b>Normalización.</b> Tanto la consulta como los textos a comparar se
 * pasan por la misma rutina ({@link #normalize}): {@code NFD} +
 * desdiacritizado ({@code \p{InCombiningDiacriticalMarks}+ → "")} + lowercase
 * en {@link Locale#ROOT} + colapso de espacios. Esto hace la búsqueda
 * insensible a mayúsculas, acentos y signos diacríticos sin necesidad de
 * tocar la base de datos: un usuario buscando {@code "rosalia"} encuentra a
 * {@code "Rosalía"} y a la inversa.
 *
 * <p><b>Matching.</b> La consulta normalizada se tokeniza por espacios y se
 * exige que <em>todos</em> los tokens aparezcan como subcadena del "haystack"
 * (texto normalizado con los campos buscables). Esto reproduce el comportamiento
 * orgánico de barras tipo YouTube/Spotify: "led zep" empareja con
 * "Led Zeppelin", "stairway heaven" con "Stairway to Heaven", etc.
 *
 * <p><b>Scoring.</b> Cuanto menor el score, más relevante. Se evalúa contra el
 * campo principal de cada categoría (título, nombre, etc.): 0 = match exacto,
 * 1 = empieza por la consulta, 2 = una palabra empieza por la consulta,
 * 3 = subcadena. Los empates se rompen por una métrica natural por categoría
 * (escuchas, conteo de canciones, alfabético).
 *
 * <p><b>Coste.</b> El servicio carga listas completas a memoria y filtra allí.
 * Es aceptable porque selfpotify es un servidor personal con catálogos
 * acotados; evita acoplarse a particularidades SQL (H2 / MariaDB no comparten
 * sintaxis para desdiacritizar) y mantiene una única fuente de verdad para la
 * normalización. Si en un futuro el catálogo crece, se puede sustituir por un
 * índice invertido sin cambiar el contrato del controlador.
 */
@Slf4j
@Service
public class SearchService {

    /** Tamaño de página por defecto cuando no llega el parámetro. */
    public static final int DEFAULT_PAGE_SIZE = 20;
    /** Cota dura para evitar respuestas gigantescas. */
    public static final int MAX_PAGE_SIZE = 100;
    /** Cuántos elementos por categoría se devuelven en modo {@code all}. */
    public static final int ALL_MODE_PER_CATEGORY = 5;

    @Autowired
    private SongRepository songRepository;
    @Autowired
    private ArtistRepository artistRepository;
    @Autowired
    private AlbumRepository albumRepository;
    @Autowired
    private PlaylistRepository playlistRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private SongService songService;

    /**
     * Punto de entrada del controlador. Despacha al modo (all o específico)
     * y devuelve siempre un {@link SearchResponseDTO} con la misma forma.
     *
     * @param rawQuery    consulta sin normalizar
     * @param type        "all" o categoría concreta
     * @param page        índice 0-based
     * @param size        tamaño de página (acotado por {@link #MAX_PAGE_SIZE})
     * @param currentUser usuario autenticado (para filtrar playlists privadas)
     */
    public SearchResponseDTO search(String rawQuery, String type, int page, int size, User currentUser) {
        String normalized = normalize(rawQuery);
        String mode = (type == null || type.isBlank()) ? "all" : type.toLowerCase(Locale.ROOT).trim();
        int safeSize = clampSize(size);
        int safePage = Math.max(0, page);

        SearchResponseDTO response = new SearchResponseDTO();
        response.setQuery(normalized);
        response.setType(mode);
        response.setPage(safePage);
        response.setSize(safeSize);

        if (normalized.isBlank()) {
            return emptyResponse(response, mode, safeSize);
        }

        String[] tokens = normalized.split("\\s+");

        switch (mode) {
            case "songs"     -> response.setSongs(searchSongs(tokens, safePage, safeSize));
            case "artists"   -> response.setArtists(searchArtists(tokens, safePage, safeSize));
            case "albums"    -> response.setAlbums(searchAlbums(tokens, safePage, safeSize));
            case "playlists" -> response.setPlaylists(searchPlaylists(tokens, safePage, safeSize, currentUser));
            case "users"     -> response.setUsers(searchUsers(tokens, safePage, safeSize));
            case "genres"    -> response.setGenres(searchGenres(tokens, safePage, safeSize));
            case "all" -> {
                int preview = Math.min(safeSize, ALL_MODE_PER_CATEGORY);
                response.setSize(preview);
                response.setSongs(searchSongs(tokens, 0, preview));
                response.setArtists(searchArtists(tokens, 0, preview));
                response.setAlbums(searchAlbums(tokens, 0, preview));
                response.setPlaylists(searchPlaylists(tokens, 0, preview, currentUser));
                response.setUsers(searchUsers(tokens, 0, preview));
                response.setGenres(searchGenres(tokens, 0, preview));
            }
            default -> throw new IllegalArgumentException(
                    "Tipo de búsqueda no soportado: " + mode +
                    ". Valores válidos: all, songs, artists, albums, playlists, users, genres.");
        }

        return response;
    }

    // =====================================
    // ----- Búsqueda por categoría
    // =====================================

    /**
     * Canciones: matchea contra título, nombres de artistas, álbum y género;
     * scoring sobre el título. Empate por escuchas desc → título asc.
     */
    private CategoryPage<SongDTO> searchSongs(String[] tokens, int page, int size) {
        Map<Long, Long> listenCounts = songService.getListenCountsBySong();
        List<Scored<Song>> matches = new ArrayList<>();
        for (Song s : songRepository.findAll()) {
            String haystack = haystackOfSong(s);
            if (!matchesAll(haystack, tokens)) continue;
            int score = scoreField(s.getTitle(), tokens);
            matches.add(new Scored<>(s, score, listenCounts.getOrDefault(s.getId(), 0L)));
        }
        matches.sort(Comparator
                .comparingInt(Scored<Song>::score)
                .thenComparing(Comparator.comparingLong((Scored<Song> sc) -> sc.tiebreak).reversed())
                .thenComparing(sc -> safe(sc.value.getTitle())));
        return slice(matches, page, size, sc -> SongDTO.fromEntity(sc.value, sc.tiebreak));
    }

    /**
     * Artistas: matchea contra nombre; scoring también sobre nombre. Empate
     * por número de canciones desc → nombre asc.
     */
    private CategoryPage<ArtistDTO> searchArtists(String[] tokens, int page, int size) {
        List<Scored<Artist>> matches = new ArrayList<>();
        for (Artist a : artistRepository.findAll()) {
            String haystack = normalize(a.getName());
            if (!matchesAll(haystack, tokens)) continue;
            int score = scoreField(a.getName(), tokens);
            long songCount = a.getSongs() == null ? 0 : a.getSongs().size();
            matches.add(new Scored<>(a, score, songCount));
        }
        matches.sort(Comparator
                .comparingInt(Scored<Artist>::score)
                .thenComparing(Comparator.comparingLong((Scored<Artist> sc) -> sc.tiebreak).reversed())
                .thenComparing(sc -> safe(sc.value.getName())));
        return slice(matches, page, size, sc -> toArtistDTO(sc.value));
    }

    /**
     * Álbumes: matchea contra nombre y artistas; scoring sobre nombre. Empate
     * por número de canciones desc → nombre asc.
     */
    private CategoryPage<AlbumDTO> searchAlbums(String[] tokens, int page, int size) {
        List<Scored<Album>> matches = new ArrayList<>();
        for (Album a : albumRepository.findAll()) {
            String haystack = haystackOfAlbum(a);
            if (!matchesAll(haystack, tokens)) continue;
            int score = scoreField(a.getName(), tokens);
            long songCount = a.getSongs() == null ? 0 : a.getSongs().size();
            matches.add(new Scored<>(a, score, songCount));
        }
        matches.sort(Comparator
                .comparingInt(Scored<Album>::score)
                .thenComparing(Comparator.comparingLong((Scored<Album> sc) -> sc.tiebreak).reversed())
                .thenComparing(sc -> safe(sc.value.getName())));
        return slice(matches, page, size, sc -> toAlbumDTO(sc.value));
    }

    /**
     * Playlists: solo las públicas o las del usuario autenticado. Matchea contra
     * nombre, descripción y username del creador.
     */
    private CategoryPage<PlaylistDTO> searchPlaylists(String[] tokens, int page, int size, User currentUser) {
        Long currentUserId = currentUser == null ? null : currentUser.getId();
        List<Scored<Playlist>> matches = new ArrayList<>();
        for (Playlist p : playlistRepository.findAll()) {
            boolean visible = p.isPublic()
                    || (currentUserId != null
                        && p.getCreator() != null
                        && currentUserId.equals(p.getCreator().getId()));
            if (!visible) continue;
            String haystack = haystackOfPlaylist(p);
            if (!matchesAll(haystack, tokens)) continue;
            int score = scoreField(p.getName(), tokens);
            long songCount = p.getSongs() == null ? 0 : p.getSongs().size();
            matches.add(new Scored<>(p, score, songCount));
        }
        matches.sort(Comparator
                .comparingInt(Scored<Playlist>::score)
                .thenComparing(Comparator.comparingLong((Scored<Playlist> sc) -> sc.tiebreak).reversed())
                .thenComparing(sc -> safe(sc.value.getName())));
        return slice(matches, page, size, sc -> toPlaylistDTO(sc.value));
    }

    /**
     * Usuarios: matchea contra username y nombre de perfil; el score es el
     * <em>mejor</em> match entre ambos campos para que buscar por el nombre
     * visible no penalice respecto a buscar por username. Empate alfabético
     * por username (más estable, único y siempre presente).
     */
    private CategoryPage<UserSummaryDTO> searchUsers(String[] tokens, int page, int size) {
        List<Scored<User>> matches = new ArrayList<>();
        for (User u : userRepository.findAll()) {
            String haystack = haystackOfUser(u);
            if (!matchesAll(haystack, tokens)) continue;
            int byUsername = scoreField(u.getUsername(), tokens);
            int byDisplayName = u.getProfile() == null
                    ? Integer.MAX_VALUE
                    : scoreField(u.getProfile().getName(), tokens);
            int score = Math.min(byUsername, byDisplayName);
            matches.add(new Scored<>(u, score, 0L));
        }
        matches.sort(Comparator
                .comparingInt(Scored<User>::score)
                .thenComparing(sc -> safe(sc.value.getUsername())));
        return slice(matches, page, size, sc -> UserSummaryDTO.fromEntity(sc.value));
    }

    /**
     * Géneros: trabaja sobre los géneros distintos del catálogo. Cuenta cuántas
     * canciones tiene cada uno con una única pasada. Empate por conteo desc.
     */
    private CategoryPage<GenreResultDTO> searchGenres(String[] tokens, int page, int size) {
        List<String> genres = songRepository.findDistinctGenres();
        Map<String, Long> countByGenre = songRepository.findAll().stream()
                .filter(s -> s.getGenre() != null && !s.getGenre().isBlank())
                .collect(Collectors.groupingBy(Song::getGenre, Collectors.counting()));

        List<Scored<String>> matches = new ArrayList<>();
        for (String g : genres) {
            if (!matchesAll(normalize(g), tokens)) continue;
            int score = scoreField(g, tokens);
            long count = countByGenre.getOrDefault(g, 0L);
            matches.add(new Scored<>(g, score, count));
        }
        matches.sort(Comparator
                .comparingInt(Scored<String>::score)
                .thenComparing(Comparator.comparingLong((Scored<String> sc) -> sc.tiebreak).reversed())
                .thenComparing(sc -> safe(sc.value)));
        return slice(matches, page, size, sc -> new GenreResultDTO(sc.value, sc.tiebreak));
    }

    // =====================================
    // ----- Normalización / matching / scoring
    // =====================================

    /**
     * Forma canónica de una cadena para la búsqueda: NFD + strip diacríticos +
     * lowercase root + colapso de espacios. Determinista e independiente del
     * locale del servidor. {@code null} se trata como vacío.
     */
    public static String normalize(String s) {
        if (s == null) return "";
        String nfd = Normalizer.normalize(s, Normalizer.Form.NFD);
        String stripped = nfd.replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        return stripped.toLowerCase(Locale.ROOT).trim().replaceAll("\\s+", " ");
    }

    /** {@code true} si todos los tokens están presentes (como subcadena) en {@code haystack}. */
    private static boolean matchesAll(String haystack, String[] tokens) {
        if (haystack.isEmpty()) return false;
        for (String t : tokens) {
            if (!t.isEmpty() && !haystack.contains(t)) return false;
        }
        return true;
    }

    /**
     * Score de relevancia respecto al campo principal. Valores bajos = mejores.
     * <ul>
     *   <li>0: el campo normalizado <em>es</em> la consulta unida por espacios.</li>
     *   <li>1: el campo empieza por la consulta unida.</li>
     *   <li>2: alguna palabra del campo empieza por el primer token.</li>
     *   <li>3: contiene los tokens (caso por defecto, ya garantizado por el filtro).</li>
     * </ul>
     */
    private static int scoreField(String field, String[] tokens) {
        String norm = normalize(field);
        if (norm.isEmpty() || tokens.length == 0) return 3;
        String joined = String.join(" ", tokens);
        if (norm.equals(joined)) return 0;
        if (norm.startsWith(joined)) return 1;
        for (String word : norm.split("\\s+")) {
            if (word.startsWith(tokens[0])) return 2;
        }
        return 3;
    }

    // =====================================
    // ----- Haystacks (texto buscable por entidad)
    // =====================================

    /** Título + artistas + álbum + género de la canción, todo normalizado. */
    private static String haystackOfSong(Song s) {
        StringBuilder sb = new StringBuilder();
        appendNormalized(sb, s.getTitle());
        if (s.getArtists() != null) {
            for (Artist a : s.getArtists()) appendNormalized(sb, a.getName());
        }
        if (s.getAlbum() != null) appendNormalized(sb, s.getAlbum().getName());
        appendNormalized(sb, s.getGenre());
        return sb.toString();
    }

    /** Nombre del álbum + nombres de artistas. */
    private static String haystackOfAlbum(Album a) {
        StringBuilder sb = new StringBuilder();
        appendNormalized(sb, a.getName());
        if (a.getArtists() != null) {
            for (Artist ar : a.getArtists()) appendNormalized(sb, ar.getName());
        }
        return sb.toString();
    }

    /** Nombre + descripción + username del creador. */
    private static String haystackOfPlaylist(Playlist p) {
        StringBuilder sb = new StringBuilder();
        appendNormalized(sb, p.getName());
        appendNormalized(sb, p.getDescription());
        if (p.getCreator() != null) appendNormalized(sb, p.getCreator().getUsername());
        return sb.toString();
    }

    /** Username + nombre del perfil. */
    private static String haystackOfUser(User u) {
        StringBuilder sb = new StringBuilder();
        appendNormalized(sb, u.getUsername());
        if (u.getProfile() != null) appendNormalized(sb, u.getProfile().getName());
        return sb.toString();
    }

    private static void appendNormalized(StringBuilder sb, String fragment) {
        if (fragment == null || fragment.isBlank()) return;
        if (sb.length() > 0) sb.append(' ');
        sb.append(normalize(fragment));
    }

    // =====================================
    // ----- Paginación + mapeo a DTO
    // =====================================

    private static <T, R> CategoryPage<R> slice(List<T> all, int page, int size, Function<T, R> mapper) {
        long total = all.size();
        int totalPages = size <= 0 ? 1 : (int) Math.max(1, (total + size - 1) / size);
        if (total == 0) {
            return new CategoryPage<>(Collections.emptyList(), 0L, 1);
        }
        int from = Math.min(page * size, all.size());
        int to = Math.min(from + size, all.size());
        List<R> mapped = all.subList(from, to).stream().map(mapper).collect(Collectors.toList());
        return new CategoryPage<>(mapped, total, totalPages);
    }

    private static int clampSize(int size) {
        if (size <= 0) return DEFAULT_PAGE_SIZE;
        return Math.min(size, MAX_PAGE_SIZE);
    }

    private static String safe(String s) {
        return s == null ? "" : s;
    }

    /**
     * Respuesta vacía pero con la forma esperada cuando la consulta es vacía.
     * En modo {@code all} se devuelven todas las categorías vacías para que el
     * cliente no tenga que ramificar el render; en modo específico, solo la
     * categoría pedida.
     */
    private SearchResponseDTO emptyResponse(SearchResponseDTO response, String mode, int size) {
        CategoryPage<?> empty = new CategoryPage<>(Collections.emptyList(), 0L, 1);
        switch (mode) {
            case "songs"     -> response.setSongs(cast(empty));
            case "artists"   -> response.setArtists(cast(empty));
            case "albums"    -> response.setAlbums(cast(empty));
            case "playlists" -> response.setPlaylists(cast(empty));
            case "users"     -> response.setUsers(cast(empty));
            case "genres"    -> response.setGenres(cast(empty));
            case "all" -> {
                response.setSongs(cast(empty));
                response.setArtists(cast(empty));
                response.setAlbums(cast(empty));
                response.setPlaylists(cast(empty));
                response.setUsers(cast(empty));
                response.setGenres(cast(empty));
            }
            default -> throw new IllegalArgumentException("Tipo desconocido: " + mode);
        }
        return response;
    }

    @SuppressWarnings("unchecked")
    private static <T> CategoryPage<T> cast(CategoryPage<?> empty) {
        return (CategoryPage<T>) empty;
    }

    // =====================================
    // ----- Mappers a DTO (sin tocar los controllers existentes)
    // =====================================

    private static ArtistDTO toArtistDTO(Artist artist) {
        ArtistDTO dto = new ArtistDTO();
        dto.setId(artist.getId());
        dto.setName(artist.getName());
        dto.setPhotoUrl(artist.getPicture_path());
        if (artist.getAlbums() != null) {
            dto.setAlbumIds(artist.getAlbums().stream().map(Album::getId).collect(Collectors.toList()));
        }
        if (artist.getSongs() != null) {
            dto.setSongIds(artist.getSongs().stream().map(Song::getId).collect(Collectors.toList()));
        }
        return dto;
    }

    private static AlbumDTO toAlbumDTO(Album album) {
        AlbumDTO dto = new AlbumDTO();
        dto.setId(album.getId());
        dto.setName(album.getName());
        dto.setPictureUrl(album.getPicture_url());
        if (album.getSongs() != null) {
            dto.setSongIds(album.getSongs().stream().map(Song::getId).collect(Collectors.toList()));
        }
        return dto;
    }

    private static PlaylistDTO toPlaylistDTO(Playlist playlist) {
        PlaylistDTO dto = new PlaylistDTO();
        dto.setId(playlist.getId());
        dto.setName(playlist.getName());
        dto.setDescription(playlist.getDescription());
        dto.setPublic(playlist.isPublic());
        dto.setPictureUrl(playlist.getPictureUrl());
        if (playlist.getCreator() != null) {
            dto.setCreatorId(playlist.getCreator().getId());
        }
        dto.setSongIds(playlist.getSongs() == null
                ? Collections.emptyList()
                : playlist.getSongs().stream().map(Song::getId).collect(Collectors.toList()));
        return dto;
    }

    /**
     * Tupla interna entidad + score + tiebreaker (escuchas o conteo). El
     * tiebreaker se almacena junto al score para no recalcularlo durante la
     * ordenación y, en el caso de las canciones, viajar luego al DTO como
     * {@code listeners} sin volver a contar.
     */
    private record Scored<T>(T value, int score, long tiebreak) {}
}
