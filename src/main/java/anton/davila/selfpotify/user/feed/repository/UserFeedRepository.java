package anton.davila.selfpotify.user.feed.repository;

import anton.davila.selfpotify.user.feed.entity.UserFeed;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserFeedRepository extends JpaRepository<UserFeed, Long> {
}
