package anton.davila.selfpotify.config;

import anton.davila.selfpotify.controllers.dto.RescanResultDTO;
import anton.davila.selfpotify.music.entity.Song;
import anton.davila.selfpotify.music.repository.SongRepository;
import anton.davila.selfpotify.music.service.SongService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.locks.ReentrantLock;

@Slf4j
@Service
public class ScanService {

    @Autowired
    private ConfigService configService;

    @Autowired
    private SongService songService;

    @Autowired
    private SongRepository songRepository;

    private final ReentrantLock scanLock = new ReentrantLock();

    /** Indica si hay un escaneo en curso en este momento. */
    public boolean isScanning() {
        return scanLock.isLocked();
    }

    /** Ejecuta un escaneo de todas las rutas configuradas. Devuelve false si ya había uno en curso. */
    public boolean runScan() {
        if (!scanLock.tryLock()) {
            log.info("ScanService: scan ya en curso, se omite el tick");
            return false;
        }
        try {
            log.info("ScanService: starting periodic scan");
            List<String> paths = configService.getConfig().getScan().getPaths();
            for (String p : paths) {
                Path folder = Paths.get(p);
                if (!Files.isDirectory(folder) || !Files.isReadable(folder)) {
                    log.warn("ScanService: ruta no accesible, se omite: {}", p);
                    continue;
                }
                try {
                    songService.loadFolder(folder.toAbsolutePath().normalize().toString());
                } catch (Exception e) {
                    log.error("ScanService: error escaneando {}", p, e);
                }
            }
            sweepAvailability();
            configService.markScanFinished(Instant.now().getEpochSecond());
            log.info("ScanService: scan completed");
            return true;
        } finally {
            scanLock.unlock();
        }
    }

    /** Lanza un escaneo inicial asíncrono sobre una única ruta recién añadida. */
    public CompletableFuture<Void> runScanForPath(String absolutePath) {
        return CompletableFuture.runAsync(() -> {
            if (!scanLock.tryLock()) {
                log.info("ScanService: scan en curso, scan inicial de {} esperará al próximo tick", absolutePath);
                return;
            }
            try {
                Path folder = Paths.get(absolutePath);
                if (!Files.isDirectory(folder)) {
                    log.warn("ScanService: ruta no accesible para scan inicial: {}", absolutePath);
                    return;
                }
                songService.loadFolder(folder.toAbsolutePath().normalize().toString());
            } catch (Exception e) {
                log.error("ScanService: error en scan inicial de {}", absolutePath, e);
            } finally {
                scanLock.unlock();
            }
        });
    }

    /**
     * Re-escaneo idempotente sobre todas las rutas configuradas: añade canciones
     * cuyo songPath aún no estaba en BBDD y recupera (available=true) las que
     * estaban marcadas como no disponibles. Devuelve null si ya hay un escaneo
     * en curso.
     */
    public RescanResultDTO rescan() {
        if (!scanLock.tryLock()) {
            log.info("ScanService: rescan ya en curso, se omite la petición");
            return null;
        }
        try {
            log.info("ScanService: starting rescan");
            SongService.RescanStats total = new SongService.RescanStats(0, 0, 0, 0);
            List<String> paths = configService.getConfig().getScan().getPaths();
            for (String p : paths) {
                Path folder = Paths.get(p);
                if (!Files.isDirectory(folder) || !Files.isReadable(folder)) {
                    log.warn("ScanService: ruta no accesible para rescan, se omite: {}", p);
                    continue;
                }
                try {
                    SongService.RescanStats stats = songService.rescanFolder(
                            folder.toAbsolutePath().normalize().toString());
                    total = total.plus(stats);
                } catch (Exception e) {
                    log.error("ScanService: error en rescan de {}", p, e);
                }
            }
            sweepAvailability();
            configService.markScanFinished(Instant.now().getEpochSecond());
            log.info("ScanService: rescan completed → {}", total);
            return new RescanResultDTO(total.added(), total.recovered(), total.skipped(), total.failed());
        } finally {
            scanLock.unlock();
        }
    }

    private void sweepAvailability() {
        for (Song song : songRepository.findAll()) {
            songService.isPathAvailable(song);
        }
    }
}
