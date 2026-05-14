package anton.davila.selfpotify;

import anton.davila.selfpotify.config.AppProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(AppProperties.class)
public class SelfpotifyServer {

    public static void main(String[] args) {
        SpringApplication.run(SelfpotifyServer.class, args);
    }

}
