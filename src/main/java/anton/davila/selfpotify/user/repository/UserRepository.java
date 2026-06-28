package anton.davila.selfpotify.user.repository;

import anton.davila.selfpotify.user.entity.User;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);

    /**
     * Todos los usuarios con su {@code profile} ya resuelto para la búsqueda en
     * memoria, que matchea y puntúa contra el nombre del perfil además del
     * username. Evita el N+1 de cargar cada perfil por separado al recorrer el
     * catálogo de usuarios.
     */
    @EntityGraph(attributePaths = {"profile"})
    @Query("select u from User u")
    List<User> findAllForSearch();

    @Modifying
    @Query(value = "UPDATE users SET type = :type WHERE id = :id", nativeQuery = true)
    int updateUserType(@Param("id") Long id, @Param("type") String type);
}
