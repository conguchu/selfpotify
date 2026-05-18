package anton.davila.selfpotify.music.repository;

import anton.davila.selfpotify.music.entity.Artist;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

import java.util.List;

public interface ArtistRepository extends JpaRepository<Artist, Long> {

    Optional<Artist> findByNameIgnoreCase(String name);

    Optional<Artist> findByMbid(String mbid);

    List<Artist> findTop10ByOrderByListenersDesc();

    @Modifying
    @Transactional
    @Query("update Artist a set a.listeners = a.listeners + 1 where a.id = :id")
    void incrementListeners(@Param("id") Long id);
}
