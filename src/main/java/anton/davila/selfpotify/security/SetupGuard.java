package anton.davila.selfpotify.security;

import anton.davila.selfpotify.config.ConfigService;
import org.springframework.stereotype.Component;

/**
 * Guard del "modo setup": expone {@link #inSetupMode()} para que la seguridad
 * (a nivel de método con {@code @setupGuard.inSetupMode()}) permita el acceso sin
 * login a los endpoints que usa el wizard de configuración inicial mientras
 * {@code features.setupComplete} sea {@code false}. Cuando el setup se completa,
 * el guard devuelve {@code false} y esos endpoints vuelven a exigir rol ADMIN.
 */
@Component("setupGuard")
public class SetupGuard {

    private final ConfigService configService;

    public SetupGuard(ConfigService configService) {
        this.configService = configService;
    }

    public boolean inSetupMode() {
        return !configService.getConfig().getFeatures().isSetupComplete();
    }
}
