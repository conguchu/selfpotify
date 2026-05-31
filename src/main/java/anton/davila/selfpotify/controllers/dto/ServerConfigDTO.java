package anton.davila.selfpotify.controllers.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ServerConfigDTO {
    private BrandingDTO branding;
    private boolean autoCompleteMetadata;
    private boolean autoCompleteCoverArt;
    private boolean setupComplete;
    private List<String> scanPaths;
    private long scanIntervalSeconds;
    private long lastScanEpochSec;
    // Contexto de ejecución para el panel: si corremos en Docker, los audios
    // subidos van siempre a la carpeta de datos; si no, el panel deja elegir una
    // de las scanPaths como destino. addedSongsDir es la carpeta selfpotify_added
    // por defecto (dentro de la carpeta de datos), mostrada en el panel.
    private boolean runningInDocker;
    private String addedSongsDir;
}
