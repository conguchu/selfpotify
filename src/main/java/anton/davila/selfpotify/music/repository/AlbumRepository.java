package anton.davila.selfpotify.music.repository;

import anton.davila.selfpotify.music.entity.Album;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AlbumRepository extends JpaRepository<Album, Long> {

    Optional<Album> findByNameIgnoreCase(String name);
}
