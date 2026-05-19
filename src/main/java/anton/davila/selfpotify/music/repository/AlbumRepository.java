package anton.davila.selfpotify.music.repository;

import anton.davila.selfpotify.music.entity.Album;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AlbumRepository extends JpaRepository<Album, Long> {

    @Modifying
    @Transactional
    @Query("update Album a set a.listeners = a.listeners + 1 where a.id = :id")
    void incrementListeners(@Param("id") Long id);
}
