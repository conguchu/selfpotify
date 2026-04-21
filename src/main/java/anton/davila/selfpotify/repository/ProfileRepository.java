package anton.davila.selfpotify.repository;

import anton.davila.selfpotify.entity.user.profile.Profile;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProfileRepository extends JpaRepository<Profile, Long> {
}
