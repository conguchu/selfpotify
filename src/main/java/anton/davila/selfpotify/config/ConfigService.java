package anton.davila.selfpotify.config;

import anton.davila.selfpotify.ServerGlobalConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@Service
public class ConfigService {

    private final ObjectMapper yamlMapper = new ObjectMapper(
            new YAMLFactory()
                    .disable(YAMLGenerator.Feature.WRITE_DOC_START_MARKER)
                    .enable(YAMLGenerator.Feature.MINIMIZE_QUOTES)
    );

    private final Object writeLock = new Object();

    private volatile ServerGlobalConfig current;
    private Path configPath;
    private Path assetsDir;

    @Value("${app.config.path:#{systemProperties['user.home'] + '/.selfpotify/config.yml'}}")
    private String configPathProp;

    /** Inicializa rutas, crea el directorio de assets y carga (o crea) el config.yml. */
    @PostConstruct
    public void init() throws IOException {
        this.configPath = Paths.get(configPathProp).toAbsolutePath().normalize();
        this.assetsDir = configPath.getParent().resolve("assets");
        Files.createDirectories(configPath.getParent());
        Files.createDirectories(assetsDir);

        if (Files.exists(configPath)) {
            this.current = yamlMapper.readValue(configPath.toFile(), ServerGlobalConfig.class);
            mergeColorDefaults(this.current);
            log.info("Loaded config from {}", configPath);
        } else {
            this.current = new ServerGlobalConfig();
            persistInternal(this.current);
            log.info("Created blank config at {} (setupComplete=false)", configPath);
        }
    }

    private void mergeColorDefaults(ServerGlobalConfig cfg) {
        Map<String, String> defaults = ServerGlobalConfig.Branding.defaultColors();
        Map<String, String> existing = cfg.getBranding().getColors();
        if (existing == null) {
            cfg.getBranding().setColors(defaults);
            return;
        }
        Map<String, String> merged = new LinkedHashMap<>(defaults);
        merged.putAll(existing);
        cfg.getBranding().setColors(merged);
    }

    /** Devuelve la configuración actual cargada en memoria. */
    public ServerGlobalConfig getConfig() {
        return current;
    }

    /** Directorio donde se almacenan los assets servidos en /assets/**. */
    public Path assetsDir() {
        return assetsDir;
    }

    /** Ruta absoluta del fichero config.yml. */
    public Path configPath() {
        return configPath;
    }

    /** Actualiza el nombre de la app y/o los colores de branding y persiste a disco. */
    public void updateBranding(String appName, Map<String, String> colors) {
        synchronized (writeLock) {
            ServerGlobalConfig cfg = copy(current);
            if (appName != null) cfg.getBranding().setAppName(appName);
            if (colors != null && !colors.isEmpty()) {
                cfg.getBranding().getColors().putAll(colors);
            }
            persistInternal(cfg);
            current = cfg;
        }
    }

    /** Activa o desactiva el autocompletado de metadatos/carátulas y persiste el cambio. */
    public void updateFeatures(Boolean autoCompleteMetadata, Boolean autoCompleteCoverArt) {
        synchronized (writeLock) {
            ServerGlobalConfig cfg = copy(current);
            if (autoCompleteMetadata != null) cfg.getFeatures().setAutoCompleteMetadata(autoCompleteMetadata);
            if (autoCompleteCoverArt != null) cfg.getFeatures().setAutoCompleteCoverArt(autoCompleteCoverArt);
            persistInternal(cfg);
            current = cfg;
        }
    }

    /** Cambia en caliente el intervalo (en segundos) del escaneo periódico. */
    public void updateScanInterval(long intervalSeconds) {
        synchronized (writeLock) {
            ServerGlobalConfig cfg = copy(current);
            cfg.getScan().setIntervalSeconds(intervalSeconds);
            persistInternal(cfg);
            current = cfg;
        }
    }

    /** Añade una ruta a escanear. Devuelve false si ya estaba registrada. */
    public boolean addScanPath(String absolutePath) {
        synchronized (writeLock) {
            ServerGlobalConfig cfg = copy(current);
            if (cfg.getScan().getPaths().contains(absolutePath)) {
                return false;
            }
            cfg.getScan().getPaths().add(absolutePath);
            persistInternal(cfg);
            current = cfg;
            return true;
        }
    }

    /** Elimina una ruta de escaneo. Devuelve true si existía y fue eliminada. */
    public boolean removeScanPath(String absolutePath) {
        synchronized (writeLock) {
            ServerGlobalConfig cfg = copy(current);
            boolean removed = cfg.getScan().getPaths().remove(absolutePath);
            if (removed) {
                persistInternal(cfg);
                current = cfg;
            }
            return removed;
        }
    }

    /** Establece la URL (o ruta /assets/...) del logo de la app. */
    public void setLogoUrl(String logoUrl) {
        synchronized (writeLock) {
            ServerGlobalConfig cfg = copy(current);
            cfg.getBranding().setLogoUrl(logoUrl);
            persistInternal(cfg);
            current = cfg;
        }
    }

    /** Registra el timestamp (epoch en segundos) del último escaneo finalizado. */
    public void markScanFinished(long epochSec) {
        synchronized (writeLock) {
            ServerGlobalConfig cfg = copy(current);
            cfg.getScan().setLastRunEpochSec(epochSec);
            persistInternal(cfg);
            current = cfg;
        }
    }

    /** Marca el setup inicial como completado para no volver a mostrarlo. */
    public void markSetupComplete() {
        synchronized (writeLock) {
            ServerGlobalConfig cfg = copy(current);
            cfg.getFeatures().setSetupComplete(true);
            persistInternal(cfg);
            current = cfg;
        }
    }

    /** Borra assets y config.yml y regenera una configuración en blanco por defecto. */
    public void resetToDefaults() throws IOException {
        synchronized (writeLock) {
            if (Files.isDirectory(assetsDir)) {
                try (var stream = Files.list(assetsDir)) {
                    stream.forEach(p -> {
                        try {
                            Files.deleteIfExists(p);
                        } catch (IOException e) {
                            log.warn("No se pudo borrar asset {}", p, e);
                        }
                    });
                }
            }
            Files.deleteIfExists(configPath);
            this.current = new ServerGlobalConfig();
            persistInternal(this.current);
            log.info("Config reseteada a valores por defecto en {}", configPath);
        }
    }

    private ServerGlobalConfig copy(ServerGlobalConfig src) {
        try {
            byte[] bytes = yamlMapper.writeValueAsBytes(src);
            return yamlMapper.readValue(bytes, ServerGlobalConfig.class);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to deep-copy config", e);
        }
    }

    private void persistInternal(ServerGlobalConfig cfg) {
        Path tmp = configPath.resolveSibling(configPath.getFileName().toString() + ".tmp");
        try {
            yamlMapper.writeValue(tmp.toFile(), cfg);
            try {
                Files.move(tmp, configPath, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (IOException atomicFail) {
                Files.move(tmp, configPath, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            throw new IllegalStateException("Failed to persist config to " + configPath, e);
        }
    }
}
