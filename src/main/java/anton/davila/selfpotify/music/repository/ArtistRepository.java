package anton.davila.selfpotify.music.repository;

import anton.davila.selfpotify.music.entity.Artist;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ArtistRepository extends JpaRepository<Artist, Long> {
}
