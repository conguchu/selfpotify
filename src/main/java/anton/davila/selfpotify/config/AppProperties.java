package anton.davila.selfpotify.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "app")
public class AppProperties {

    private Admin admin = new Admin();
    private Jwt jwt = new Jwt();
    private Web web = new Web();

    @Getter
    @Setter
    public static class Admin {
        private String username = "";
        private String password = "";
    }

    @Getter
    @Setter
    public static class Jwt {
        private String secret = "";
        private long expirationMs = 86_400_000L;
    }

    @Getter
    @Setter
    public static class Web {
        private String origin = "http://localhost:3000";
    }
}
