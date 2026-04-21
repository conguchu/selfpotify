package anton.davila.selfpotify.repository;

import anton.davila.selfpotify.entity.music.Artist;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ArtistRepository extends JpaRepository<Artist, Long> {
}
