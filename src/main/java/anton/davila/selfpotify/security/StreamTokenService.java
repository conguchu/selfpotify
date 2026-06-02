package anton.davila.selfpotify.security;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class StreamTokenService {

    // Long enough to cover a full listening session without re-issuing on every seek.
    // The token is scoped to /api/listen only and carries no JWT claims.
    private static final long TTL_MS = 4 * 60 * 60 * 1000L; // 4 hours

    private record Entry(String username, long expiry) {}

    private final ConcurrentHashMap<String, Entry> tokens = new ConcurrentHashMap<>();

    public String issue(String username) {
        String token = UUID.randomUUID().toString();
        tokens.put(token, new Entry(username, System.currentTimeMillis() + TTL_MS));
        return token;
    }

    /**
     * Validates the token and returns the associated username, or null if invalid/expired.
     * Tokens are reusable within their TTL to support HTTP range requests (seeks).
     */
    public String validate(String token) {
        if (token == null || token.isBlank()) return null;
        Entry entry = tokens.get(token);
        if (entry == null || System.currentTimeMillis() >= entry.expiry()) {
            tokens.remove(token);
            return null;
        }
        return entry.username();
    }

    @Scheduled(fixedDelay = 60_000)
    public void evictExpired() {
        long now = System.currentTimeMillis();
        tokens.entrySet().removeIf(e -> e.getValue().expiry() < now);
    }
}
