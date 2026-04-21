package anton.davila.selfpotify.repository;

import anton.davila.selfpotify.entity.music.Album;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AlbumRepository extends JpaRepository<Album, Long> {
}
