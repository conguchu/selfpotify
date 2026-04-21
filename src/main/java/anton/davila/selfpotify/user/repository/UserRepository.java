package anton.davila.selfpotify.user.repository;

import anton.davila.selfpotify.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
}
