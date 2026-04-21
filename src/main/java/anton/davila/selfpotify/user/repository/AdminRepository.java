package anton.davila.selfpotify.user.repository;

import anton.davila.selfpotify.user.entity.Admin;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdminRepository extends JpaRepository<Admin, Long> {
}
