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

    @Value("${app.music.import-folder:#{systemProperties['user.home'] + '/Downloads'}}")
    private String legacyImportFolder;

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
            this.current = buildDefaults();
            persistInternal(this.current);
            log.info("Created default config at {}", configPath);
        }
    }

    private ServerGlobalConfig buildDefaults() {
        ServerGlobalConfig cfg = new ServerGlobalConfig();
        Path seed = Paths.get(legacyImportFolder);
        if (Files.isDirectory(seed) && Files.isReadable(seed)) {
            cfg.getScan().getPaths().add(seed.toAbsolutePath().normalize().toString());
            log.info("Seeded scan path from app.music.import-folder: {}", seed);
        }
        return cfg;
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

    public ServerGlobalConfig getConfig() {
        return current;
    }

    public Path assetsDir() {
        return assetsDir;
    }

    public Path configPath() {
        return configPath;
    }

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

    public void updateFeatures(Boolean autoCompleteMetadata) {
        synchronized (writeLock) {
            ServerGlobalConfig cfg = copy(current);
            if (autoCompleteMetadata != null) cfg.getFeatures().setAutoCompleteMetadata(autoCompleteMetadata);
            persistInternal(cfg);
            current = cfg;
        }
    }

    public void updateScanInterval(long intervalSeconds) {
        synchronized (writeLock) {
            ServerGlobalConfig cfg = copy(current);
            cfg.getScan().setIntervalSeconds(intervalSeconds);
            persistInternal(cfg);
            current = cfg;
        }
    }

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

    public void setLogoUrl(String logoUrl) {
        synchronized (writeLock) {
            ServerGlobalConfig cfg = copy(current);
            cfg.getBranding().setLogoUrl(logoUrl);
            persistInternal(cfg);
            current = cfg;
        }
    }

    public void markScanFinished(long epochSec) {
        synchronized (writeLock) {
            ServerGlobalConfig cfg = copy(current);
            cfg.getScan().setLastRunEpochSec(epochSec);
            persistInternal(cfg);
            current = cfg;
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
