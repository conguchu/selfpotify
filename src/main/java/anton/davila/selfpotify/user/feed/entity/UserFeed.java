package anton.davila.selfpotify.user.feed.entity;

import anton.davila.selfpotify.music.entity.Artist;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OrderColumn;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Entity
@Data
public class UserFeed {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // un feed recomienda varios artistas; un artista puede aparecer en varios feeds
    @ManyToMany
    private List<Artist> recommendedArtists = new ArrayList<>();

    // capacidad máxima de la pila de géneros
    private static final int MAX_GENEROS = 20;

    // guarda los ultimos generos escuchados por el usuario.
    // cada vez que se escucha una canción, se almacena su género en la pila.
    // se comporta como una pila acotada: la cabeza (índice 0) es el último
    // escuchado y, al superar MAX_GENEROS, se descarta el más antiguo.
    @ElementCollection
    @OrderColumn(name = "posicion")
    @CollectionTable(name = "feed_generos",
            joinColumns = @JoinColumn(name = "feed_id"))
    private List<String> last20GenresListened = new ArrayList<>();

    /**
     * Apila un género como el más recientemente escuchado. Mantiene el orden
     * cronológico (índice 0 = más reciente) y descarta el más antiguo si se
     * supera la capacidad máxima.
     *
     * @param genero género de la canción recién escuchada
     */
    public void pushGenero(String genero) {
        last20GenresListened.add(0, genero);
        if (last20GenresListened.size() > MAX_GENEROS) {
            last20GenresListened.remove(last20GenresListened.size() - 1);
        }
    }

    public void copy(UserFeed f) {
        this.setRecommendedArtists(f.getRecommendedArtists());
    }
}
