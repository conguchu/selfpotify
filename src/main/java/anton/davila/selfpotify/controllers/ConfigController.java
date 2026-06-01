package anton.davila.selfpotify.controllers;

import anton.davila.selfpotify.ServerGlobalConfig;
import anton.davila.selfpotify.config.AppProperties;
import anton.davila.selfpotify.config.ConfigService;
import anton.davila.selfpotify.config.MusicLibraryResolver;
import anton.davila.selfpotify.config.ResetService;
import anton.davila.selfpotify.config.ScanService;
import anton.davila.selfpotify.controllers.dto.BrandingDTO;
import anton.davila.selfpotify.controllers.dto.ConfigUpdateRequest;
import anton.davila.selfpotify.controllers.dto.PublicConfigDTO;
import anton.davila.selfpotify.controllers.dto.RescanResultDTO;
import anton.davila.selfpotify.controllers.dto.ScanPathRequest;
import anton.davila.selfpotify.controllers.dto.ServerConfigDTO;
import anton.davila.selfpotify.controllers.dto.SetupRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

@Slf4j
@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/config")
public class ConfigController {

    private static final Pattern HEX_COLOR = Pattern.compile("^#([0-9a-fA-F]{3}|[0-9a-fA-F]{6})$");
    private static final long MIN_INTERVAL = 30L;
    private static final long MAX_INTERVAL = 86400L;
    private static final int MAX_APP_NAME = 64;
    private static final Map<String, String> ACCEPTED_LOGO_MIME = Map.of(
            "image/png", "png",
            "image/jpeg", "jpg",
            "image/svg+xml", "svg",
            "image/webp", "webp"
    );

    @Autowired
    private ConfigService configService;

    @Autowired
    private ScanService scanService;

    @Autowired
    private ResetService resetService;

    @Autowired
    private AppProperties appProperties;

    @Autowired
    private MusicLibraryResolver musicLibraryResolver;

    @GetMapping("/public")
    public PublicConfigDTO getPublic() {
        ServerGlobalConfig cfg = configService.getConfig();
        ServerGlobalConfig.Branding b = cfg.getBranding();
        boolean lastfmEnabled = appProperties.getLastfm().getApiKey() != null
                && !appProperties.getLastfm().getApiKey().isBlank();
        boolean coverArtEnabled = appProperties.getCoverArt().isEnabled();
        String musicLibraryPath = musicLibraryResolver.resolvePath().orElse(null);
        return new PublicConfigDTO(
                new BrandingDTO(b.getAppName(), b.getLogoUrl(), b.getColors()),
                cfg.getFeatures().isSetupComplete(),
                lastfmEnabled,
                coverArtEnabled,
                musicLibraryPath,
                appProperties.getLogo().getMaxFileSize().toBytes()
        );
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ServerConfigDTO getFull() {
        return toDTO(configService.getConfig());
    }

    @PutMapping
    @PreAuthorize("hasRole('ADMIN') or @setupGuard.inSetupMode()")
    public ServerConfigDTO update(@RequestBody ConfigUpdateRequest req) {
        if (req == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Body requerido");
        }
        if (req.getBranding() != null) {
            BrandingDTO b = req.getBranding();
            String appName = b.getAppName();
            if (appName != null) {
                if (appName.isBlank() || appName.length() > MAX_APP_NAME) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "appName no puede estar vacío ni superar " + MAX_APP_NAME + " caracteres");
                }
            }
            Map<String, String> colors = b.getColors();
            if (colors != null) {
                for (Map.Entry<String, String> e : colors.entrySet()) {
                    if (e.getValue() == null || !HEX_COLOR.matcher(e.getValue()).matches()) {
                        throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                                "Color hex inválido para '" + e.getKey() + "': " + e.getValue());
                    }
                }
            }
            configService.updateBranding(appName, colors);
        }
        if (req.getAutoCompleteMetadata() != null || req.getAutoCompleteCoverArt() != null) {
            configService.updateFeatures(req.getAutoCompleteMetadata(), req.getAutoCompleteCoverArt());
        }
        if (req.getScanIntervalSeconds() != null) {
            long s = req.getScanIntervalSeconds();
            if (s < MIN_INTERVAL || s > MAX_INTERVAL) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "scanIntervalSeconds debe estar entre " + MIN_INTERVAL + " y " + MAX_INTERVAL);
            }
            configService.updateScanInterval(s);
        }
        return toDTO(configService.getConfig());
    }

    @PostMapping("/scan-paths")
    @PreAuthorize("hasRole('ADMIN')")
    public ServerConfigDTO addScanPath(@RequestBody ScanPathRequest req) {
        String raw = req == null ? null : req.getPath();
        if (raw == null || raw.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El campo 'path' es obligatorio");
        }
        Path folder = Paths.get(raw).toAbsolutePath().normalize();
        if (!Files.exists(folder) || !Files.isDirectory(folder) || !Files.isReadable(folder)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "La ruta no existe, no es un directorio o no es legible: " + raw);
        }
        boolean added = configService.addScanPath(folder.toString());
        if (!added) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "La ruta ya está en la lista de escaneo");
        }
        scanService.runScanForPath(folder.toString());
        return toDTO(configService.getConfig());
    }

    @DeleteMapping("/scan-paths")
    @PreAuthorize("hasRole('ADMIN')")
    public ServerConfigDTO removeScanPath(@RequestParam("path") String raw) {
        if (raw == null || raw.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El parámetro 'path' es obligatorio");
        }
        String normalized = Paths.get(raw).toAbsolutePath().normalize().toString();
        boolean removed = configService.removeScanPath(normalized);
        if (!removed) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "La ruta no estaba registrada: " + normalized);
        }
        return toDTO(configService.getConfig());
    }

    @PostMapping("/logo")
    @PreAuthorize("hasRole('ADMIN') or @setupGuard.inSetupMode()")
    public BrandingDTO uploadLogo(@RequestParam("file") MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Archivo requerido");
        }
        long maxBytes = appProperties.getLogo().getMaxFileSize().toBytes();
        if (file.getSize() > maxBytes) {
            throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE,
                    "El archivo excede el tamaño máximo permitido ("
                            + appProperties.getLogo().getMaxFileSize().toMegabytes() + " MB)");
        }
        String mime = file.getContentType();
        String ext = ACCEPTED_LOGO_MIME.get(mime);
        if (ext == null) {
            throw new ResponseStatusException(HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                    "MIME no soportado: " + mime + ". Aceptados: " + ACCEPTED_LOGO_MIME.keySet());
        }
        deleteExistingLogos();
        Path target = configService.assetsDir().resolve("logo." + ext);
        try {
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            log.error("No se pudo guardar el logo en {}", target, e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "No se pudo guardar el logo");
        }
        String url = "/assets/logo." + ext;
        configService.setLogoUrl(url);
        ServerGlobalConfig.Branding b = configService.getConfig().getBranding();
        return new BrandingDTO(b.getAppName(), b.getLogoUrl(), b.getColors());
    }

    @PostMapping("/scan/run")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> runScanNow() {
        boolean ran = scanService.runScan();
        if (!ran) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya hay un escaneo en curso");
        }
        Map<String, Object> body = new HashMap<>();
        body.put("status", "ok");
        body.put("lastRunEpochSec", configService.getConfig().getScan().getLastRunEpochSec());
        return ResponseEntity.ok(body);
    }

    @PostMapping("/scan/rescan")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<RescanResultDTO> rescan() {
        RescanResultDTO result = scanService.rescan();
        if (result == null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya hay un escaneo en curso");
        }
        return ResponseEntity.ok(result);
    }

    private void deleteExistingLogos() {
        Set<String> exts = Set.copyOf(ACCEPTED_LOGO_MIME.values());
        for (String e : exts) {
            try {
                Files.deleteIfExists(configService.assetsDir().resolve("logo." + e));
            } catch (IOException ex) {
                log.warn("No se pudo borrar logo previo .{}", e);
            }
        }
    }

    @PostMapping("/setup")
    @PreAuthorize("hasRole('ADMIN') or @setupGuard.inSetupMode()")
    public ServerConfigDTO setup(@RequestBody SetupRequest req) {
        if (configService.getConfig().getFeatures().isSetupComplete()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El servidor ya está configurado");
        }
        if (req == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Body requerido");
        }
        if (req.getAppName() != null) {
            String name = req.getAppName();
            if (name.isBlank() || name.length() > MAX_APP_NAME) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "appName no puede estar vacío ni superar " + MAX_APP_NAME + " caracteres");
            }
            configService.updateBranding(name, null);
        }
        if (req.getAutoCompleteMetadata() != null || req.getAutoCompleteCoverArt() != null) {
            configService.updateFeatures(req.getAutoCompleteMetadata(), req.getAutoCompleteCoverArt());
        }
        if (req.getScanIntervalSeconds() != null) {
            long s = req.getScanIntervalSeconds();
            if (s < MIN_INTERVAL || s > MAX_INTERVAL) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "scanIntervalSeconds debe estar entre " + MIN_INTERVAL + " y " + MAX_INTERVAL);
            }
            configService.updateScanInterval(s);
        }
        if (req.getScanPaths() != null) {
            for (String raw : req.getScanPaths()) {
                if (raw == null || raw.isBlank()) continue;
                Path folder = Paths.get(raw).toAbsolutePath().normalize();
                if (!Files.exists(folder) || !Files.isDirectory(folder) || !Files.isReadable(folder)) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "La ruta no existe, no es un directorio o no es legible: " + raw);
                }
                configService.addScanPath(folder.toString());
            }
        }
        configService.markSetupComplete();
        // Escaneo inicial asíncrono si hay CUALQUIER ruta configurada (incluida la
        // librería auto-añadida del .env), no solo las que vengan en el body.
        if (!configService.getConfig().getScan().getPaths().isEmpty()) {
            java.util.concurrent.CompletableFuture.runAsync(() -> scanService.runScan());
        }
        return toDTO(configService.getConfig());
    }

    @PostMapping("/reset")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> reset() {
        try {
            resetService.resetAll();
        } catch (IOException e) {
            log.error("Error reseteando el servidor", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "No se pudo completar el reset");
        }
        Map<String, Object> body = new HashMap<>();
        body.put("status", "ok");
        body.put("message", "Servidor reseteado. Vuelve a iniciar sesión con el admin definido en el .env");
        return ResponseEntity.ok(body);
    }

    private ServerConfigDTO toDTO(ServerGlobalConfig cfg) {
        ServerGlobalConfig.Branding b = cfg.getBranding();
        return new ServerConfigDTO(
                new BrandingDTO(b.getAppName(), b.getLogoUrl(), b.getColors()),
                cfg.getFeatures().isAutoCompleteMetadata(),
                cfg.getFeatures().isAutoCompleteCoverArt(),
                cfg.getFeatures().isSetupComplete(),
                cfg.getScan().getPaths(),
                cfg.getScan().getIntervalSeconds(),
                cfg.getScan().getLastRunEpochSec(),
                musicLibraryResolver.runningInDocker()
        );
    }
}
