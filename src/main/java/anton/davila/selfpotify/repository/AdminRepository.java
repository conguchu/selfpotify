package anton.davila.selfpotify.repository;

import anton.davila.selfpotify.entity.user.Admin;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdminRepository extends JpaRepository<Admin, Long> {
}
