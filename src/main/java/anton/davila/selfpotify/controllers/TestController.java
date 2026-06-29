package anton.davila.selfpotify.controllers;

import org.springframework.context.annotation.Profile;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// Endpoints de diagnóstico de auth: solo fuera del perfil "prod" (no deben existir
// en un despliegue productivo). El CORS lo aplica la configuración global.
@Profile("!prod")
@RestController
@RequestMapping("/api/test")
public class TestController {
    @GetMapping("/public")
    public String allAccess() {
        return "Public Content.";
    }

    @GetMapping("/user")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public String userAccess() {
        return "User Content.";
    }

    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public String adminAccess() {
        return "Admin Board.";
    }
}
