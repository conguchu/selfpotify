package anton.davila.selfpotify.music.repository;

import anton.davila.selfpotify.music.entity.Album;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AlbumRepository extends JpaRepository<Album, Long> {
}
