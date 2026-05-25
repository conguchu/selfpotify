package anton.davila.selfpotify.user.repository;

import anton.davila.selfpotify.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);

    @Modifying
    @Query(value = "UPDATE users SET type = :type WHERE id = :id", nativeQuery = true)
    int updateUserType(@Param("id") Long id, @Param("type") String type);
}
