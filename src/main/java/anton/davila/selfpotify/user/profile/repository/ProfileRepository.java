package anton.davila.selfpotify.user.profile.repository;

import anton.davila.selfpotify.user.profile.entity.Profile;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProfileRepository extends JpaRepository<Profile, Long> {
}
