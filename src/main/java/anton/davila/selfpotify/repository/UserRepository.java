package anton.davila.selfpotify.repository;

import anton.davila.selfpotify.entity.user.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
}
