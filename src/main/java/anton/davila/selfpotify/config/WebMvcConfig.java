package anton.davila.selfpotify.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Autowired
    private ConfigService configService;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String location = "file:" + configService.assetsDir().toAbsolutePath() + "/";
        registry.addResourceHandler("/assets/**")
                .addResourceLocations(location)
                .setCachePeriod(60);
    }
}
