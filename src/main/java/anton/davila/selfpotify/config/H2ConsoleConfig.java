package anton.davila.selfpotify.config;

import org.h2.server.web.JakartaWebServlet;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(name = "spring.h2.console.enabled", havingValue = "true")
public class H2ConsoleConfig {

    @Bean
    public ServletRegistrationBean<JakartaWebServlet> h2ConsoleServlet(
            @Value("${spring.h2.console.path:/h2-console}") String path) {
        String mapping = path.endsWith("/*") ? path : path + "/*";
        ServletRegistrationBean<JakartaWebServlet> reg =
                new ServletRegistrationBean<>(new JakartaWebServlet(), mapping);
        reg.setLoadOnStartup(1);
        return reg;
    }
}
